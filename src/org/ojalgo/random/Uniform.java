/*
 * Copyright 1997-2017 Optimatika (www.optimatika.se)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.random;

import static org.ojalgo.constant.PrimitiveMath.*;

import org.ojalgo.function.PrimitiveFunction;

/**
 * Certain waiting times. Rounding errors.
 *
 * @author apete
 */
public class Uniform extends AbstractContinuous {

    private static final long serialVersionUID = -8198257914507986404L;

    /**
     * @return An integer: 0 &lt;= ? &lt; limit
     */
    public static int randomInteger(final int limit) {
        return (int) PrimitiveFunction.FLOOR.invoke(limit * Math.random());
    }

    /**
     * @return An integer: lower &lt;= ? &lt; higher
     */
    public static int randomInteger(final int lower, final int higher) {
        return lower + Uniform.randomInteger(higher - lower);
    }

    /**
     * @return An integer: 0 &lt;= ? &lt; limit
     */
    public static long randomInteger(final long limit) {
        return (long) PrimitiveFunction.FLOOR.invoke(limit * Math.random());
    }

    private final double myLower;
    private final double myRange;

    public Uniform() {
        this(ZERO, ONE);
    }

    public Uniform(final double lower, final double range) {

        super();

        if (range <= ZERO) {
            throw new IllegalArgumentException("The range must be larger than 0.0!");
        }

        myLower = lower;
        myRange = range;
    }

    public double getDistribution(final double value) {

        double retVal = ZERO;

        if ((value <= (myLower + myRange)) && (myLower <= value)) {
            retVal = (value - myLower) / myRange;
        } else if (myLower <= value) {
            retVal = ONE;
        }

        return retVal;
    }

    public double getExpected() {
        return myLower + (myRange / TWO);
    }

    public double getProbability(final double value) {

        double retVal = ZERO;

        if ((myLower <= value) && (value <= (myLower + myRange))) {
            retVal = ONE / myRange;
        }

        return retVal;
    }

    public double getQuantile(final double probality) {

        this.checkProbabilty(probality);

        return myLower + (probality * myRange);
    }

    @Override
    public double getVariance() {
        return (myRange * myRange) / TWELVE;
    }

    @Override
    protected double generate() {
        return myLower + (myRange * this.random().nextDouble());
    }
}
