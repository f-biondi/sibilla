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

package quasylab.sibilla.core.simulator.sampling;

import quasylab.sibilla.core.models.MeasureFunction;
import quasylab.sibilla.core.models.State;

import java.io.Serializable;
import java.util.function.Function;

public class SimpleMeasure<S extends State> implements Measure<S> {

    private final String name;
    private final MeasureFunction<S> measure;

    public SimpleMeasure(String name, MeasureFunction<S> measure) {
        this.name = name;
        this.measure = measure;
    }


    @Override
    public double measure(S t) {
        return measure.apply(t);
    }

    @Override
    public String getName() {
        return name;
    }
}
