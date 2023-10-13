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

package it.unicam.quasylab.sibilla.core.network.client;

import it.unicam.quasylab.sibilla.core.models.Model;
import it.unicam.quasylab.sibilla.core.models.ModelDefinition;
import it.unicam.quasylab.sibilla.core.models.State;
import it.unicam.quasylab.sibilla.core.network.HostLoggerSupplier;
import it.unicam.quasylab.sibilla.core.network.NetworkInfo;
import it.unicam.quasylab.sibilla.core.network.SimulationDataSet;
import it.unicam.quasylab.sibilla.core.network.benchmark.BenchmarkUnit;
import it.unicam.quasylab.sibilla.core.network.communication.TCPNetworkManager;
import it.unicam.quasylab.sibilla.core.network.loaders.ClassBytesLoader;
import it.unicam.quasylab.sibilla.core.network.master.MasterCommand;
import it.unicam.quasylab.sibilla.core.network.serialization.Serializer;
import it.unicam.quasylab.sibilla.core.network.serialization.SerializerType;
import it.unicam.quasylab.sibilla.core.simulator.sampling.SamplingFunction;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Manages the connection with a master server to submit simulations and retrieve related results.
 *
 * @param <S> The {@link State} of the simulation model.
 * @author Stelluti Francesco Pio
 * @author Zamponi Marco
 */
public class ClientSimulationEnvironment<S extends State> {

    /**
     * Class logger.
     */
    private final Logger LOGGER;

    /**
     * The {@link SimulationDataSet} object to be sent to the master server.
     */
    private final SimulationDataSet<S> data;

    /**
     * Manages the network communication with the master server.
     */
    private TCPNetworkManager masterServerNetworkManager;


    private final Serializer serializer;

    private final int submitRepetitions;
    private final BenchmarkUnit clientBenchmark;

    /**
     * Initiates a new client that submits simulations using the parameters of
     * the simulation to execute and the network related data of the
     * master server that will manage such simulation.
     *
     * @param random            {@link org.apache.commons.math3.random.RandomGenerator} of the simulation.
     * @param modelDefinition   {@link ModelDefinition} that defines the simulation model to be sent.
     * @param model             The {@link Model} of the simulation.
     * @param initialState      The initial {@link State} of the model.
     * @param samplingFunction The {@link SamplingFunction} that will be used to collect
     *                          data.
     * @param replica           Repetitions of the simulation.
     * @param deadline          Time interval between two samplings.
     * @param masterNetworkInfo {@link NetworkInfo} of the master to be reached.
     */
    public ClientSimulationEnvironment(RandomGenerator random, ModelDefinition<S> modelDefinition, Model<S> model,
                                       S initialState, SamplingFunction<S> samplingFunction, int replica, double deadline,
                                       NetworkInfo masterNetworkInfo, SerializerType serializerType, int submitRepetitions) {

        LOGGER = HostLoggerSupplier.getInstance().getLogger();

        serializer = Serializer.getSerializer(serializerType);
        this.submitRepetitions = submitRepetitions;
        this.clientBenchmark = new BenchmarkUnit(
                "sibillaBenchmarks/clientBenchmarking/",
                "submitReceiveAndClose",
                "csv",
                "client",
                List.of("submitreceiveandclosetime"));

        this.data = new SimulationDataSet<>(random, modelDefinition.getClass().getName(), model, initialState, samplingFunction, replica,
                deadline);

        for (int i = 1; i <= this.submitRepetitions; i++) {
            this.clientBenchmark.run(() -> {

                this.masterServerNetworkManager = TCPNetworkManager.createNetworkManager(masterNetworkInfo);
                LOGGER.info(String.format("Starting a new client that will submit the simulation to the master: %s",
                        masterNetworkInfo));

                this.initConnection(masterServerNetworkManager);
                this.sendSimulationInfo(masterServerNetworkManager);
                this.closeConnection(masterServerNetworkManager);

                return List.of();
            });
        }


    }

    /**
     * Closes the connection with the given master server
     *
     * @param targetMaster NetworkManager to the master server
     */
    private void closeConnection(TCPNetworkManager targetMaster) throws IOException {
        try {
            targetMaster.writeObject(serializer.serialize(ClientCommand.CLOSE_CONNECTION));
            LOGGER.info(String.format("[%s] command sent to the master: %s", ClientCommand.CLOSE_CONNECTION,
                    targetMaster.getNetworkInfo().toString()));
            targetMaster.writeObject(serializer.serialize(this.data.getModelDefinitionClassName()));

            MasterCommand answer = (MasterCommand) serializer.deserialize(targetMaster.readObject());
            if (answer.equals(MasterCommand.CLOSE_CONNECTION)) {
                LOGGER.info(String.format("Answer received: [%s] - Master: %s", answer, targetMaster.getNetworkInfo().toString()));
            } else {
                throw new ClassCastException("Wrong answer after CLOSE_CONNECTION command. Expected CLOSE_CONNECTION from master");
            }

            this.masterServerNetworkManager.closeConnection();
            LOGGER.info(String.format("Closed the connection with the master: %s", targetMaster.getNetworkInfo()));

        } catch (ClassCastException e) {
            LOGGER.severe(String.format("[%s] Message cast failure during the connection closure - Master: %s", e.getMessage(), targetMaster.getNetworkInfo().toString()));
            throw new IOException();
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the connection closure - Master: %s", e.getMessage(), targetMaster.getNetworkInfo().toString()));
            throw new IOException();
        }
    }

