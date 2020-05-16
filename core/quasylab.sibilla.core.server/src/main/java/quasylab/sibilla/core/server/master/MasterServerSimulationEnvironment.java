/*
 * Sibilla:  a Java framework designed to support analysis of Collective
 * Adaptive Systems.
 *
 * Copyright (C) 2020.
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package quasylab.sibilla.core.server.master;

import quasylab.sibilla.core.past.State;
import quasylab.sibilla.core.server.NetworkInfo;
import quasylab.sibilla.core.server.NetworkSimulationManager;
import quasylab.sibilla.core.server.SimulationDataSet;
import quasylab.sibilla.core.server.client.ClientCommand;
import quasylab.sibilla.core.server.network.TCPNetworkManager;
import quasylab.sibilla.core.server.network.TCPNetworkManagerType;
import quasylab.sibilla.core.server.network.UDPNetworkManager;
import quasylab.sibilla.core.server.network.UDPNetworkManagerType;
import quasylab.sibilla.core.server.serialization.CustomClassLoader;
import quasylab.sibilla.core.server.serialization.ObjectSerializer;
import quasylab.sibilla.core.server.util.NetworkUtils;
import quasylab.sibilla.core.simulator.SimulationEnvironment;
import quasylab.sibilla.core.simulator.sampling.SamplingFunction;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Manages connection with clients and slave servers to execute and manage the
 * simulations' tasks and their results over network connections.
 *
 * @author Stelluti Francesco Pio
 * @author Zamponi Marco
 */
public class MasterServerSimulationEnvironment implements PropertyChangeListener {

    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(MasterServerSimulationEnvironment.class.getName());

    /**
     * Discovery's network communication related infos.
     */
    private NetworkInfo LOCAL_DISCOVERY_INFO;

    /**
     * Simulation's network communication related infos.
     */
    private NetworkInfo LOCAL_SIMULATION_INFO;

    /**
     * Manages the network communication with the slave servers to be discovered.
     */
    private UDPNetworkManager discoveryNetworkManager;

    /**
     * Slave servers' discovery related thread executor.
     */
    private final ExecutorService slaveDiscoveryConnectionExecutor = Executors.newCachedThreadPool();

    /**
     * Used by the master server to manage the incoming clients' simulation requests.
     */
    private int localSimulationPort;

    /**
     * State of the master server.
     */
    private MasterState state;

    /**
     * Used by the slave servers to manage the incoming master server discovery message.
     */
    private int remoteDiscoveryPort;

    /**
     * Clients' communications related thread executor.
     */
    private final ExecutorService clientConnectionExecutor = Executors.newCachedThreadPool();

    /**
     * Milliseconds between two broadcast slave server discovery messages.
     */
    private final int discoveryTime = 5000;

    /**
     * Creates and starts up a master server with the given parameters.
     *
     * @param localDiscoveryPort       port used by the master server to manage the
     *                                 incoming slave servers' registration
     *                                 requests.
     * @param remoteDiscoveryPort      port used by the slave servers to manage the
     *                                 incoming master server discovery message.
     * @param discoveryNetworkManager  {@link quasylab.sibilla.core.server.network.UDPNetworkManagerType} of UDP network communication that will
     *                                 be used during the slave servers' discovery
     *                                 by the master.
     * @param localSimulationPort      port used by the master server to manage the
     *                                 incoming clients' simulation requests.
     * @param simulationNetworkManager {@link quasylab.sibilla.core.server.network.TCPNetworkManagerType} of TCP network communication that will
     *                                 be used between master server and clients.
     * @param listeners                {@link java.beans.PropertyChangeListener} instances that will be
     *                                 updated about the state of this master
     *                                 server.
     */

