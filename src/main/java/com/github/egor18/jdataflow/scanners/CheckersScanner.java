package com.github.egor18.jdataflow.scanners;

import com.github.egor18.jdataflow.Configuration;
import com.github.egor18.jdataflow.checkers.*;
import com.github.egor18.jdataflow.exceptions.JDataFlowException;
import com.github.egor18.jdataflow.exceptions.JDataFlowTimeoutException;
import com.github.egor18.jdataflow.warning.Warning;
import com.github.egor18.jdataflow.warning.WarningKind;
import spoon.reflect.code.*;
import spoon.reflect.factory.Factory;

import java.util.ArrayList;
import java.util.List;

public class CheckersScanner extends DataFlowScanner
{
    // A list of all available checkers
    private List<AbstractChecker> checkers = new ArrayList<>();

    // A list of warnings
    private List<Warning> warnings = new ArrayList<>();

    public List<Warning> getWarnings()
    {
        return warnings;
    }

    public void addWarning(Warning warning)
    {
        warnings.add(warning);
    }

    public CheckersScanner(Factory factory, Configuration config)
    {
        super(factory, config);
        checkers.add(new AlwaysTrueFalseChecker(this));
        checkers.add(new NullDereferenceChecker(this));
        checkers.add(new ArrayIndexChecker(this));
        checkers.add(new ResultIgnoredChecker(this));
    }

    @Override
    public void handleException(JDataFlowException e)
    {
        WarningKind kind = e instanceof JDataFlowTimeoutException ? WarningKind.TIMEOUT : WarningKind.EXCEPTION;
        warnings.add(new Warning(e.getPosition(), kind, e.getMessage()));
    }

    @Override
    public void checkCondition(CtExpression<?> condition)
    {
        checkers.forEach(c -> c.checkCondition(condition));
    }

    @Override
    public void checkBinaryOperatorLeft(BinaryOperatorKind kind, CtExpression<?> left)
    {
        checkers.forEach(c -> c.checkBinaryOperatorLeft(kind, left));
    }

    @Override
    public void checkBinaryOperatorRight(BinaryOperatorKind kind, CtExpression<?> right)
    {
        checkers.forEach(c -> c.checkBinaryOperatorRight(kind, right));
    }

    @Override
    public void checkConditionalThenExpression(CtExpression<?> thenExpression)
    {
        checkers.forEach(c -> c.checkConditionalThenExpression(thenExpression));
    }

    @Override
    public void checkConditionalElseExpression(CtExpression<?> elseExpression)
    {
        checkers.forEach(c -> c.checkConditionalElseExpression(elseExpression));
    }

    @Override
    public void checkReturnedExpression(CtExpression<?> returnedExpression)
    {
        checkers.forEach(c -> c.checkReturnedExpression(returnedExpression));
    }

    @Override
    public void checkAssignmentLeft(CtExpression<?> left)
    {
        checkers.forEach(c -> c.checkAssignmentLeft(left));
    }

    @Override
    public void checkAssignmentRight(CtExpression<?> right)
    {
        checkers.forEach(c -> c.checkAssignmentRight(right));
    }

    @Override
    public void checkAssignmentResult(CtAssignment<?, ?> assignment)
    {
        checkers.forEach(c -> c.checkAssignmentResult(assignment));
    }

    @Override
    public void checkInvocation(CtInvocation<?> invocation)
    {
        checkers.forEach(c -> c.checkInvocation(invocation));
    }

    @Override
    public void checkConditionalResult(CtConditional<?> conditional)
    {
        checkers.forEach(c -> c.checkConditionalResult(conditional));
    }

    @Override
    public void checkBinaryOperatorResult(CtBinaryOperator<?> operator)
    {
        checkers.forEach(c -> c.checkBinaryOperatorResult(operator));
    }

    @Override
    public void checkFieldRead(CtFieldRead<?> fieldRead)
    {
        checkers.forEach(c -> c.checkFieldRead(fieldRead));
    }

    @Override
    public void checkFieldWrite(CtFieldWrite<?> fieldWrite)
    {
        checkers.forEach(c -> c.checkFieldWrite(fieldWrite));
    }

    @Override
    public void checkArrayRead(CtArrayRead<?> arrayRead)
    {
        checkers.forEach(c -> c.checkArrayRead(arrayRead));
    }

    @Override
    public void checkArrayWrite(CtArrayWrite<?> arrayWrite)
    {
        checkers.forEach(c -> c.checkArrayWrite(arrayWrite));
    }
}
