package com.github.egor18.jdataflow.checkers;

import com.github.egor18.jdataflow.scanners.CheckersScanner;
import com.github.egor18.jdataflow.summaries.FunctionSummary;
import com.github.egor18.jdataflow.warning.Warning;
import com.github.egor18.jdataflow.warning.WarningKind;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import static com.github.egor18.jdataflow.utils.TypeUtils.isVoid;

/**
 * This checker warns if return value of a pure function is not used.
 * For example:
 *    Math.max(x, y); // <= result is ignored warning
 */
public class ResultIgnoredChecker extends AbstractChecker
{
    public ResultIgnoredChecker(CheckersScanner scanner)
    {
        super(scanner);
    }

    @Override
    public void checkInvocation(CtInvocation<?> invocation)
    {
        if (isVoid(invocation.getType()))
        {
            return;
        }

        CtElement invocationParent = invocation.getParent();
        if (!(invocationParent instanceof CtBlock))
        {
            return;
        }

        FunctionSummary functionSummary = getScanner().getFunctionSummary(invocation);
        if (functionSummary == null)
        {
            return;
        }

        if (functionSummary.isPure())
        {
            addWarning(new Warning(invocation, WarningKind.RESULT_IGNORED));
        }
    }
}