    public MasterServerSimulationEnvironment(int localDiscoveryPort, int remoteDiscoveryPort,
                                             UDPNetworkManagerType discoveryNetworkManager, int localSimulationPort,
                                             TCPNetworkManagerType simulationNetworkManager, PropertyChangeListener... listeners) {
        try {
            LOCAL_DISCOVERY_INFO = new NetworkInfo(NetworkUtils.getLocalAddress(), localDiscoveryPort, discoveryNetworkManager);
            LOCAL_SIMULATION_INFO = new NetworkInfo(NetworkUtils.getLocalAddress(), localSimulationPort,
                    simulationNetworkManager);
            this.remoteDiscoveryPort = remoteDiscoveryPort;
            this.state = new MasterState(LOCAL_SIMULATION_INFO);
            this.localSimulationPort = localSimulationPort;
            Arrays.stream(listeners).forEach(listener -> this.state.addPropertyChangeListener("Master Listener Update", listener));

            this.discoveryNetworkManager = UDPNetworkManager.createNetworkManager(LOCAL_DISCOVERY_INFO, true);

            LOGGER.info(String.format(
                    "Starting a new Master server"
                            + "\n- Local discovery port: [%d] - Discovery communication type: [%s - %s]"
                            + "\n- Local simulation handling port: [%d] - Simulation handling communication type[%s - %s]",
                    LOCAL_DISCOVERY_INFO.getPort(), LOCAL_DISCOVERY_INFO.getType().getClass(),
                    LOCAL_DISCOVERY_INFO.getType(), LOCAL_SIMULATION_INFO.getPort(),
                    LOCAL_SIMULATION_INFO.getType().getClass(), LOCAL_SIMULATION_INFO.getType()));

            ExecutorService activitiesExecutors = Executors.newCachedThreadPool();
            activitiesExecutors.execute(this::startDiscoveryServer);
            activitiesExecutors.execute(this::startSimulationServer);
            activitiesExecutors.execute(this::broadcastToInterfaces);
        } catch (SocketException e) {
            LOGGER.severe(String.format("[%s] Network interfaces exception", e.getMessage()));
        }

    }

    /**
     * Broadcasts the slave server discovery message through every master's network interface.
     */
    private void broadcastToInterfaces() {
        try {
            while (true) {
                state.resetKeepAlive();
                NetworkUtils.getBroadcastAddresses().stream().forEach(this::broadcastToSingleInterface);
                Thread.sleep(discoveryTime);
                state.cleanKeepAlive();
                LOGGER.info(String.format("Current set of servers: %s", state.slaveServersMap()));
            }
        } catch (InterruptedException e) {
            LOGGER.severe(String.format("[%s] Interrupted exception", e.getMessage()));
        } catch (SocketException e) {
            LOGGER.severe(String.format("[%s] Network interfaces exception", e.getMessage()));
        }
    }

    /**
     * Sends a broadcast message through a specified network interface.
     *
     * @param address of broadcast related to a specific interface.
     */
    private void broadcastToSingleInterface(InetAddress address) {
        try {
            discoveryNetworkManager.writeObject(ObjectSerializer.serializeObject(LOCAL_DISCOVERY_INFO), address,
                    remoteDiscoveryPort);
            LOGGER.info(String.format("Sent the discovery broadcast packet to the port: [%d]", remoteDiscoveryPort));
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the broadcast", e.getMessage()));
        }
    }

    /**
     * Starts the server that manages incoming discovery response messages from slave servers.
     */
    private void startDiscoveryServer() {
        try {
            while (true) {
                NetworkInfo slaveSimulationServer = (NetworkInfo) ObjectSerializer
                        .deserializeObject(discoveryNetworkManager.readObject());
                slaveDiscoveryConnectionExecutor.execute(() -> {
                    manageServers(slaveSimulationServer);
                });
            }
        } catch (ClassCastException e) {
            LOGGER.severe(String.format("[%s] Message cast failure during the discovery server startup", e.getMessage()));
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the discovery server startup", e.getMessage()));
        }
    }

    /**
     * Adds the network related informations received by a discovered slave server.
     *
     * @param info {@link quasylab.sibilla.core.server.NetworkInfo} received by a discovered slave server that will be used to submit it simulations.
     */
    private void manageServers(NetworkInfo info) {
        if (state.addSlaveServer(info)) {
            LOGGER.info(String.format("Added slave server - %s", info.toString()));
        }
    }

    /**
     * Starts the server that listens for clients that want to submit simulations.
     */
    private void startSimulationServer() {
        try {
            ServerSocket serverSocket = TCPNetworkManager
                    .createServerSocket((TCPNetworkManagerType) LOCAL_SIMULATION_INFO.getType(), localSimulationPort);
            LOGGER.info(String.format("The server is now listening for clients on port: [%d]",
                    LOCAL_SIMULATION_INFO.getPort()));
            while (true) {
                Socket socket = serverSocket.accept();
                clientConnectionExecutor.execute(() -> {
                    manageClientMessage(socket);
                });
            }
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the server socket startup", e.getMessage()));
        }
    }

