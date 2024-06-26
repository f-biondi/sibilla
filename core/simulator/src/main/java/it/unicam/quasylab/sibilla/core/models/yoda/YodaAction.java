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

package it.unicam.quasylab.sibilla.core.models.yoda;

import it.unicam.quasylab.sibilla.core.models.agents.VariableMapping;
import it.unicam.quasylab.sibilla.core.util.values.SibillaValue;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.Serializable;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * The interface <code>YodaAction</code> represents
 * the action done by agents in a system.
 *
 */
public interface YodaAction extends Serializable {

    /**
     * This method returns the name of this action
     *
     * @return the name of this action
     */
    String getName();

    /**
     * This method returns the new internal state of an agent after performing an action
     *
     * @param rg a random generator
     * @param currentState the agent current internal state
     * @return the new internal state of an agent after performing an action
     */
    YodaVariableMapping performAction(RandomGenerator rg, YodaVariableMapping currentState);


    /**
     * Returns the action with the given name that transforms the state according to the given function.
     *
     * @param name action name
     * @param f action function
     * @return returns the action with the given name that transforms the state according to the given function.
     */
    static YodaAction actionOf(String name, BiFunction<RandomGenerator, YodaVariableMapping, YodaVariableMapping> f) {
        return new YodaAction() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public YodaVariableMapping performAction(RandomGenerator rg, YodaVariableMapping currentState) {
                return f.apply(rg, currentState);
            }
        };
    }


    static YodaAction actionOf(String name, Map<YodaVariable, BiFunction<RandomGenerator, YodaVariableMapping, SibillaValue>> updates) {
        return actionOf(name, (rg, vm) -> {
            YodaVariableMapping result = vm;
            for (Map.Entry<YodaVariable, BiFunction<RandomGenerator, YodaVariableMapping, SibillaValue>> e: updates.entrySet()) {
                result = result.setValue(e.getKey(), e.getValue().apply(rg, vm));
            }
            return result;
        });
    }

}
