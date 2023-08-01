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

package it.unicam.quasylab.sibilla.core.util.values;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;

/**
 * Instances of this interface represent values in a Sibilla model.
 */
public interface SibillaValue {

    SibillaValue ERROR_VALUE = SibillaErrorValue.INSTANCE;

    static SibillaValue and(SibillaValue v1, SibillaValue v2) {
        if ((v1 instanceof SibillaBoolean)&&(v2 instanceof SibillaBoolean)) {
            return ((SibillaBoolean) v1).and((SibillaBoolean) v2);
        } else {
            return SibillaValue.ERROR_VALUE;
        }
    }

    static SibillaValue or(SibillaValue v1, SibillaValue v2) {
        if ((v1 instanceof SibillaBoolean)&&(v2 instanceof SibillaBoolean)) {
            return ((SibillaBoolean) v1).or((SibillaBoolean) v2);
        } else {
            return SibillaValue.ERROR_VALUE;
        }
    }

    static SibillaValue not(SibillaValue v1) {
        if (v1 instanceof SibillaBoolean) {
            return ((SibillaBoolean) v1).not();
        } else {
            return SibillaValue.ERROR_VALUE;
        }
    }

    static SibillaValue sum(SibillaValue v1, SibillaValue v2) {
        if (((v1 instanceof SibillaDouble)&&((v2 instanceof SibillaDouble)||(v2 instanceof SibillaInteger)))||
                ((v2 instanceof SibillaDouble)&&(v1 instanceof SibillaInteger))) {
            return new SibillaDouble(v1.doubleOf()+ v2.doubleOf());
        }
        if ((v1 instanceof SibillaInteger)&&(v2 instanceof SibillaInteger)) {
            return new SibillaInteger(v1.intOf()+v2.intOf());
        }
        return SibillaValue.ERROR_VALUE;
    }

    static SibillaValue sub(SibillaValue v1, SibillaValue v2) {
        if (((v1 instanceof SibillaDouble)&&((v2 instanceof SibillaDouble)||(v2 instanceof SibillaInteger)))||
                ((v2 instanceof SibillaDouble)&&(v1 instanceof SibillaInteger))) {
            return new SibillaDouble(v1.doubleOf()- v2.doubleOf());
        }
        if ((v1 instanceof SibillaInteger)&&(v2 instanceof SibillaInteger)) {
            return new SibillaInteger(v1.intOf()-v2.intOf());
        }
        return SibillaValue.ERROR_VALUE;
    }

    static SibillaValue mod(SibillaValue v1, SibillaValue v2) {
        if (((v1 instanceof SibillaDouble)&&((v2 instanceof SibillaDouble)||(v2 instanceof SibillaInteger)))||
                ((v2 instanceof SibillaDouble)&&(v1 instanceof SibillaInteger))) {
            return new SibillaDouble(v1.doubleOf() % v2.doubleOf());
        }
        if ((v1 instanceof SibillaInteger)&&(v2 instanceof SibillaInteger)) {
            return new SibillaInteger(v1.intOf() % v2.intOf());
        }
        return SibillaValue.ERROR_VALUE;
    }

    static SibillaValue mul(SibillaValue v1, SibillaValue v2) {
        if (((v1 instanceof SibillaDouble)&&((v2 instanceof SibillaDouble)||(v2 instanceof SibillaInteger)))||
                ((v2 instanceof SibillaDouble)&&(v1 instanceof SibillaInteger))) {
            return new SibillaDouble(v1.doubleOf() * v2.doubleOf());
        }
        if ((v1 instanceof SibillaInteger)&&(v2 instanceof SibillaInteger)) {
            return new SibillaInteger(v1.intOf() * v2.intOf());
        }
        return SibillaValue.ERROR_VALUE;
    }

    static SibillaValue div(SibillaValue v1, SibillaValue v2) {
        if (((v1 instanceof SibillaDouble)&&((v2 instanceof SibillaDouble)||(v2 instanceof SibillaInteger)))||
                ((v2 instanceof SibillaDouble)&&(v1 instanceof SibillaInteger))) {
            return new SibillaDouble(v1.doubleOf() / v2.doubleOf());
        }
        if ((v1 instanceof SibillaInteger)&&(v2 instanceof SibillaInteger)) {
            return new SibillaInteger(v1.intOf() / v2.intOf());
        }
        return SibillaValue.ERROR_VALUE;
    }