    /**
     * Handles a message received by a client.
     *
     * @param socket {@link java.net.Socket} through which the message arrived.
     */
    private void manageClientMessage(Socket socket) {
        try {
            TCPNetworkManager simulationNetworkManager = TCPNetworkManager
                    .createNetworkManager((TCPNetworkManagerType) LOCAL_SIMULATION_INFO.getType(), socket);
            SimulationState simulationState = new SimulationState(this.state, LOCAL_SIMULATION_INFO,
                    simulationNetworkManager.getServerInfo(), this.state.getSlaveServersNetworkInfos(), this);

            AtomicBoolean clientIsActive = new AtomicBoolean(true);

            Map<ClientCommand, Runnable> map = Map.of(ClientCommand.PING,
                    () -> this.respondPingRequest(simulationNetworkManager), ClientCommand.INIT,
                    () -> this.loadModelClass(simulationNetworkManager, simulationState), ClientCommand.DATA,
                    () -> this.handleSimulationDataSet(simulationNetworkManager, simulationState),
                    ClientCommand.CLOSE_CONNECTION,
                    () -> this.closeConnectionWithClient(simulationNetworkManager, clientIsActive, simulationState));
            while (clientIsActive.get()) {
                ClientCommand command = (ClientCommand) ObjectSerializer
                        .deserializeObject(simulationNetworkManager.readObject());
                LOGGER.info(String.format("[%s] command received by client - %s", command,
                        simulationNetworkManager.getServerInfo().toString()));
                map.getOrDefault(command, () -> {
                    throw new ClassCastException("Command received from client wasn't expected.");
                }).run();
            }
        } catch (ClassCastException e) {
            LOGGER.severe(String.format("[%s] Message cast failure during client communication", e.getMessage()));
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during client communication", e.getMessage()));
        }
    }

    /**
     * Closes a client related network communication, removes the received simulation model from the master memory and signals that the client is no longer communicating with the master server.
     *
     * @param client          client related {@link quasylab.sibilla.core.server.network.TCPNetworkManager} which connection has to be closed.
     * @param clientActive    whether the client is active or not.
     * @param simulationState the state of the simulation related to the client's connection that needs to be closed.
     */
    private void closeConnectionWithClient(TCPNetworkManager client, AtomicBoolean clientActive,
                                           SimulationState simulationState) {
        try {
            String modelName = (String) ObjectSerializer.deserializeObject(client.readObject());
            LOGGER.info(String.format("[%s] Model name to be deleted read by client: %s", modelName,
                    client.getServerInfo().toString()));
            clientActive.set(false);
            CustomClassLoader.classes.remove(modelName);
            LOGGER.info(String.format("[%s] Model deleted off the class loader", modelName));
            client.writeObject(ObjectSerializer.serializeObject(MasterCommand.CLOSE_CONNECTION));
            LOGGER.info(String.format("[%s] command sent to the client: %s", MasterCommand.CLOSE_CONNECTION,
                    client.getServerInfo().toString()));

            client.closeConnection();
            LOGGER.info(String.format("Master closed the connection with client: %s", client.getServerInfo().toString()));
        } catch (ClassCastException e) {
            LOGGER.severe(String.format("[%s] Message cast failure during connection closure - Client: %s", e.getMessage(), client.getServerInfo().toString()));
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the connection closure - Client: %s", e.getMessage(), client.getServerInfo().toString()));
        }
    }

    /**
     * Handles the simulation datas' from the client and submits the slave servers a new set of simulations.
     *
     * @param client          client related {@link quasylab.sibilla.core.server.network.TCPNetworkManager}.
     * @param simulationState the state of the simulation related to the datas that need to be managed.
     */
    private void handleSimulationDataSet(TCPNetworkManager client, SimulationState simulationState) {
        try {
            SimulationDataSet<State> dataSet = (SimulationDataSet<State>) ObjectSerializer
                    .deserializeObject(client.readObject());
            simulationState.setSimulationDataSet(dataSet);
            simulationState.setClientConnection(client);
            LOGGER.info(
                    String.format("Simulation datas received by the client: %s", client.getServerInfo().toString()));
            client.writeObject(ObjectSerializer.serializeObject(MasterCommand.DATA_RESPONSE));
            LOGGER.info(String.format("[%s] command sent to the client: %s", MasterCommand.DATA_RESPONSE,
                    client.getServerInfo().toString()));
            this.submitSimulations(client, dataSet, simulationState);
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the simulation dataset reception - Client: %s", e.getMessage(), client.getServerInfo().toString()));
        }
    }

