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

package it.unicam.quasylab.sibilla.langs.yoda;

import it.unicam.quasylab.sibilla.langs.util.ErrorCollector;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import javax.xml.validation.Validator;
import java.util.HashMap;
import java.util.Map;

public class ModelValidator {

    private final ErrorCollector errorCollector;
    private final Map<String, Token> table;
    private final Map<String, DataType> types;


    public ModelValidator(ErrorCollector errorCollector) {
        this.errorCollector = errorCollector;
        this.table = new HashMap<>();
        this.types = new HashMap<>();
    }

    public boolean validate(ParseTree parseTree){
        if (parseTree==null){
            return false;
        }
        return parseTree.accept(new ValidatorVisitor());
    }


    public class ValidatorVisitor extends  YodaModelBaseVisitor<Boolean>{


    }
}
