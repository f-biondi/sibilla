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

package it.unicam.quasylab.sibilla.examples.lio.seir;

import it.unicam.quasylab.sibilla.core.models.EvaluationEnvironment;
import it.unicam.quasylab.sibilla.core.models.ParametricValue;
import it.unicam.quasylab.sibilla.core.models.ParametricDataSet;
import it.unicam.quasylab.sibilla.core.models.pm.*;
import it.unicam.quasylab.sibilla.core.models.pm.util.PopulationRegistry;
import it.unicam.quasylab.sibilla.core.util.values.SibillaValue;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class CovidDefinition {

    public final static int SCALE = 150;
    public final static int INIT_S = 99*SCALE;
    public final static int INIT_A = 1*SCALE;
    public final static int INIT_G = 0*SCALE;
    public final static int INIT_R = 0*SCALE;
    public final static int INIT_D = 0*SCALE;
    public final static double N = INIT_S + INIT_A + INIT_G + INIT_R + INIT_D;

    public final static double LAMBDA_MEET = 4;
    public final static double PROB_TRANSMISSION = 0.1;
    public final static double LAMBDA_R_A = 1 / 7.0;
    public final static double LAMBDA_R_G = 1 / 15.0;

    private final static double PROB_ASINT = 0.8;
    private final static double PROB_A_G = 0.5;
    private final static double PROB_DEATH = 0.02;


    public static PopulationRegistry generatePopulationRegistry(EvaluationEnvironment environment) {
        return PopulationRegistry.createRegistry("S","A","G","R","D");
    }

    public static List<PopulationRule> getRules(EvaluationEnvironment environment, PopulationRegistry registry) {
        int S = registry.indexOf("S");
        int A = registry.indexOf("A");
        int G = registry.indexOf("G");
        int R = registry.indexOf("R");
        int D = registry.indexOf("D");

        LinkedList<PopulationRule> rules = new LinkedList<>();
        double lambda = environment.get("lambdaMeet").doubleOf();
        PopulationRule rule_S_A_A = new ReactionRule(
                "S->A",
                new Population[] { new Population(S), new Population(A)} ,
                new Population[] { new Population(A), new Population(A)},
                (t,s) -> SibillaValue.of(PROB_ASINT*s.getOccupancy(S)* PROB_TRANSMISSION*lambda *(s.getOccupancy(A)/N)));

        PopulationRule rule_S_G_A = new ReactionRule(
                "S->G",
                new Population[] { new Population(S), new Population(A)} ,
                new Population[] { new Population(G), new Population(A)},
                (t,s) -> SibillaValue.of((1-PROB_ASINT)*s.getOccupancy(S)* PROB_TRANSMISSION*lambda *(s.getOccupancy(A)/N)));

        PopulationRule rule_S_A_G = new ReactionRule(
                "S->A",
                new Population[] { new Population(S), new Population(G)} ,
                new Population[] { new Population(A), new Population(G)},
                (t,s) -> SibillaValue.of(PROB_ASINT*s.getOccupancy(S)* PROB_TRANSMISSION*lambda *(s.getOccupancy(G)/N)));

        PopulationRule rule_S_G_G = new ReactionRule(
                "S->A",
                new Population[] { new Population(S), new Population(G)} ,
                new Population[] { new Population(G), new Population(G)},
                (t,s) -> SibillaValue.of((1-PROB_ASINT)*s.getOccupancy(S)* PROB_TRANSMISSION*lambda *(s.getOccupancy(G)/N)));

        PopulationRule rule_A_R = new ReactionRule(
                "I->R",
                new Population[] { new Population(A) },
                new Population[] { new Population(R) },
                (t,s) -> SibillaValue.of(s.getOccupancy(A)*LAMBDA_R_A*(1-PROB_A_G))
        );

        PopulationRule rule_A_G = new ReactionRule(
                "I->R",
                new Population[] { new Population(A) },
                new Population[] { new Population(G) },
                (t,s) -> SibillaValue.of(s.getOccupancy(A)*LAMBDA_R_A*PROB_A_G)
        );

        PopulationRule rule_G_R = new ReactionRule(
                "I->R",
                new Population[] { new Population(G) },
                new Population[] { new Population(R) },
                (t,s) -> SibillaValue.of(s.getOccupancy(G)*LAMBDA_R_G*(1-PROB_DEATH))
        );

        PopulationRule rule_G_D = new ReactionRule(
                "I->R",
                new Population[] { new Population(G) },
                new Population[] { new Population(D) },
                (t,s) -> SibillaValue.of(s.getOccupancy(G)*LAMBDA_R_G*PROB_DEATH)
        );
        rules.add(rule_S_A_A);
        rules.add(rule_S_G_A);
        rules.add(rule_S_A_G);
        rules.add(rule_S_G_G);
        rules.add(rule_A_G);
        rules.add(rule_A_R);
        rules.add(rule_G_R);
        rules.add(rule_G_D);
        return rules;
    }



    public static ParametricDataSet<Function<RandomGenerator,PopulationState>> states(EvaluationEnvironment environment, PopulationRegistry registry) {
        ParametricDataSet<Function<RandomGenerator,PopulationState>> states = new ParametricDataSet<>();
        ParametricValue<Function<RandomGenerator,PopulationState>> state = defaultState(registry);
        states.setDefaultState( state );
        states.set("state", parametricState(registry));
        return states;
    }

    public static ParametricValue<Function<RandomGenerator,PopulationState>> defaultState(PopulationRegistry registry) {
        int S = registry.indexOf("S");
        int A = registry.indexOf( "A");
        int G = registry.indexOf("G");
        int R = registry.indexOf("R");
        int D = registry.indexOf("D");
        PopulationState state = new PopulationState( new int[] { INIT_S, INIT_A, INIT_G, INIT_R, INIT_D } );
        return new ParametricValue<>( rg -> state );
    }


    public static ParametricValue<Function<RandomGenerator,PopulationState>> parametricState(PopulationRegistry registry) {
        int S = registry.indexOf("S");
        int A = registry.indexOf( "A");
        int G = registry.indexOf("G");
        int R = registry.indexOf("R");
        int D = registry.indexOf("D");
        return new ParametricValue<>( new String[] { "s", "a", "g", "r", "d"}
                , args -> {
                    PopulationState state = new PopulationState( new int[] {
                        (int) args[S], (int) args[A],
                    (int) args[G],
                    (int) args[R],
                    (int) args[D] });
            return rg -> state;
        } );
        }

}
