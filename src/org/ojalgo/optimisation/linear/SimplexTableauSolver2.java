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
package org.ojalgo.optimisation.linear;

import static org.ojalgo.constant.PrimitiveMath.*;
import static org.ojalgo.function.PrimitiveFunction.*;

import java.util.Arrays;

import org.ojalgo.access.Access1D;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.BasicArray;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.AggregatorFunction;
import org.ojalgo.function.aggregator.PrimitiveAggregator;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.PrimitiveDenseStore;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.linear.SimplexTableau.IterationPoint;
import org.ojalgo.type.IndexSelector;

/**
 * SimplexTableauSolver
 *
 * @author apete
 */
public final class SimplexTableauSolver2 extends AbstractLinearSolver {

    private final IndexSelector mySelector;

    static final class PivotPoint {

        private int myRowObjective = -1;

        private final SimplexTableauSolver2 mySolver;
        int col = -1;
        int row = -1;

        PivotPoint(final SimplexTableauSolver2 solver) {

            super();

            mySolver = solver;

            myRowObjective = mySolver.myTableau.countConstraints() + 1;

            this.reset();
        }

        int getColRHS() {
            return mySolver.myTableau.countConstraints() + mySolver.myTableau.countVariables();
        }

        int getRowObjective() {
            return myRowObjective;
        }

        boolean isPhase1() {
            return myRowObjective == (mySolver.myTableau.countConstraints() + 1);
        }

        boolean isPhase2() {
            return myRowObjective == mySolver.myTableau.countConstraints();
        }

        double objective() {
            return mySolver.myTableau.doubleValue(this.getRowObjective(), mySolver.myTableau.countConstraints() + mySolver.myTableau.countVariables());
        }

        int phase() {
            return myRowObjective == mySolver.myTableau.countConstraints() ? 2 : 1;
        }

        void reset() {
            row = -1;
            col = -1;
        }

        void switchToPhase2() {
            myRowObjective = mySolver.myTableau.countConstraints();
        }

    }

    private final int[] myBasis;
    private final PivotPoint myPoint;
    private final SimplexTableau myTableau;

    public SimplexTableauSolver2(final SparseTableau tableau, final Optimisation.Options solverOptions) {

        super(solverOptions);

        myTableau = tableau;

        myPoint = new PivotPoint(this);

        mySelector = new IndexSelector(tableau.countVariables());
        myBasis = BasicArray.makeIncreasingRange(-myTableau.countConstraints(), myTableau.countConstraints());

        if (this.isDebug() && this.isTableauPrintable()) {
            this.logDebugTableau("Tableau Created");
        }
    }

    public int[] getBasis() {
        return myBasis.clone();
    }

    public double[] getDualVariables() {

        this.logDebugTableau("Tableau extracted");

        final double[] retVal = new double[myTableau.countConstraints()];

        final int tmpRowObjective = myTableau.countConstraints();

        final int tmpCountVariables = myTableau.countVariables();
        for (int j = 0; j < retVal.length; j++) {
            // retVal[j] = -myTransposedTableau.doubleValue(tmpCountVariables + j, tmpRowObjective);
            retVal[j] = -myTableau.doubleValue(tmpRowObjective, tmpCountVariables + j);
        }

        return retVal;
    }

    public double[] getResidualCosts() {

        this.logDebugTableau("Tableau extracted");

        final double[] retVal = new double[myTableau.countVariables()];

        final int tmpRowObjective = myTableau.countConstraints();

        // final double tmpObjVal = myTransposedTableau.doubleValue(32, tmpRowObjective);

        for (int j = 0; j < retVal.length; j++) {
            // retVal[j] = myTransposedTableau.doubleValue(j, tmpRowObjective);
            retVal[j] = myTableau.doubleValue(tmpRowObjective, j);
        }

        return retVal;
    }

    public Result solve(final Result kickStarter) {

        while (this.needsAnotherIteration()) {

            this.performIteration(myPoint.row, myPoint.col);

            if (this.isDebug() && this.isTableauPrintable()) {
                this.logDebugTableau("Tableau Iteration");
            }
        }

        return this.buildResult();
    }

    private int countBasicArtificials() {
        int retVal = 0;
        final int tmpLength = myBasis.length;
        for (int i = 0; i < tmpLength; i++) {
            if (myBasis[i] < 0) {
                retVal++;
            }
        }
        return retVal;
    }

    protected final int countBasisDeficit() {
        return myTableau.countConstraints() - mySelector.countIncluded();
    }

    protected final void exclude(final int anIndexToExclude) {
        mySelector.exclude(anIndexToExclude);
    }

    protected final int[] getExcluded() {
        return mySelector.getExcluded();
    }

    protected final int[] getIncluded() {
        return mySelector.getIncluded();
    }

    protected final void include(final int anIndexToInclude) {
        mySelector.include(anIndexToInclude);
    }