    /**
     * Sends the original class to the quasylab.sibilla.core.server.master server
     *
     * @param targetMaster NetworkManager to the master server
     */
    private void initConnection(TCPNetworkManager targetMaster) throws IOException {
        try {
            LOGGER.info(String.format("Loading [%s] class bytes to be transmitted over network", data.getModelDefinitionClassName()));
            byte[] classBytes = ClassBytesLoader.loadClassBytes(data.getModelDefinitionClassName());

            targetMaster.writeObject(serializer.serialize(ClientCommand.INIT));
            LOGGER.info(String.format("[%s] command sent to the master: %s", ClientCommand.INIT,
                    targetMaster.getNetworkInfo().toString()));
            targetMaster.writeObject(serializer.serialize(data.getModelDefinitionClassName()));
            LOGGER.info(String.format("[%s] Model name has been sent to the master: %s", this.data.getModelDefinitionClassName(),
                    targetMaster.getNetworkInfo().toString()));
            targetMaster.writeObject(classBytes);
            LOGGER.info(String.format("Class bytes have been sent to the master: %s", targetMaster.getNetworkInfo().toString()));

            MasterCommand answer = (MasterCommand) serializer.deserialize(targetMaster.readObject());
            if (answer.equals(MasterCommand.INIT_RESPONSE)) {
                LOGGER.info(String.format("Answer received: [%s] - Master: %s", answer, targetMaster.getNetworkInfo().toString()));
            } else {
                throw new ClassCastException("Wrong answer after INIT command. Expected INIT_RESPONSE by master");
            }

        } catch (ClassCastException e) {
            LOGGER.severe(String.format("[%s] Message cast failure during the connection initialization - Master: %s", e.getMessage(), targetMaster.getNetworkInfo().toString()));
            throw new IOException();
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the connection initialization  - Master: %s", e.getMessage(), targetMaster.getNetworkInfo().toString()));
            throw new IOException();
        }
    }

    /**
     * Sends the info of the simulation to execute to the master server
     *
     * @param targetMaster NetworkManager to the master server
     */
    private void sendSimulationInfo(TCPNetworkManager targetMaster) throws IOException {
        try {
            targetMaster.writeObject(serializer.serialize(ClientCommand.DATA));
            LOGGER.info(String.format("[%s] command sent to the master: %s", ClientCommand.DATA,
                    targetMaster.getNetworkInfo().toString()));
            targetMaster.writeObject(serializer.serialize(data));
            LOGGER.info(String.format("Simulation datas have been sent to the master: %s",
                    targetMaster.getNetworkInfo().toString()));

            MasterCommand answer = (MasterCommand) serializer.deserialize(targetMaster.readObject());
            if (answer.equals(MasterCommand.DATA_RESPONSE)) {
                LOGGER.info(String.format("Answer received: [%s] - Master: %s", answer, targetMaster.getNetworkInfo().toString()));
            } else {
                throw new ClassCastException("Wrong answer after DATA command. Expected DATA_RESPONSE");
            }
            MasterCommand command = (MasterCommand) serializer.deserialize(targetMaster.readObject());
            LOGGER.info(String.format("[%s] command read by the master: %s", command,
                    targetMaster.getNetworkInfo().toString()));
            if (command.equals(MasterCommand.RESULTS)) {
                SamplingFunction<?> samplingFunction = (SamplingFunction<?>) serializer
                        .deserialize(targetMaster.readObject());
                LOGGER.info("The simulation results have been received correctly");
            } else {
                throw new ClassCastException("Wrong command from master. Expected RESULTS");
            }
        } catch (ClassCastException e) {
            LOGGER.severe(String.format("[%s] Message cast failure during the simulation submit - Master: %s", e.getMessage(), targetMaster.getNetworkInfo().toString()));
            throw new IOException();
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the simulation submit - Master: %s", e.getMessage(), targetMaster.getNetworkInfo().toString()));
            throw new IOException();
        }
    }

    /**
     * Sends a ping command to the given master server
     *
     * @param targetMaster NetworkManager to the master server
     */
    private void sendPing(TCPNetworkManager targetMaster) throws IOException {
        try {
            targetMaster.writeObject(serializer.serialize(ClientCommand.PING));
            LOGGER.info(String.format("[%s] command sent to the master: %s", ClientCommand.PING,
                    targetMaster.getNetworkInfo().toString()));
            LOGGER.info("Ping has been sent to the master");

            MasterCommand answer = (MasterCommand) serializer.deserialize(targetMaster.readObject());
            if (answer.equals(MasterCommand.PONG)) {
                LOGGER.info(String.format("Answer received: [%s] - Master: %s", answer, targetMaster.getNetworkInfo().toString()));
            } else {
                LOGGER.severe("Wrong answer after PING command. Expected PONG");
            }
        } catch (ClassCastException e) {
            LOGGER.severe(String.format("[%s] Message cast failure during the ping - Master: %s", e.getMessage(), targetMaster.getNetworkInfo().toString()));
            throw new IOException();
        } catch (IOException e) {
            LOGGER.severe(String.format("[%s] Network communication failure during the ping - Master: %s", e.getMessage(), targetMaster.getNetworkInfo().toString()));
            throw new IOException();
        }

    }

}
