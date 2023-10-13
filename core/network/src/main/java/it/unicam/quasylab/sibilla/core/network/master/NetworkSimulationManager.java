/*
 * Sibilla:  a Java framework designed to support analysis of Collective
 * Adaptive Systems.
 *
 *             Copyright (C) 2020.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package it.unicam.quasylab.sibilla.core.network.master;

import it.unicam.quasylab.sibilla.core.models.Model;
import it.unicam.quasylab.sibilla.core.models.ModelDefinition;
import it.unicam.quasylab.sibilla.core.models.State;
import it.unicam.quasylab.sibilla.core.network.ComputationResult;
import it.unicam.quasylab.sibilla.core.network.HostLoggerSupplier;
import it.unicam.quasylab.sibilla.core.network.NetworkInfo;
import it.unicam.quasylab.sibilla.core.network.NetworkTask;
import it.unicam.quasylab.sibilla.core.network.benchmark.BenchmarkUnit;
import it.unicam.quasylab.sibilla.core.network.communication.TCPNetworkManager;
import it.unicam.quasylab.sibilla.core.network.compression.Compressor;
import it.unicam.quasylab.sibilla.core.network.loaders.ClassBytesLoader;
import it.unicam.quasylab.sibilla.core.network.serialization.ComputationResultSerializer;
import it.unicam.quasylab.sibilla.core.network.serialization.ComputationResultSerializerType;
import it.unicam.quasylab.sibilla.core.network.serialization.Serializer;
import it.unicam.quasylab.sibilla.core.network.serialization.SerializerType;
import it.unicam.quasylab.sibilla.core.network.slave.SlaveCommand;
import it.unicam.quasylab.sibilla.core.network.slave.SlaveState;
import it.unicam.quasylab.sibilla.core.simulator.*;
import it.unicam.quasylab.sibilla.core.simulator.sampling.SamplingHandler;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Handles and coordinates a simulation between the slave servers
 *
 * @param <S> The {@link State} of the simulation model.
 * @author Belenchia Matteo
 * @author Stelluti Francesco Pio
 * @author Zamponi Marco
 */
public class NetworkSimulationManager<S extends State> extends QueuedSimulationManager<S> {

    /**
     * Class logger.
     */
    private final Logger LOGGER;

    /**
     * {@link ModelDefinition} that represent the Model
     * used in the simulation.
     */
    private final String modelDefinitionClassName;

    /**
     * Queue of servers used to fetch the slave servers the tasks are sent to.
     */
    private final BlockingQueue<TCPNetworkManager> serverQueue;

    /**
     * Tasks handling related thread executor.
     */
    private final ExecutorService executor;

    /**
     * State of the simulation that is being executed
     */
    private final SimulationState simulationState;

    /**
     * Set of network managers associated to the connected slave servers
     */
    private final Set<TCPNetworkManager> networkManagers;

    private final Serializer serializer;

    private final BenchmarkUnit decDesBenchmark;
    private final ComputationResultSerializerType crSerializerType;
    private final Map<NetworkInfo, BenchmarkUnit> slaveBenchmarks;
    private final NetworkInfo clientInfo;