    protected final void include(final int[] someIndecesToInclude) {
        mySelector.include(someIndecesToInclude);
    }

    private boolean isBasicArtificials() {
        final int tmpLength = myBasis.length;
        for (int i = 0; i < tmpLength; i++) {
            if (myBasis[i] < 0) {
                return true;
            }
        }
        return false;
    }

    private final boolean isTableauPrintable() {
        return myTableau.count() <= 512L;
    }

    private final void logDebugTableau(final String message) {
        this.debug(message + "; Basics: " + Arrays.toString(myBasis), myTableau);
        // this.debug("New/alt " + message + "; Basics: " + Arrays.toString(myBasis), myTableau);
    }

    @Override
    protected double evaluateFunction(final Access1D<?> solution) {
        return myPoint.objective();
    }

    /**
     * Extract solution MatrixStore from the tableau
     */
    @Override
    protected PhysicalStore<Double> extractSolution() {

        final int tmpCountVariables = myTableau.countConstraints() + myTableau.countVariables();

        final PrimitiveDenseStore solution = PrimitiveDenseStore.FACTORY.makeZero(myTableau.countVariables(), 1);

        final int tmpLength = myBasis.length;
        for (int i = 0; i < tmpLength; i++) {
            final int tmpBasisIndex = myBasis[i];
            if (tmpBasisIndex >= 0) {
                // this.setX(tmpBasisIndex, myTransposedTableau.doubleValue(tmpCountVariables, i));
                solution.set(tmpBasisIndex, myTableau.doubleValue(i, tmpCountVariables));
            }
        }

        return solution;

    }

    @Override
    protected boolean initialise(final Result kickStarter) {
        return false;
    }

    @Override
    protected boolean needsAnotherIteration() {

        if (this.isDebug()) {
            this.debug("\nNeeds Another Iteration? Phase={} Artificials={} Objective={}", myPoint.phase(), this.countBasisDeficit(), myPoint.objective());
        }

        boolean retVal = false;
        myPoint.reset();

        if (myPoint.isPhase1()) {

            // final double tmpPhaseOneValue = myTransposedTableau.doubleValue(this.countConstraints() + this.countVariables(), myPoint.getRowObjective());
            final double tmpPhaseOneValue = myTableau.doubleValue(myPoint.getRowObjective(), myTableau.countConstraints() + myTableau.countVariables());

            if (!this.isBasicArtificials() || options.objective.isZero(tmpPhaseOneValue)) {

                if (this.isDebug()) {
                    this.debug("\nSwitching to Phase2 with {} artificial variable(s) still in the basis.\n", this.countBasicArtificials());
                    // this.debug("Reduced artificial costs:\n{}", this.sliceTableauRow(myPoint.getRowObjective()).copy(this.getExcluded()));
                }

                myPoint.switchToPhase2();
            }
        }

        myPoint.col = this.findNextPivotCol();

        if (myPoint.col >= 0) {

            myPoint.row = this.findNextPivotRow();

            if (myPoint.row >= 0) {

                retVal = true;

            } else {

                if (myPoint.isPhase2()) {

                    this.setState(State.UNBOUNDED);
                    retVal = false;

                } else {

                    this.setState(State.INFEASIBLE);
                    retVal = false;
                }
            }

        } else {

            if (myPoint.isPhase1()) {

                this.setState(State.INFEASIBLE);
                retVal = false;

            } else {

                this.setState(State.OPTIMAL);
                retVal = false;
            }
        }

        if (this.isDebug()) {
            if (retVal) {
                this.debug("\n==>>\tRow: {},\tExit: {},\tColumn/Enter: {}.\n", myPoint.row, myBasis[myPoint.row], myPoint.col);
            } else {
                this.debug("\n==>>\tNo more iterations needed/possible.\n");
            }
        }

        return retVal;
    }

    @Override
    protected boolean validate() {

        final boolean retVal = true;
        this.setState(State.VALID);

        return retVal;
    }

    int findNextPivotCol() {

        final int[] tmpExcluded = this.getExcluded();

        if (this.isDebug()) {
            if (options.validate) {
                this.debug("\nfindNextPivotCol (index of most negative value) among these:\n{}",
                        Array1D.PRIMITIVE64.copy(myTableau.sliceTableauRow(myPoint.getRowObjective())).copy(tmpExcluded));
            } else {
                this.debug("\nfindNextPivotCol");
            }
        }

        int retVal = -1;

        double tmpVal;
        double tmpMinVal = myPoint.isPhase2() ? -options.problem.epsilon() : ZERO;
        //double tmpMinVal = ZERO;

        int tmpCol;

        for (int e = 0; e < tmpExcluded.length; e++) {
            tmpCol = tmpExcluded[e];
            // tmpVal = myTransposedTableau.doubleValue(tmpCol, myPoint.getRowObjective());
            tmpVal = myTableau.doubleValue(myPoint.getRowObjective(), tmpCol);
            if (tmpVal < tmpMinVal) {
                retVal = tmpCol;
                tmpMinVal = tmpVal;
                if (this.isDebug()) {
                    this.debug("Col: {}\t=>\tReduced Contribution Weight: {}.", tmpCol, tmpVal);
                }
            }
        }

        return retVal;
    }

