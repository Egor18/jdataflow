package com.github.egor18.jdataflow.checkers;

import com.github.egor18.jdataflow.misc.ConditionStatus;
import com.github.egor18.jdataflow.scanners.CheckersScanner;
import com.github.egor18.jdataflow.warning.Warning;
import com.github.egor18.jdataflow.warning.WarningKind;
import com.microsoft.z3.*;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.reference.CtTypeReference;

import static com.github.egor18.jdataflow.utils.PromotionUtils.promoteNumericValue;
import static com.github.egor18.jdataflow.utils.TypeUtils.getActualType;

/**
 * This checker warns if array index is out of bounds.
 * For example:
 *    int[] arr = {1, 2, 3};
 *    int z = arr[3]; // <= array index is out of bounds warning
 */
public class ArrayIndexChecker extends AbstractChecker
{
    public ArrayIndexChecker(CheckersScanner scanner)
    {
        super(scanner);
    }

    private void check(CtArrayAccess<?, ?> arrayAccess)
    {
        if (arrayAccess.getTarget() == null)
        {
            return;
        }

        CtTypeReference<?> indexType = getActualType(arrayAccess.getIndexExpression());

        Expr indexExpr = (Expr) arrayAccess.getIndexExpression().getMetadata("value");
        if (indexExpr == null)
        {
            return;
        }

        // Unboxing conversion
        if (!indexType.isPrimitive())
        {
            indexExpr = getMemory().read(indexType.unbox(), (IntExpr) indexExpr);
        }

        indexExpr = promoteNumericValue(getContext(), indexExpr, indexType);

        IntExpr targetExpr = (IntExpr) arrayAccess.getTarget().getMetadata("value");
        if (targetExpr == null)
        {
            return;
        }

        Expr arrayLengthExpr = getMemory().read(getScanner().getArrayLengthFieldReference(), targetExpr);

        BoolExpr zeroCond = getContext().mkBVSLT((BitVecExpr) indexExpr, getContext().mkBV(0, 32));
        BoolExpr sizeCond = getContext().mkBVSGE((BitVecExpr) indexExpr, (BitVecExpr) arrayLengthExpr);

        ConditionStatus isOutOfBounds = checkCond(getContext().mkOr(zeroCond, sizeCond));
        if (isOutOfBounds == ConditionStatus.ALWAYS_TRUE)
        {
            addWarning(new Warning(arrayAccess.getIndexExpression(), WarningKind.ARRAY_INDEX_IS_OUT_OF_BOUNDS));
        }
    }

    @Override
    public void checkArrayRead(CtArrayRead<?> arrayRead)
    {
        check(arrayRead);
    }

    @Override
    public void checkArrayWrite(CtArrayWrite<?> arrayWrite)
    {
        check(arrayWrite);
    }
}