    static SibillaValue zeroDiv(SibillaValue v1, SibillaValue v2) {
        if (((v1 instanceof SibillaDouble)&&((v2 instanceof SibillaDouble)||(v2 instanceof SibillaInteger)))||
                ((v2 instanceof SibillaDouble)&&(v1 instanceof SibillaInteger))) {
            if (v2.doubleOf() != 0.0) {
                return new SibillaDouble(v1.doubleOf() / v2.doubleOf());
            } else {
                return new SibillaDouble(0);
            }
        }
        if ((v1 instanceof SibillaInteger)&&(v2 instanceof SibillaInteger)) {
            if (v2.intOf() != 0) {
                return new SibillaInteger(v1.intOf() / v2.intOf());
            } else {
                return new SibillaInteger(0);
            }
        }
        return SibillaValue.ERROR_VALUE;
    }

    static SibillaValue minus(SibillaValue v) {
        if (v instanceof SibillaInteger) {
            return new SibillaInteger(-v.intOf());
        }
        if (v instanceof SibillaDouble) {
            return new SibillaDouble(-v.doubleOf());
        }
        return SibillaValue.ERROR_VALUE;
    }

    static SibillaValue eval(DoubleBinaryOperator op, SibillaValue v1, SibillaValue v2) {
        return new SibillaDouble(op.applyAsDouble(v1.doubleOf(), v2.doubleOf()));
    }

    /**
     * Returns the sibilla value representing the double value <code>v</code>.
     *
     * @param v a double value.
     * @return the sibilla value representing the double value <code>v</code>.
     */
    static SibillaValue of(double v) {
        return new SibillaDouble(v);
    }

    /**
     * Returns the sibilla value representing the int value <code>v</code>.
     *
     * @param v a double value.
     * @return the sibilla value representing the int value <code>v</code>.
     */
    static SibillaValue of(int v) {
        return new SibillaInteger(v);
    }

    /**
     * Returns the sibilla value representing the boolean value <code>v</code>.
     *
     * @param v a double value.
     * @return the sibilla value representing the boolean value <code>v</code>.
     */
    static SibillaValue of(boolean v) {
        if (v) {
            return SibillaBoolean.TRUE;
        } else {
            return SibillaBoolean.FALSE;
        }
    }



    /**
     * Returns the double representation of this value. {@link Double#NaN} is returned if this
     * value has not a double representation.
     *
     * @return the double representation of this value.
     */
    double doubleOf();

    /**
     * Returns the boolean representation of this value.
     *
     * @return the boolean representation of this value.
     */
    boolean booleanOf();

    /**
     * Returns the integer representation of this value.
     *
     * @return the integer representation of this value.
     */
    int intOf();


    /**
     * Returns the type associated with this value.
     *
     * @return the type associated with this value.
     */
    SibillaType<?> getType();

    public static BiPredicate<SibillaValue,SibillaValue> getRelationOperator(String op) {
        if (op.equals("<"))  { return (x,y) -> x.doubleOf()<y.doubleOf(); }
        if (op.equals("<="))  { return (x,y) -> x.doubleOf()<=y.doubleOf(); }
        if (op.equals("=="))  { return (x,y) -> x.doubleOf()==y.doubleOf(); }
        if (op.equals("!="))  { return (x,y) -> !x.equals(y); }
        if (op.equals(">"))  { return (x,y) -> x.doubleOf()>y.doubleOf(); }
        if (op.equals(">="))  { return (x,y) -> x.doubleOf()>=y.doubleOf(); }
        return (x,y) -> false;
    }


    public static BinaryOperator<SibillaValue> getOperator(String op) {
        if (op.equals("+")) {return SibillaValue::sum;}
        if (op.equals("-")) {return SibillaValue::sub; }
        if (op.equals("%")) {return SibillaValue::mod; }
        if (op.equals("*")) {return SibillaValue::mul; }
        if (op.equals("/")) {return SibillaValue::div; }
        if (op.equals("//")) {return SibillaValue::zeroDiv; }
        return (x,y) -> SibillaValue.ERROR_VALUE;
    }
}