    int findNextPivotRow() {

        final int tmpNumerCol = myPoint.getColRHS();
        final int tmpDenomCol = myPoint.col;

        if (this.isDebug()) {
            if (options.validate) {
                final Access1D<Double> tmpNumerators = myTableau.sliceTableauColumn(tmpNumerCol);
                final Access1D<Double> tmpDenominators = myTableau.sliceTableauColumn(tmpDenomCol);
                final Array1D<Double> tmpRatios = Array1D.PRIMITIVE64.copy(tmpNumerators);
                tmpRatios.modifyMatching(DIVIDE, tmpDenominators);
                this.debug("\nfindNextPivotRow (smallest positive ratio) among these:\nNumerators={}\nDenominators={}\nRatios={}", tmpNumerators,
                        tmpDenominators, tmpRatios);
            } else {
                this.debug("\nfindNextPivotRow");
            }
        }

        int retVal = -1;
        double tmpNumer = NaN, tmpDenom = NaN, tmpRatio = NaN;
        double tmpMinRatio = MACHINE_LARGEST;

        final int tmpConstraintsCount = myTableau.countConstraints();

        final boolean tmpPhase2 = myPoint.isPhase2();

        for (int i = 0; i < tmpConstraintsCount; i++) {

            // Phase 2 with artificials still in the basis
            final boolean tmpSpecialCase = tmpPhase2 && (myBasis[i] < 0);

            // tmpDenom = myTransposedTableau.doubleValue(tmpDenomCol, i);
            tmpDenom = myTableau.doubleValue(i, tmpDenomCol);

            // Should always be >=0.0, but very small numbers may "accidentally" get a negative sign.
            // tmpNumer = PrimitiveFunction.ABS.invoke(myTransposedTableau.doubleValue(tmpNumerCol, i));
            tmpNumer = PrimitiveFunction.ABS.invoke(myTableau.doubleValue(i, tmpNumerCol));

            if (options.problem.isSmall(tmpNumer, tmpDenom)) {

                tmpRatio = MACHINE_LARGEST;

            } else {

                if (tmpSpecialCase) {
                    if (options.problem.isSmall(tmpDenom, tmpNumer)) {
                        tmpRatio = MACHINE_EPSILON;
                    } else {
                        tmpRatio = MACHINE_LARGEST;
                    }
                } else {
                    tmpRatio = tmpNumer / tmpDenom;
                }
            }

            if ((tmpSpecialCase || (tmpDenom > ZERO)) && (tmpRatio >= ZERO) && (tmpRatio < tmpMinRatio)) {

                retVal = i;
                tmpMinRatio = tmpRatio;

                if (this.isDebug()) {
                    this.debug("Row: {}\t=>\tRatio: {},\tNumerator/RHS: {}, \tDenominator/Pivot: {}.", i, tmpRatio, tmpNumer, tmpDenom);
                }
            }
        }

        return retVal;
    }

    void performIteration(final int pivotRow, final int pivotCol) {

        final double tmpPivotElement = myTableau.doubleValue(pivotRow, pivotCol);
        final double tmpPivotRHS = myTableau.doubleValue(pivotRow, myPoint.getColRHS());

        final IterationPoint point = new IterationPoint();
        point.row = pivotRow;
        point.col = pivotCol;

        myTableau.pivot(point);

        if (this.isDebug()) {
            this.debug("Iteration Point <{},{}>\tPivot: {} => {}\tRHS: {} => {}.", pivotRow, pivotCol, tmpPivotElement,
                    myTableau.doubleValue(pivotRow, pivotCol), tmpPivotRHS, myTableau.doubleValue(pivotRow, myPoint.getColRHS()));
        }

        final int tmpOld = myBasis[pivotRow];
        if (tmpOld >= 0) {
            this.exclude(tmpOld);
        }
        final int tmpNew = pivotCol;
        if (tmpNew >= 0) {
            this.include(tmpNew);
        }
        myBasis[pivotRow] = pivotCol;

        if (options.validate) {

            // Right-most column of the tableau
            final Array1D<Double> tmpRHS = myTableau.sliceConstraintsRHS();

            final AggregatorFunction<Double> tmpMinAggr = PrimitiveAggregator.getSet().minimum();
            tmpRHS.visitAll(tmpMinAggr);
            final double tmpMinVal = tmpMinAggr.doubleValue();

            if ((tmpMinVal < ZERO) && !options.problem.isZero(tmpMinVal)) {
                this.debug("\nNegative RHS! {}", tmpMinVal);
                if (this.isDebug()) {
                    this.debug("Entire RHS columns: {}\n", tmpRHS);
                }
            }

        }
    }

}