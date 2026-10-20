/*
 * Automaton
 * Copyright (c) 2026, Julien Menana, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package fr.menana.automaton;

final class WeightMath {
    private WeightMath() { }
    static double finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Finite weight required: " + value);
        return value == 0 ? 0 : value;
    }
    static double add(double a, double b) {
        double sum = a + b;
        if (!Double.isFinite(sum)) throw new ArithmeticException("Cost overflow");
        return sum == 0 ? 0 : sum;
    }
    static double multiply(double a, double b) {
        double product = a * b;
        if (!Double.isFinite(product)) throw new ArithmeticException("Cost overflow");
        return product == 0 ? 0 : product;
    }
    static double[] copy(double[] values, int size) {
        if (values.length != size) throw new IllegalArgumentException("Expected " + size + " weights, got " + values.length);
        double[] result = values.clone();
        for (int i = 0; i < result.length; i++) result[i] = finite(result[i]);
        return result;
    }
    /** Sufficient certificate for exact sums of a bounded number of dyadic doubles. */
    static final class Exactness {
        private int exponent = Integer.MAX_VALUE;
        private double largest;
        void include(double value) {
            if (value == 0) return;
            largest = Math.max(largest, Math.abs(value));
            long bits = Double.doubleToRawLongBits(Math.abs(value));
            int e = (int) (bits >>> 52);
            long mantissa = bits & ((1L << 52) - 1);
            if (e != 0) mantissa |= 1L << 52;
            int quantum = (e == 0 ? -1074 : e - 1023 - 52) + Long.numberOfTrailingZeros(mantissa);
            exponent = Math.min(exponent, quantum);
        }
        boolean safe(double terms) {
            return largest == 0 || (largest <= Double.MAX_VALUE / terms && Math.scalb(largest, -exponent) <= 0x1.0p53 / terms);
        }
    }
}
