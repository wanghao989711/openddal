/*
 * Copyright 2014-2016 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openddal.value;

import com.openddal.message.DbException;
import com.openddal.message.ErrorCode;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implementation of the REAL data type.
 */
public class ValueFloat extends Value {

    /**
     * Float.floatToIntBits(0.0F).
     */
    public static final int ZERO_BITS = Float.floatToIntBits(0.0F);

    /**
     * The precision in digits.
     */
    static final int PRECISION = 7;

    /**
     * The maximum display size of a float.
     * Example: -1.12345676E-20
     */
    static final int DISPLAY_SIZE = 15;

    private static final ValueFloat ZERO = new ValueFloat(0.0F);
    private static final ValueFloat ONE = new ValueFloat(1.0F);

    private final float value;

    private ValueFloat(float value) {
        this.value = value;
    }

    /**
     * Get or create float value for the given float.
     *
     * @param d the float
     * @return the value
     */
    public static ValueFloat get(float d) {
        if (d == 1.0F) {
            return ONE;
        } else if (d == 0.0F) {
            // unfortunately, -0.0 == 0.0, but we don't want to return
            // 0.0 in this case
            if (Float.floatToIntBits(d) == ZERO_BITS) {
                return ZERO;
            }
        }
        return (ValueFloat) cache(new ValueFloat(d));
    }

    @Override
    public Value add(Value v) {
        ValueFloat v2 = (ValueFloat) v;
        return ValueFloat.get(value + v2.value);
    }

    @Override
    public Value subtract(Value v) {
        ValueFloat v2 = (ValueFloat) v;
        return ValueFloat.get(value - v2.value);
    }

    @Override
    public Value negate() {
        return ValueFloat.get(-value);
    }

    @Override
    public Value multiply(Value v) {
        ValueFloat v2 = (ValueFloat) v;
        return ValueFloat.get(value * v2.value);
    }

    @Override
    public Value divide(Value v) {
        ValueFloat v2 = (ValueFloat) v;
        if (v2.value == 0.0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getSQL());
        }
        return ValueFloat.get(value / v2.value);
    }

    @Override
    public Value modulus(Value v) {
        ValueFloat other = (ValueFloat) v;
        if (other.value == 0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getSQL());
        }
        return ValueFloat.get(value % other.value);
    }

    @Override
    public String getSQL() {
        if (value == Float.POSITIVE_INFINITY) {
            return "POWER(0, -1)";
        } else if (value == Float.NEGATIVE_INFINITY) {
            return "(-POWER(0, -1))";
        } else if (Double.isNaN(value)) {
            // NaN
            return "SQRT(-1)";
        }
        String s = getString();
        if (s.equals("-0.0")) {
            return "-CAST(0 AS REAL)";
        }
        return s;
    }

    @Override
    public int getType() {
        return FLOAT;
    }

    @Override
    protected int compareSecure(Value o, CompareMode mode) {
        ValueFloat v = (ValueFloat) o;
        return Float.compare(value, v.value);
    }

    @Override
    public int getSignum() {
        return value == 0 ? 0 : (value < 0 ? -1 : 1);
    }

    @Override
    public float getFloat() {
        return value;
    }

    @Override
    public String getString() {
        return String.valueOf(value);
    }

    @Override
    public long getPrecision() {
        return PRECISION;
    }

    @Override
    public int getScale() {
        return 0;
    }

    @Override
    public int hashCode() {
        long hash = Float.floatToIntBits(value);
        return (int) (hash ^ (hash >> 32));
    }

    @Override
    public Object getObject() {
        return Float.valueOf(value);
    }

    @Override
    public void set(PreparedStatement prep, int parameterIndex)
            throws SQLException {
        prep.setFloat(parameterIndex, value);
    }

    @Override
    public int getDisplaySize() {
        return DISPLAY_SIZE;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ValueFloat)) {
            return false;
        }
        return compareSecure((ValueFloat) other, null) == 0;
    }

}