    /**
     * Creates a NetworkSimulationManager with the parameters given in input
     *
     * @param random          RandomGenerator used in the simulation
     * @param monitor         TODO
     * @param simulationState state of the simulation that is being executed
     */
    public NetworkSimulationManager(RandomGenerator random, SimulationMonitor monitor,
                                    SimulationState simulationState, SerializerType serializerType,
                                    ComputationResultSerializerType crSerializerType, NetworkInfo clientInfo) {
        super(random, monitor);// TODO: Gestire parametro Monitor
        this.clientInfo = clientInfo;
        this.LOGGER = HostLoggerSupplier.getInstance().getLogger();
        this.serializer = Serializer.getSerializer(serializerType);
        this.slaveBenchmarks = new ConcurrentHashMap<NetworkInfo, BenchmarkUnit>();
        this.crSerializerType = crSerializerType;
        this.decDesBenchmark = new BenchmarkUnit(
                String.format("sibillaBenchmarks/masterBenchmarking/ComputationResultSerializer_%s/", this.crSerializerType.getFullName()),
                String.format("%s_resultsDecompressAndDeserialize", this.crSerializerType), "csv",
                this.crSerializerType.getLabel(), List.of("decomprtime", "desertime", "trajectories"));

        List<NetworkInfo> slaveNetworkInfos = simulationState.getSlaveServersStates().stream()
                .map(SlaveState::getSlaveInfo).collect(Collectors.toList());
        LOGGER.info(String.format("Creating a new NetworkSimulationManager to contact the slaves: [%s]",
                slaveNetworkInfos));
        this.simulationState = simulationState;
        this.modelDefinitionClassName = simulationState.getSimulationModelName();
        executor = Executors.newCachedThreadPool();
        networkManagers = slaveNetworkInfos.stream().map(serverInfo -> {
            try {
                TCPNetworkManager server = TCPNetworkManager.createNetworkManager(serverInfo);
                LOGGER.info(String.format("Created a NetworkManager to contact the slave: %s",
                        server.getNetworkInfo().toString()));
                initConnection(server);
                LOGGER.info(String.format("All the model informations have been sent to the slave: %s",
                        server.getNetworkInfo().toString()));
                BenchmarkUnit newSlaveBenchmark = new BenchmarkUnit(
                        String.format("sibillaBenchmarks/masterBenchmarking/Client_%s/Slave_%s/ComputationResultSerializer_%s/",
                                this.clientInfo.getAddress().toString().split("/")[0], serverInfo.getAddress().toString().split("/")[0], this.crSerializerType.getFullName()),
                        String.format("%s_sendAndReceive", this.crSerializerType), "csv",
                        this.crSerializerType.getLabel(), List.of("sendandreceivetime", "tasks", "results"));
                this.slaveBenchmarks.put(serverInfo, newSlaveBenchmark);
                return server;
            } catch (IOException e) {
                LOGGER.severe(String.format("[%s] Error during server initialization, removing slave", e.getMessage()));
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
        serverQueue = new LinkedBlockingQueue<>(networkManagers);
        this.startTasksHandling();
    }

    public static SimulationManagerFactory getNetworkSimulationManagerFactory(SimulationState simulationState,
                                                                              SerializerType serializerType, ComputationResultSerializerType crSerializerType, NetworkInfo clientInfo) {
        return new SimulationManagerFactory() {
            @Override
            public <S extends State> SimulationManager<S> getSimulationManager(RandomGenerator random,
                                                                               SimulationMonitor monitor) {
                return new NetworkSimulationManager<>(random, monitor, simulationState,
                        serializerType, crSerializerType, clientInfo);
            }
        };

    }

    /**
     * Initializes a connection to the target server sending the model class
     *
     * @param slave NetworkManager through the model is passed
     */
    private void initConnection(TCPNetworkManager slave) throws IOException {
        try {
            slave.writeObject(serializer.serialize(MasterCommand.INIT));
            LOGGER.info(String.format("[%s] command sent to the slave: %s", MasterCommand.INIT,
                    slave.getNetworkInfo().toString()));
            slave.writeObject(serializer.serialize(modelDefinitionClassName));
            LOGGER.info(String.format("[%s] Model name has been sent to the slave: %s",
                    modelDefinitionClassName, slave.getNetworkInfo().toString()));
            slave.writeObject(ClassBytesLoader.loadClassBytes(modelDefinitionClassName));
            LOGGER.info(
                    String.format("Class bytes have been sent to the slave: %s", slave.getNetworkInfo().toString()));

            SlaveCommand answer = (SlaveCommand) serializer.deserialize(slave.readObject());
            if (answer.equals(SlaveCommand.INIT_RESPONSE)) {
                LOGGER.info(
                        String.format("Answer received: [%s] - Slave: %s", answer, slave.getNetworkInfo().toString()));
            } else {
                throw new ClassCastException("Wrong answer after INIT command. Expected INIT_RESPONSE");
            }

        } catch (ClassCastException e) {
            LOGGER.severe(String.format("[%s] Message cast failure during the connection initialization - Slave: %s",
                    e.getMessage(), slave.getNetworkInfo().toString()));
            throw new IOException();
        } catch (IOException e) {
            LOGGER.severe(String.format(
                    "[%s] Network communication failure during the connection initialization  - Slave: %s",
                    e.getMessage(), slave.getNetworkInfo().toString()));
            throw new IOException();
        }
    }

    @Override
    protected void startTasksHandling() {
        new Thread(this::handleTasks).start();
    }

    private void handleTasks() {
        while ((isRunning() || hasTasks() || this.simulationState.getRunningSlaveServers() > 0)
                && !this.simulationState.getSlaveServersStates().isEmpty()) {
            singleTaskExecution();
        }
        this.simulationState.setConcluded();
        this.closeStreams();
    }

    /**
     * Sends to the next server in the queue the task to execute and waits for the
     * results
     */
    private void singleTaskExecution() {
        try {
            TCPNetworkManager server = findServer();
            LOGGER.info(String.format("Slave currently connected to: %s", server.getNetworkInfo().toString()));
            SlaveState serverState = this.simulationState.getSlaveStateByServerInfo(server.getNetworkInfo());
            LOGGER.info(String.format("State of the slave: [%s]", serverState.toString()));
            int acceptableTasks = serverState.getExpectedTasks();
            if (!serverState.canCompleteTask(acceptableTasks)) {
                acceptableTasks = acceptableTasks == 1 ? 1 : acceptableTasks / 2;
                LOGGER.severe(String.format("Server's tasks window has been reduced in half - %s",
                        server.getNetworkInfo().toString()));
            }
            LOGGER.info(String.format("Has tasks: %s", hasTasks()));
            LOGGER.info(String.format("Is running: %s", isRunning()));
            List<SimulationTask<S>> toRun = getTask(acceptableTasks, true);
            LOGGER.info(String.format("Tasks to run: %d", toRun.size()));
            this.simulationState.setPendingTasks(this.pendingTasks());
            if (toRun.size() > 0) {
                simulationState.increaseRunningServers();
                NetworkTask<S> networkTask = new NetworkTask<>(toRun);
                CompletableFuture.supplyAsync(() -> send(networkTask, server), executor)
                        .whenComplete((value, error) -> manageResult(value, error, toRun, server));
            }
        } catch (InterruptedException e) {
            LOGGER.severe(String.format("[%s] Interrupted exception", e.getMessage()));
        }
    }

    /**
     * Takes the first server from the queue
     *
     * @return First server available in the queue
     */
    private TCPNetworkManager findServer() throws InterruptedException {
        return serverQueue.take();
    }

    /**
     * Adds a server to the queue
     *
     * @param server Server to be added to the queue
     */
    private void enqueueServer(TCPNetworkManager server) {
        serverQueue.add(server);
    }

    /**
     * Manages the results of a NetworkTask sent by a simulation server
     *
     * @param value  results of the computation
     * @param error  eventually thrown error
     * @param tasks  list of tasks executed
     * @param server server which has been used for the simulation
     */
    private void manageResult(ComputationResult<S> value, Throwable error, List<SimulationTask<S>> tasks,
                              TCPNetworkManager server) {
        LOGGER.info(String.format("Managing results by the slave: %s", server.getNetworkInfo().toString()));
        if (error != null) {
            error.printStackTrace();
            LOGGER.severe(String.format("Timeout occurred for slave: %s", server.getNetworkInfo().toString()));
            TCPNetworkManager newServer;
            if ((newServer = manageTimeout(server)) != null) {
                LOGGER.info(String.format("The slave has responded. New server: %s",
                        newServer.getNetworkInfo().toString()));
                enqueueServer(newServer);// add new server to queue, old server won't return
            } else if (this.simulationState.getSlaveServersStates().isEmpty()) {
                synchronized (this) {
                    notifyAll();
                }
            }
            rescheduleAll(tasks);
            simulationState.decreaseRunningServers();
        } else {
            LOGGER.info(String.format("Timeout did not occurred for slave: %s", server.getNetworkInfo().toString()));
            enqueueServer(server);
            simulationState.decreaseRunningServers();
            //FIXME!
            //value.getResults().forEach(this::handleTrajectory);
        }
    }

    /**
     * Manages a timeout
     *
     * @param server server that was in timeout
     * @return new server to use to execute tasks
     */
    private TCPNetworkManager manageTimeout(TCPNetworkManager server) {
        SlaveState oldState = this.simulationState.getSlaveStateByServerInfo(server.getNetworkInfo());
        TCPNetworkManager pingServer = null;
        NetworkInfo pingNetworkInfo;
        try {
            LOGGER.warning(String.format("Managing timeout of slave: %s", server.getNetworkInfo().toString()));
            pingNetworkInfo = new NetworkInfo(server.getSocket().getInetAddress(), server.getSocket().getPort(),
                    server.getType());
            pingServer = TCPNetworkManager.createNetworkManager(pingNetworkInfo);
            pingServer.getSocket().setSoTimeout(5000); // set 5 seconds timeout on read operations
            LOGGER.info(String.format("Creating a new NetworkManager to ping slave: %s",
                    pingServer.getNetworkInfo().toString()));
            oldState.timedOut(); // mark server as timed out

            initConnection(pingServer); // initialize connection sending model data
            pingServer.writeObject(serializer.serialize(MasterCommand.PING));
            LOGGER.info(String.format("Ping request sent to slave: %s", pingServer.getNetworkInfo().toString())); // send
            // ping
            // request
            SlaveCommand response = (SlaveCommand) serializer.deserialize(pingServer.readObject()); // wait
            // for
            // response
            if (!response.equals(SlaveCommand.PONG)) {
                LOGGER.severe(String.format("The response received wasn't the one expected by the slave: %s",
                        pingServer.getNetworkInfo().toString()));
                throw new IllegalStateException("Expected a different reply!");
            }
            LOGGER.info(String.format(
                    "The response has been received within the time limit. The task window will be reduced by half for the slave: %s",
                    pingServer.getNetworkInfo().toString()));
            oldState.forceExpiredTimeLimit(); // halve the task window
            oldState.migrate(pingNetworkInfo);
            this.networkManagers.add(pingServer);

            server.getSocket().close();
            this.networkManagers.remove(server);
        } catch (Exception e) {
            assert pingServer != null;
            LOGGER.severe(
                    String.format("The response has been received after the time limit. The slave will be removed: %s",
                            pingServer.getNetworkInfo().toString()));
            oldState.setRemoved(); // mark server as removed
            this.networkManagers.remove(server);
            return null;
        }
        return pingServer;
    }

    @Override
    public synchronized void join() throws InterruptedException {
        while ((getRunningTasks() > 0 || hasTasks() || this.simulationState.getRunningSlaveServers() > 0)
                && !this.simulationState.getSlaveServersStates().isEmpty()) {
            wait();
        }
        closeStreams();

    }

    /**
     * Closes all the connection streams
     */
    private void closeStreams() {
        try {
            for (TCPNetworkManager server : this.networkManagers) {
                server.writeObject(serializer.serialize(MasterCommand.CLOSE_CONNECTION));
                LOGGER.info(String.format("[%s] command sent to the slave: %s", MasterCommand.CLOSE_CONNECTION,
                        server.getNetworkInfo().toString()));
                server.writeObject(serializer.serialize(modelDefinitionClassName));

                SlaveCommand answer = (SlaveCommand) serializer.deserialize(server.readObject());
                if (answer.equals(SlaveCommand.CLOSE_CONNECTION)) {
                    LOGGER.info(String.format("Answer received: [%s] - Slave: %s", answer,
                            server.getNetworkInfo().toString()));
                } else {
                    throw new ClassCastException(String.format(
                            "Wrong answer after CLOSE_CONNECTION command. Expected CLOSE_CONNECTION from slave: %s ",
                            server.getNetworkInfo().toString()));
                }

                server.closeConnection();
                LOGGER.info(
                        String.format("Closed the connection with the slave: %s", server.getNetworkInfo().toString()));
            }
        } catch (IOException e) {
            LOGGER.severe(
                    String.format("[%s] Network communication failure during the connection closure", e.getMessage()));
        }
    }

    /**
     * Sends tasks to execute to a server
     *
     * @param networkTask tasks to execute
     * @param server      server to send the tasks to
     * @return result of the computation
     */
    private ComputationResult<S> send(NetworkTask<S> networkTask, TCPNetworkManager server) {

        SlaveState state = this.simulationState.getSlaveStateByServerInfo(server.getNetworkInfo());
        BenchmarkUnit sendRecBenchmark = this.slaveBenchmarks.get(server.getNetworkInfo());

        final var wrapper = new Object() {
            private ComputationResult<S> result;
        };
        try {
            sendRecBenchmark.run(() -> {
                server.writeObject(serializer.serialize(MasterCommand.TASK));
                server.writeObject(Compressor.compress(serializer.serialize(networkTask)));
                wrapper.result = awaitingResults(server, state, networkTask);

                return List.of((double) networkTask.getTasks().size(), (double) wrapper.result.getResults().size());
            });

        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
        return wrapper.result;
    }

    /**
     * Puts the master server in a state where he listens for results until all the
     * tasks sent to the slave server have been executed and their results are sent
     * to the server.
     *
     * @param server the NetworkManager of the slave server that the master listens
     *               to for results
     * @param state  the SlaveState associated to the slave server
     * @param tasks  the NetworkTask that contains the simulations to execute
     * @return the ComputationResult that contains all the result for the given
     * NetworkTask
     * @throws IOException if communication error between servers occur
     */
    private ComputationResult<S> awaitingResults(TCPNetworkManager server, SlaveState state, NetworkTask<?> tasks)
            throws IOException {
        ComputationResult<S> results = new ComputationResult<>(new LinkedList<>());
        //FIXME!
//        state.setSentTasks(tasks.getTasks().size());
//        state.setReceivedTasks(0);
//        long elapsedTime = System.nanoTime();
//
//        server.getSocket().setSoTimeout((int) (state.getTimeout() / 1000000));
//        LOGGER.info(
//                String.format("A group of tasks has been sent to the server - %s", server.getNetworkInfo().toString()));
//        Model model = tasks.getTasks().get(0).getUnit().getModel();
//        while (state.getReceivedTasks() < state.getSentTasks()) {
//            final var wrapper = new Object() {
//                private byte[] received;
//                private ComputationResult<S> results;
//            };
//
//            wrapper.received = server.readObject();
//            this.decDesBenchmark.run(() -> {
//                wrapper.received = Compressor.decompress(wrapper.received);
//                return List.of();
//            }, () -> {
//                wrapper.results = this.deserializeComputationResult(wrapper.received, model);
//                return List.of((double) wrapper.results.getResults().size());
//            });
//            results.add(wrapper.results);
//            state.setReceivedTasks(state.getReceivedTasks() + wrapper.results.getResults().size());
//        }
//        elapsedTime = System.nanoTime() - elapsedTime;
//        LOGGER.info(String.format("\nSent tasks size: %d\nReceived tasks size: %d", tasks.getTasks().size(),
//                results.getResults().size()));
//        state.update(elapsedTime);
//        LOGGER.info(String.format("The results from the computation have been received from the server - %s",
//                server.getNetworkInfo().toString()));
        return results;
    }

    private ComputationResult deserializeComputationResult(byte[] toDeserialize, Model model) throws IOException {
        switch (this.crSerializerType) {
            case FST:
                return (ComputationResult) Serializer.getSerializer(SerializerType.FST).deserialize(toDeserialize);
            case APACHE:
                return (ComputationResult) Serializer.getSerializer(SerializerType.APACHE).deserialize(toDeserialize);
            default:
            case CUSTOM:
                return ComputationResultSerializer.deserialize(toDeserialize, model);
        }
    }
}