    /**
     * Submits the slave servers a new set of simulations.
     *
     * @param dataSet containing all the simulation oriented datas.
     */
    private SamplingFunction submitSimulations(TCPNetworkManager client, SimulationDataSet dataSet, SimulationState simulationState) {
        try {
            SimulationEnvironment sim = new SimulationEnvironment(
                    NetworkSimulationManager.getNetworkSimulationManagerFactory(simulationState));
            sim.simulate(dataSet.getRandomGenerator(), dataSet.getModel(), dataSet.getModelInitialState(),
                    dataSet.getModelSamplingFunction(), dataSet.getReplica(), dataSet.getDeadline());
            this.state.increaseExecutedSimulations();
            return dataSet.getModelSamplingFunction();
        } catch (InterruptedException e) {
            LOGGER.severe(String.format("[%s] Simulation has been interrupted before its completion - Client: %s", e.getMessage(), client.getServerInfo().toString()));
        }
        return null;
    }

    /**
     * Manages the reception of the simulation model from the client.
     *
     * @param client          client related {@link quasylab.sibilla.core.server.network.TCPNetworkManager}.
     * @param simulationState the state of the simulation related to the simulation model that need to be managed.
     */
    private void loadModelClass(TCPNetworkManager client, SimulationState simulationState) {
        try {
            String modelName = (String) ObjectSerializer.deserializeObject(client.readObject());
            LOGGER.info(String.format("[%s] Model name read by client: %s", modelName,
                    client.getServerInfo().toString()));
            byte[] modelBytes = client.readObject();
            CustomClassLoader.defClass(modelName, modelBytes);
            String classLoadedName = Class.forName(modelName).getName();
            simulationState.setSimulationModelName(classLoadedName);
            LOGGER.info(String.format("[%s] Class loaded with success", classLoadedName));
            client.writeObject(ObjectSerializer.serializeObject(MasterCommand.INIT_RESPONSE));
            LOGGER.info(String.format("[%s] command sent to the client: %s", MasterCommand.INIT_RESPONSE,
                    client.getServerInfo().toString()));
        } catch (ClassCastException e) {
            LOGGER.severe(String.format("[%s] Message cast failure during the simulation model loading - Client: %s", e.getMessage(), client.getServerInfo().toString()));

        } catch (ClassNotFoundException e) {
            LOGGER.severe(String.format("[%s] The simulation model was not loaded with success - Client: %s", e.getMessage(), client.getServerInfo().toString()));

        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the simulation model loading - Client: %s", e.getMessage(), client.getServerInfo().toString()));

        }
    }

    /**
     * Manages a ping request from the client.
     *
     * @param client client related {@link quasylab.sibilla.core.server.network.TCPNetworkManager}.
     */
    private void respondPingRequest(TCPNetworkManager client) {
        try {
            client.writeObject(ObjectSerializer.serializeObject(MasterCommand.PONG));
            LOGGER.info(String.format("[%s] command sent to the client: %s", MasterCommand.PONG,
                    client.getServerInfo().toString()));
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the ping response - %s", e.getMessage(), client.getServerInfo().toString()));

        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof SimulationState) {
            SimulationState state = (SimulationState) evt.getNewValue();
            if (state.isConcluded()) {
                try {
                    state.clientConnection().writeObject(ObjectSerializer.serializeObject(MasterCommand.RESULTS));
                    LOGGER.info(String.format("[%s] command sent to the client: %s", MasterCommand.RESULTS,
                            state.clientConnection().getServerInfo().toString()));
                    state.clientConnection().writeObject(ObjectSerializer.serializeObject(state.simulationDataSet().getModelSamplingFunction()));
                    LOGGER.info(String.format("Results have been sent to the client: %s",
                            state.clientConnection().getServerInfo().toString()));
                } catch (IOException e) {
                    LOGGER.severe(String.format("[%s] Network communication failure during the results submit - Client: %s", e.getMessage(), state.clientConnection().getServerInfo().toString()));
                }
            }
        }
    }
}
