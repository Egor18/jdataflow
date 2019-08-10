package com.github.egor18.jdataflow.scanners;

import com.github.egor18.jdataflow.memory.Memory;
import com.github.egor18.jdataflow.misc.BranchData;
import com.github.egor18.jdataflow.misc.ConditionStatus;
import com.github.egor18.jdataflow.misc.FlagReference;
import com.github.egor18.jdataflow.utils.TypeUtils;
import com.microsoft.z3.*;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.filter.AbstractFilter;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;

import static com.github.egor18.jdataflow.utils.CommonUtils.getTargetValue;
import static com.github.egor18.jdataflow.utils.PromotionUtils.promoteNumericValue;
import static com.github.egor18.jdataflow.utils.PromotionUtils.promoteNumericValues;
import static com.github.egor18.jdataflow.utils.TypeUtils.*;
import static com.github.egor18.jdataflow.utils.TypeUtils.getActualType;
import static com.github.egor18.jdataflow.utils.TypeUtils.isCalculable;
import static com.github.egor18.jdataflow.utils.TypeUtils.isChar;
import static com.github.egor18.jdataflow.utils.TypeUtils.isImmutable;
import static com.github.egor18.jdataflow.utils.TypeUtils.makeFreshBool;
import static com.github.egor18.jdataflow.utils.TypeUtils.makeFreshConstFromType;

/**
 * The main Data-flow scanner, which calculates the values of expressions.
 */
public abstract class DataFlowScanner extends AbstractCheckingScanner
{
    // Spoon factory
    private final Factory factory;

    // Maps variable reference to the latest corresponding value
    private Map<CtReference, Expr> variablesMap = new HashMap<>();

    // Stack of indirect modifications (i.e. within inner classes and lambdas) of current methods
    private Stack<List<CtElement>> currentMethodsIndirectModifications = new Stack<>();

    // z3 solver context
    private Context context;

    // z3 solver
    private Solver solver;

    // Memory model
    private Memory memory;

    // Abrupt termination flags references
    private CtReference returnFlagReference;
    private CtReference breakFlagReference;
    private CtReference continueFlagReference;
    private CtReference throwFlagReference;

    // Active conditions at the moment
    private BoolExpr currentConditions;

    // Current calculated result
    private Expr currentResult;

    public DataFlowScanner(Factory factory)
    {
        this.factory = factory;
        this.context = new Context();
        this.solver = context.mkSolver();
        this.memory = new Memory(context);
    }

    private BoolExpr getReturnExpr()
    {
        return (BoolExpr) variablesMap.get(returnFlagReference);
    }

    private BoolExpr getBreakExpr()
    {
        return (BoolExpr) variablesMap.get(breakFlagReference);
    }

    private BoolExpr getContinueExpr()
    {
        return (BoolExpr) variablesMap.get(continueFlagReference);
    }

    private BoolExpr getThrowExpr()
    {
        return (BoolExpr) variablesMap.get(throwFlagReference);
    }

    public Context getContext()
    {
        return context;
    }

    public Solver getSolver()
    {
        return solver;
    }

    public Memory getMemory()
    {
        return memory;
    }

    /**
     * Creates z3 expression from the known value.
     */
    private Expr makeLiteral(Object value)
    {
        if (value instanceof Boolean)
        {
            return context.mkBool((Boolean) value);
        }
        else if (value instanceof Byte)
        {
            return context.mkBV((Byte) value, 8);
        }
        else if (value instanceof Short)
        {
            return context.mkBV((Short) value, 16);
        }
        else if (value instanceof Integer)
        {
            return context.mkBV((Integer) value, 32);
        }
        else if (value instanceof Long)
        {
            return context.mkBV((Long) value, 64);
        }
        else if (value instanceof Character)
        {
            return context.mkBV((Character) value, 16);
        }

        return null;
    }

    /**
     * Applies cast to the bit-vector expression, returns resulting bit-vector expression.
     */
    private BitVecExpr castBV(BitVecExpr bitVec, CtTypeReference<?> fromType, CtTypeReference<?> toType)
    {
        if (!isBitVector(fromType) || !isBitVector(toType))
        {
            throw new RuntimeException("Invalid castBV arguments");
        }

        int newSize = TypeUtils.getPrimitiveTypeSize(toType);
        int sizeDifference = newSize - bitVec.getSortSize();

        if (sizeDifference > 0)
        {
            // Widening Primitive Conversion
            if (!isChar(fromType))
            {
                return context.mkSignExt(sizeDifference, bitVec);
            }
            else
            {
                return context.mkZeroExt(sizeDifference, bitVec);
            }
        }
        else if (sizeDifference < 0)
        {
            // Narrowing Primitive Conversion (signed)
            return context.mkExtract(newSize - 1, 0, bitVec);
        }

        return bitVec;
    }

    /**
     * Applies casts to the expression, returns resulting expression.
     */
    private Expr applyCasts(Expr expr, CtTypeReference<?> originalType, List<CtTypeReference<?>> casts)
    {
        if (expr == null)
        {
            Sort sort = TypeUtils.getTypeSort(context, originalType);
            expr = context.mkFreshConst("", sort);
        }

        for (int i = casts.size() - 1; i >= 0; i--)
        {
            CtTypeReference<?> castType = casts.get(i);

            if (expr instanceof IntExpr)
            {
                if (castType.isPrimitive())
                {
                    if (isCalculable(originalType))
                    {
                        // Unboxing conversion
                        expr = memory.read(originalType.unbox(), (IntExpr) expr);
                    }
                    else
                    {
                        // Casting Object to primitive => create value from type
                        expr = makeFreshConstFromType(context, castType);
                    }
                }
            }

            if (expr instanceof BitVecExpr)
            {
                if (isBitVector(originalType) && isBitVector(castType))
                {
                    expr = castBV((BitVecExpr) expr, originalType, castType);
                }
                else
                {
                    Sort sort = TypeUtils.getTypeSort(context, castType);
                    expr = context.mkFreshConst("", sort);
                }
            }
            else if (expr instanceof RealExpr)
            {
                // We don't actually calculate floats right now => create value from type
                expr = makeFreshConstFromType(context, castType);
            }

            // Boxing conversion
            if (!castType.isPrimitive() && originalType.isPrimitive())
            {
                expr = box(expr, castType);
            }

            originalType = castType;
        }

        return expr;
    }

    /**
     * Applies boxing operation to the expression, returns resulting expression.
     */
    private Expr box(Expr expr, CtTypeReference<?> type)
    {
        int nextPointer = memory.nextPointer();
        IntExpr index =  context.mkInt(nextPointer);
        if (isCalculable(type) && expr != null)
        {
            memory.write(type.unbox(), index, expr);
        }
        return index;
    }

    /**
     * This method is equivalent to solver.check(assumption).
     * We should use it because of this bug: https://github.com/Z3Prover/z3/issues/2107
     */
    private Status checkAssumption(BoolExpr assumption)
    {
        solver.push();
        solver.add(assumption);
        Status status = solver.check();
        solver.pop();
        return status;
    }

    /**
     * Adds current conditions and abrupt termination flags to the solver.
     */
    private void applyState()
    {
        // Apply current conditions
        if (currentConditions != null)
        {
            solver.add(currentConditions);
        }

        // Apply information about exit points
        BoolExpr returnExpr = getReturnExpr();
        if (returnExpr != null)
        {
            BoolExpr reachableExpr = context.mkNot(returnExpr);
            solver.add(reachableExpr);
        }

        // Apply information about breaks
        BoolExpr breakExpr = getBreakExpr();
        if (breakExpr != null)
        {
            BoolExpr reachableExpr = context.mkNot(breakExpr);
            solver.add(reachableExpr);
        }

        // Apply information about continues
        BoolExpr continueExpr = getContinueExpr();
        if (continueExpr != null)
        {
            BoolExpr reachableExpr = context.mkNot(continueExpr);
            solver.add(reachableExpr);
        }

        // Apply information about throws
        BoolExpr throwExpr = getThrowExpr();
        if (throwExpr != null)
        {
            BoolExpr reachableExpr = context.mkNot(throwExpr);
            solver.add(reachableExpr);
        }
    }

    /**
     * Checks if the scanner is inside some unreachable condition.
     * For example: if (false) {...}
     */
    private boolean isInsideUnreachableCondition()
    {
        if (currentConditions == null)
        {
            return false;
        }
        solver.push();
        applyState();
        Status status = solver.check();
        solver.pop();
        return currentConditions != null && status == Status.UNSATISFIABLE;
    }

    /**
     * Checks if some arbitrary condition is always true/false at the moment.
     */
    public ConditionStatus checkCond(BoolExpr conditionExpr)
    {
        ConditionStatus result = ConditionStatus.OK;
        if (isInsideUnreachableCondition())
        {
            return result;
        }

        solver.push();

        applyState();

        Status status1 = checkAssumption(conditionExpr);
        if (status1 == Status.SATISFIABLE)
        {
            // To check if formula is valid (i.e., to prove it), we show its negation to be unsatisfiable.
            Status status2 = checkAssumption(context.mkNot(conditionExpr));
            if (status2 == Status.UNSATISFIABLE)
            {
                result = ConditionStatus.ALWAYS_TRUE;
            }
        }
        else if (status1 == Status.UNSATISFIABLE)
        {
            result = ConditionStatus.ALWAYS_FALSE;
        }

        solver.pop();
        return result;
    }

    @Override
    public void visitCtIf(CtIf ifElement)
    {
        BoolExpr conditionExpr = visitCondition(ifElement.getCondition(), false);

        final boolean hasElseBranch = ifElement.getElseStatement() != null;

        BranchData thenBranchData = visitBranch(conditionExpr, ifElement.getThenStatement());
        BranchData elseBranchData = hasElseBranch ? visitBranch(context.mkNot(conditionExpr), ifElement.getElseStatement()) : new BranchData(variablesMap, memory);

        mergeBranches(conditionExpr, thenBranchData, elseBranchData);
    }

    private void visitLoop(CtExpression<Boolean> loopCondition, boolean isPrecondition, CtStatement... loopBody)
    {
        BoolExpr oldBreakExpr = getBreakExpr();
        BoolExpr oldContinueExpr = getContinueExpr();

        BoolExpr iterationConditionExpr;
        BranchData iterationBranchData;
        if (isPrecondition)
        {
            if (loopCondition != null)
            {
                // Check if loop condition is always false
                visitCondition(loopCondition, true);
            }
            ResetOnModificationScanner resetScanner = new ResetOnModificationScanner(context, variablesMap, memory);
            Arrays.stream(loopBody).forEach(resetScanner::scan);
            iterationConditionExpr = loopCondition == null ? makeFreshBool(context) : visitCondition(loopCondition, true);

            iterationBranchData = visitBranch(iterationConditionExpr, loopBody);
        }
        else
        {
            ResetOnModificationScanner resetScanner = new ResetOnModificationScanner(context, variablesMap, memory);
            Arrays.stream(loopBody).forEach(resetScanner::scan);
            iterationBranchData = visitBranch(context.mkTrue(), loopBody);
            iterationConditionExpr = loopCondition == null ? makeFreshBool(context) : visitCondition(loopCondition, true);
        }

        // Save information about the break
        variablesMap.put(breakFlagReference, iterationBranchData.getVariablesMap().get(breakFlagReference));

        BoolExpr currentBreakExpr = getBreakExpr();

        // Reset flow flags after exiting the loop
        variablesMap.put(breakFlagReference, oldBreakExpr);
        variablesMap.put(continueFlagReference, oldContinueExpr);

        // Invert loop condition
        solver.add(context.mkOr(context.mkNot(iterationConditionExpr), currentBreakExpr));
    }

    @Override
    public void visitCtWhile(CtWhile whileLoop)
    {
        CtExpression<Boolean> loopCondition = whileLoop.getLoopingExpression();
        CtStatement loopBody = whileLoop.getBody();
        visitLoop(loopCondition, true, loopBody);
    }

    @Override
    public void visitCtFor(CtFor forLoop)
    {
        scan(forLoop.getForInit());
        CtExpression<Boolean> loopCondition = forLoop.getExpression();
        List<CtStatement> forUpdate = forLoop.getForUpdate();
        CtStatement forBody = forLoop.getBody();
        List<CtStatement> bodyStatements = new ArrayList<>();
        bodyStatements.add(forBody);
        bodyStatements.addAll(forUpdate);
        visitLoop(loopCondition, true, bodyStatements.toArray(new CtStatement[0]));
    }

    @Override
    public void visitCtForEach(CtForEach foreach)
    {
        scan(foreach.getVariable());
        CtStatement loopBody = foreach.getBody();
        visitLoop(null, true, loopBody);
    }

    @Override
    public void visitCtDo(CtDo doLoop)
    {
        CtExpression<Boolean> loopCondition = doLoop.getLoopingExpression();
        CtStatement loopBody = doLoop.getBody();
        visitLoop(loopCondition, false, loopBody);
    }

    private BoolExpr visitCondition(CtExpression<Boolean> condition, boolean isLoopCondition)
    {
        scan(condition);
        Expr conditionValue = currentResult;
        condition.putMetadata("value", conditionValue);

        checkCondition(condition, isLoopCondition);

        // Unboxing conversion
        if (!getActualType(condition).isPrimitive())
        {
            conditionValue = memory.read(getActualType(condition).unbox(), (IntExpr) conditionValue);
        }

        currentResult = conditionValue;
        return (BoolExpr) currentResult;
    }

    /**
     * Merges thenMap and elseMap into resultMap via ITE function.
     */
    private <T extends Expr> void mergeMaps(BoolExpr cond, Map<CtReference, T> thenMap, Map<CtReference, T> elseMap, Map<CtReference, T> resultMap)
    {
        // xNew = ITE(cond, xThen, xElse) for each entry
        for (Map.Entry<CtReference, T> entry : thenMap.entrySet())
        {
            CtReference reference = entry.getKey();
            T thenBranchValue = entry.getValue();
            T elseBranchValue = elseMap.get(reference);

            // Variable was not changed in this if-then-else block
            if (thenBranchValue == elseBranchValue)
            {
                continue;
            }

            if (thenBranchValue != null && elseBranchValue != null)
            {
                T iteExpr = (T) context.mkITE(cond, thenBranchValue, elseBranchValue);
                resultMap.put(reference, iteExpr);
            }
        }
    }

    /**
     * Merges two branches into the current.
     */
    private void mergeBranches(BoolExpr cond, BranchData thenBranchData, BranchData elseBranchData)
    {
        mergeMaps(cond, thenBranchData.getVariablesMap(), elseBranchData.getVariablesMap(), variablesMap);
        mergeMaps(cond, thenBranchData.getMemory().getMemoryMap(), elseBranchData.getMemory().getMemoryMap(), memory.getMemoryMap());
    }

    private BranchData visitBranch(BoolExpr branchCond, CtElement... branchBody)
    {
        // Values before entering the branch
        Map<CtReference, Expr> oldValues = new HashMap<>(variablesMap);
        BoolExpr oldConditions = currentConditions;
        Memory oldMemory = new Memory(memory);

        currentConditions = currentConditions == null ? branchCond : context.mkAnd(currentConditions, branchCond);
        for (CtElement element : branchBody)
        {
            scan(element);
        }
        Map<CtReference, Expr> newValues = variablesMap;
        Memory newMemory = memory;

        currentConditions = oldConditions;
        variablesMap = oldValues;
        memory = oldMemory;

        return new BranchData(newValues, newMemory);
    }

    @Override
    public <T> void visitCtConditional(CtConditional<T> conditional)
    {
        BoolExpr conditionExpr = visitCondition(conditional.getCondition(), false);

        BranchData thenBranchData = visitBranch(conditionExpr, conditional.getThenExpression());
        Expr thenExpr = currentResult;

        BranchData elseBranchData = visitBranch(context.mkNot(conditionExpr), conditional.getElseExpression());
        Expr elseExpr = currentResult;

        CtTypeReference<?> thenType = getActualType(conditional.getThenExpression());
        CtTypeReference<?> elseType = getActualType(conditional.getElseExpression());

        // There are 3 kinds of conditional expressions:
        // If both the second and the third operand expressions are boolean expressions, the conditional expression is a boolean conditional expression.
        // If both the second and the third operand expressions are numeric expressions, the conditional expression is a numeric conditional expression.
        // Otherwise, the conditional expression is a reference conditional expression.

        if (isBoolean(thenType) && isBoolean(elseType))
        {
            if (thenType.isPrimitive() || elseType.isPrimitive())
            {
                // Unboxing conversion
                if (!thenType.isPrimitive())
                {
                    thenExpr = memory.read(thenType.unbox(), (IntExpr) thenExpr);
                }

                // Unboxing conversion
                if (!elseType.isPrimitive())
                {
                    elseExpr = memory.read(elseType.unbox(), (IntExpr) elseExpr);
                }
            }
        }
        else if (isNumeric(thenType) && isNumeric(elseType))
        {
            if (isCalculable(thenType) && isCalculable(elseType))
            {
                if (thenType.isPrimitive() || elseType.isPrimitive())
                {
                    // Unboxing conversion
                    if (!thenType.isPrimitive())
                    {
                        thenExpr = memory.read(thenType.unbox(), (IntExpr) thenExpr);
                    }

                    // Unboxing conversion
                    if (!elseType.isPrimitive())
                    {
                        elseExpr = memory.read(elseType.unbox(), (IntExpr) elseExpr);
                    }
                }

                if (thenExpr instanceof BitVecExpr && elseExpr instanceof BitVecExpr)
                {
                    Expr[] result = promoteNumericValues(context, thenExpr, thenType, elseExpr, elseType);
                    thenExpr = result[0];
                    elseExpr = result[1];
                }
            }
            else
            {
                // At least one of the arguments is floating point number => create values of real sort
                thenExpr = makeFreshConstFromType(context, factory.Type().doubleType());
                elseExpr = makeFreshConstFromType(context, factory.Type().doubleType());
            }
        }
        else
        {
            // Box if necessary
            if (thenType.isPrimitive())
            {
                thenExpr = box(thenExpr, thenType);
            }

            // Box if necessary
            if (elseType.isPrimitive())
            {
                elseExpr = box(elseExpr, elseType);
            }
        }

        solver.push();
        solver.add(conditionExpr);
        checkConditionalThenExpression(conditional.getThenExpression());
        solver.pop();

        solver.push();
        solver.add(context.mkNot(conditionExpr));
        checkConditionalElseExpression(conditional.getElseExpression());
        solver.pop();

        mergeBranches(conditionExpr, thenBranchData, elseBranchData);

        currentResult = context.mkITE(conditionExpr, thenExpr, elseExpr);
        conditional.putMetadata("value", currentResult);
        checkConditionalResult(conditional);
    }

    @Override
    public <S> void visitCtSwitch(CtSwitch<S> switchStatement)
    {
        Expr breakExpr = variablesMap.get(breakFlagReference);

        // The type of the selector must be char, byte, short, int, Character, Byte, Short, Integer, String, or an enum type.
        CtExpression<S> selector = switchStatement.getSelector();
        CtTypeReference<?> selectorType = getActualType(selector);
        scan(selector);
        Expr selectorValue = currentResult;

        // Unboxing conversion
        if (!getActualType(selector).isPrimitive())
        {
            selectorValue = memory.read(getActualType(selector).unbox(), (IntExpr) selectorValue);
        }

        BoolExpr commonConditionExpr = null;
        List<CtCase<? super S>> cases = switchStatement.getCases();
        for (CtCase<? super S> aCase : cases)
        {
            CtExpression<?> caseExpression = aCase.getCaseExpression();
            BoolExpr branchExpr;
            if (caseExpression == null)
            {
                // Default label
                branchExpr = (commonConditionExpr == null) ? context.mkTrue() : context.mkNot(commonConditionExpr);
            }
            else
            {
                // Case label
                CtTypeReference<?> caseType = getActualType(caseExpression);
                scan(caseExpression);
                Expr caseValue = currentResult;

                // Binary Numeric Promotion
                if (selectorValue instanceof BitVecExpr && caseValue instanceof BitVecExpr)
                {
                    Expr[] result = promoteNumericValues(context, selectorValue, selectorType, caseValue, caseType);
                    selectorValue = result[0];
                    caseValue = result[1];
                }

                BoolExpr conditionExpr = context.mkEq(selectorValue, caseValue);

                // Handle cases as conditions
                caseExpression.putMetadata("value", conditionExpr);
                checkCondition(caseExpression, false);

                // Connect cases with OR
                commonConditionExpr = (commonConditionExpr == null) ? conditionExpr : context.mkOr(commonConditionExpr, conditionExpr);

                branchExpr = context.mkAnd(commonConditionExpr, context.mkNot(getBreakExpr()));
            }

            BranchData thenBranchData = visitBranch(branchExpr, aCase);
            BranchData elseBranchData = new BranchData(variablesMap, memory);

            mergeBranches(branchExpr, thenBranchData, elseBranchData);
        }

        variablesMap.put(breakFlagReference, breakExpr);
    }

    private void visitTryBranch(CtBlock<?> block)
    {
        // Reset all variables that are changed in try-catch
        ResetOnModificationScanner resetScanner = new ResetOnModificationScanner(context, variablesMap, memory);
        resetScanner.scan(block);
        visitBranch(makeFreshBool(context), block);
    }

    @Override
    public void visitCtTry(CtTry tryBlock)
    {
        CtBlock<?> tryBody = tryBlock.getBody();
        List<CtCatch> catchers = tryBlock.getCatchers();
        CtBlock<?> finalizer = tryBlock.getFinalizer();

        BoolExpr oldThrowExpr = getThrowExpr();
        visitTryBranch(tryBody);
        variablesMap.put(throwFlagReference, oldThrowExpr);
        catchers.forEach(c -> visitTryBranch(c.getBody()));
        scan(finalizer);
    }

    @Override
    public void visitCtTryWithResource(CtTryWithResource tryWithResource)
    {
        tryWithResource.getResources().forEach(this::scan);
        visitCtTry(tryWithResource);
    }

    @Override
    public void visitCtBreak(CtBreak breakStatement)
    {
        variablesMap.put(breakFlagReference, context.mkTrue());
    }

    @Override
    public void visitCtContinue(CtContinue continueStatement)
    {
        variablesMap.put(continueFlagReference, context.mkTrue());
    }

    @Override
    public void visitCtThrow(CtThrow throwStatement)
    {
        scan(throwStatement.getThrownExpression());
        variablesMap.put(throwFlagReference, context.mkTrue());
    }

    @Override
    public <T> void visitCtConstructorCall(CtConstructorCall<T> constructorCall)
    {
        int nextPointer = memory.nextPointer();

        // Test code for Integer(x) constructor
        if (constructorCall.getType().getQualifiedName().equals("java.lang.Integer")
            && constructorCall.getArguments().size() == 1)
        {
            CtExpression<?> arg1 = constructorCall.getArguments().get(0);
            scan(arg1);
            Expr arg1Value = currentResult;

            // Cast argument type to the parameter type
            CtTypeReference<?> arg1Type = getActualType(arg1);
            CtTypeReference<?> arg1SignatureType = constructorCall.getExecutable().getParameters().get(0);
            arg1Value = applyCasts(arg1Value, arg1Type, Collections.singletonList(arg1SignatureType));

            memory.write(constructorCall.getType().unbox(), context.mkInt(nextPointer), arg1Value);
        }
        else
        {
            constructorCall.getArguments().forEach(this::scan);
        }

        IntExpr constructorCallValue = context.mkInt(nextPointer);
        currentResult = applyCasts(constructorCallValue, constructorCall.getType(), constructorCall.getTypeCasts());
        constructorCall.putMetadata("value", currentResult);
    }

    @Override
    public <T> void visitCtNewClass(CtNewClass<T> newClass)
    {
        super.visitCtNewClass(newClass);

        // Create new object
        int nextPointer = memory.nextPointer();
        IntExpr lambdaValue = context.mkInt(nextPointer);
        currentResult = applyCasts(lambdaValue, newClass.getType(), newClass.getTypeCasts());
        newClass.putMetadata("value", currentResult);
    }

    @Override
    public <T> void visitCtNewArray(CtNewArray<T> newArray)
    {
        int nextPointer = memory.nextPointer();
        IntExpr arrayValue = context.mkInt(nextPointer);

        for (CtExpression<Integer> dimensionExpression : newArray.getDimensionExpressions())
        {
            scan(dimensionExpression);
            // TODO: Set array.length equal to dimension size
            // It seems that spoon does not provide a way to get reference to the array.length property so far.
            // The code should be something like this:
            // memory.write(lengthFieldReference, arrayValue, dimensionLengthExpr);
        }

        CtTypeReference<?> componentType = ((CtArrayTypeReference) (newArray.getType())).getComponentType();

        int i = 0;
        for (CtExpression<?> arrayElement : newArray.getElements())
        {
            scan(arrayElement);
            Expr arrayElementExpr = currentResult;
            CtTypeReference<?> arrayElementType = getActualType(arrayElement);
            arrayElementExpr = applyCasts(arrayElementExpr, arrayElementType, Collections.singletonList(componentType));
            memory.writeArray((CtArrayTypeReference) newArray.getType(), arrayValue, context.mkBV(i, 32), arrayElementExpr);
            i++;
        }

        currentResult = applyCasts(arrayValue, newArray.getType(), newArray.getTypeCasts());
        newArray.putMetadata("value", currentResult);
    }

    /**
     * Makes targetExpr not null (if something is dereferenced => it is not null).
     */
    private void visitDereference(Expr targetExpr)
    {
        if (targetExpr != null)
        {
            BoolExpr notNullExpr = context.mkDistinct(targetExpr, context.mkInt(Memory.nullPointer()));
            currentConditions = currentConditions == null ? notNullExpr : context.mkAnd(currentConditions, notNullExpr);
        }
    }

    @Override
    public <T> void visitCtInvocation(CtInvocation<T> invocation)
    {
        // Reset arguments
        List<CtExpression<?>> arguments = invocation.getArguments();
        for (CtExpression<?> argument : arguments)
        {
            scan(argument);
            Expr argumentExpr = currentResult;
            if (argumentExpr != null)
            {
                CtTypeReference<?> argumentType = getActualType(argument);
                if (!argumentType.isPrimitive() && !isImmutable(argumentType))
                {
                    memory.resetObject(argumentType, (IntExpr) argumentExpr);
                }
            }
        }

        // Reset target
        CtExpression<?> target = invocation.getTarget();
        Expr targetExpr = null;
        boolean dereferenceTarget = false;
        if (target != null)
        {
            scan(target);
            targetExpr = currentResult;
            if (targetExpr != null)
            {
                CtTypeReference<?> targetType = getActualType(target);
                if (!(target instanceof CtTypeAccess))
                {
                    dereferenceTarget = true;
                    if (!isImmutable(targetType))
                    {
                        memory.resetObject(targetType, (IntExpr) targetExpr);
                    }
                }
            }
        }

        // Reset this
        IntNum thisExpr = context.mkInt(Memory.thisPointer());
        CtTypeReference thisType = invocation.getParent(CtClass.class).getReference();
        memory.resetObject(thisType, thisExpr);

        // Reset super
        CtTypeReference superType = thisType.getSuperclass();
        if (superType != null)
        {
            memory.resetObject(superType, thisExpr);
        }

        // Reset all variables that could be possibly modified indirectly
        if (!currentMethodsIndirectModifications.isEmpty())
        {
            ResetOnModificationScanner resetScanner = new ResetOnModificationScanner(context, variablesMap, memory);
            for (CtElement indirectModification : currentMethodsIndirectModifications.peek())
            {
                // If the indirect modification occurs after the invocation, it could not yet affect variables
                SourcePosition modificationPosition = indirectModification.getPosition();
                SourcePosition invocationPosition = invocation.getPosition();
                if (modificationPosition == null || invocationPosition == null
                    || modificationPosition instanceof NoSourcePosition || invocationPosition instanceof NoSourcePosition
                    || !modificationPosition.isValidPosition() || !invocationPosition.isValidPosition()
                    || modificationPosition.getSourceStart() <= invocation.getPosition().getSourceEnd())
                {
                    resetScanner.scan(indirectModification);
                }
            }
        }

        CtTypeReference<?> returnType = invocation.getType();
        if (!isVoid(returnType))
        {
            // In the most general case the return value of a function is unknown
            Expr returnValue = makeFreshConstFromType(context, returnType);
            currentResult = applyCasts(returnValue, returnType, invocation.getTypeCasts());
            invocation.putMetadata("value", currentResult);
        }

        checkInvocation(invocation);

        if (dereferenceTarget)
        {
            visitDereference(targetExpr);
        }
    }

    @Override
    public <R> void visitCtReturn(CtReturn<R> returnStatement)
    {
        CtExpression<R> returnedExpression = returnStatement.getReturnedExpression();
        if (returnedExpression != null)
        {
            scan(returnedExpression);
            returnedExpression.putMetadata("value", currentResult);
            checkReturnedExpression(returnedExpression);
        }

        variablesMap.put(returnFlagReference, context.mkTrue());
    }

    @Override
    public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable)
    {
        CtExpression<T> defaultExpression = localVariable.getDefaultExpression();
        if (defaultExpression != null)
        {
            CtAssignment<T, T> assignment = factory.createAssignment();
            assignment.setAssignment(defaultExpression);
            CtVariableWrite<T> variableWrite = factory.createVariableWrite();
            variableWrite.setVariable(localVariable.getReference());
            variableWrite.setType(localVariable.getType());
            assignment.setAssigned(variableWrite);
            assignment.setType(localVariable.getType());
            assignment.setParent(localVariable.getParent());
            visitCtAssignment(assignment);
        }
        else
        {
            variablesMap.put(localVariable.getReference(), makeFreshConstFromType(context, localVariable.getType()));
        }
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass)
    {
        // Before visiting a class, we should visit all of its fields
        System.out.println("Analyzing class: " + ctClass.getQualifiedName());
        List<CtField<?>> fields = ctClass.getFields();
        for (CtField<?> field : fields)
        {
            if (field.isFinal())
            {
                CtExpression<?> defaultExpression = field.getDefaultExpression();
                if (defaultExpression != null)
                {
                    scan(defaultExpression);
                    Expr defaultExpr = currentResult;
                    defaultExpr = applyCasts(defaultExpr, getActualType(defaultExpression), Collections.singletonList(field.getType()));

                    Expr index;
                    if (field.isStatic())
                    {
                        CtTypeReference<?> declaringType = field.getDeclaringType().getReference();
                        index = variablesMap.get(declaringType);
                        if (index == null)
                        {
                            index = context.mkFreshConst("", context.getIntSort());
                            variablesMap.put(declaringType, index);
                        }
                    }
                    else
                    {
                        index = context.mkInt(Memory.thisPointer());
                    }

                    memory.write(field.getReference(), (IntExpr) index, defaultExpr);
                }
            }
        }

        solver.push();
        Map<CtReference, Expr> oldValues = new HashMap<>(variablesMap);
        Memory oldMemory = new Memory(memory);
        memory.resetMutable();

        super.visitCtClass(ctClass);

        variablesMap = oldValues;
        memory = oldMemory;
        solver.pop();
    }

    @Override
    public void visitCtAnonymousExecutable(CtAnonymousExecutable anonymousExec)
    {
        visitMethod(anonymousExec.getBody(), anonymousExec.getParameters());
    }

    private void visitMethod(CtElement body, List<CtParameter<?>> parameters)
    {
        solver.push();
        Map<CtReference, Expr> oldValues = new HashMap<>(variablesMap);
        Memory oldMemory = new Memory(memory);
        BoolExpr oldConditions = currentConditions;
        CtReference oldReturnFlagReference = returnFlagReference;
        CtReference oldBreakFlagReference = breakFlagReference;
        CtReference oldContinueFlagReference = continueFlagReference;
        CtReference oldThrowFlagReference = throwFlagReference;

        currentResult = null;
        currentConditions = null;

        returnFlagReference = FlagReference.makeFreshReturnReference();
        variablesMap.put(returnFlagReference, context.mkFalse());

        breakFlagReference = FlagReference.makeFreshBreakReference();
        variablesMap.put(breakFlagReference, context.mkFalse());

        continueFlagReference = FlagReference.makeFreshContinueReference();
        variablesMap.put(continueFlagReference, context.mkFalse());

        throwFlagReference = FlagReference.makeFreshThrowReference();
        variablesMap.put(throwFlagReference, context.mkFalse());

        for (CtParameter<?> parameter : parameters)
        {
            variablesMap.put(parameter.getReference(), makeFreshConstFromType(context, parameter.getType()));
        }

        scan(body);

        variablesMap = oldValues;
        memory = oldMemory;
        currentConditions = oldConditions;
        returnFlagReference = oldReturnFlagReference;
        breakFlagReference = oldBreakFlagReference;
        continueFlagReference = oldContinueFlagReference;
        throwFlagReference = oldThrowFlagReference;
        solver.pop();
    }

    @Override
    public <T> void visitCtConstructor(CtConstructor<T> constructor)
    {
        visitMethod(constructor.getBody(), constructor.getParameters());
    }

    /**
     * Collects all indirect modifications (i.e. within inner classes and lambdas) in the method.
     */
    private <T> List<CtElement> collectIndirectModifications(CtMethod<T> method)
    {
        List<CtClass> localClasses = method.getElements(new TypeFilter<>(CtClass.class));
        List<CtLambda> lambdas = method.getElements(new TypeFilter<>(CtLambda.class));
        AbstractFilter<CtElement> modificationsFilter = new AbstractFilter<CtElement>()
        {
            @Override
            public boolean matches(CtElement element)
            {
                return element instanceof CtFieldWrite
                       || element instanceof CtArrayWrite
                       || element instanceof CtInvocation
                       || element instanceof CtConstructorCall;
            }
        };

        List<CtElement> indirectModifications = new ArrayList<>();
        localClasses.forEach(c -> indirectModifications.addAll(c.filterChildren(modificationsFilter).list()));
        lambdas.forEach(l -> indirectModifications.addAll(l.filterChildren(modificationsFilter).list()));

        return indirectModifications;
    }

    @Override
    public <T> void visitCtMethod(CtMethod<T> method)
    {
        System.out.println("Analyzing method: " + method.getSimpleName());
        currentMethodsIndirectModifications.push(collectIndirectModifications(method));
        visitMethod(method.getBody(), method.getParameters());
        currentMethodsIndirectModifications.pop();
    }

    @Override
    public <T> void visitCtLambda(CtLambda<T> lambda)
    {
        CtElement lambdaBody = lambda.getBody() != null ? lambda.getBody() : lambda.getExpression();

        solver.push();
        Map<CtReference, Expr> oldValues = new HashMap<>(variablesMap);
        Memory oldMemory = new Memory(memory);
        memory.resetMutable();

        visitMethod(lambdaBody, lambda.getParameters());

        variablesMap = oldValues;
        memory = oldMemory;
        solver.pop();

        // Create new object for lambda
        int nextPointer = memory.nextPointer();
        IntExpr lambdaValue = context.mkInt(nextPointer);
        currentResult = applyCasts(lambdaValue, lambda.getType(), lambda.getTypeCasts());
        lambda.putMetadata("value", currentResult);
    }

    private void visitAssignment(CtExpression<?> left, Expr leftValue, CtTypeReference<?> leftType,
                                 Expr rightValue, CtTypeReference<?> rightType)
    {
        CtReference leftReference;
        if (left instanceof CtArrayWrite)
        {
            leftReference = ((CtArrayWrite<?>) left).getTarget().getType();
        }
        else
        {
            leftReference = ((CtVariableWrite<?>) left).getVariable();
        }

        rightValue = applyCasts(rightValue, rightType, Collections.singletonList(leftType));

        if (left instanceof CtFieldWrite)
        {
            // Update memory
            memory.write(leftReference, (IntExpr) leftValue, rightValue);
        }

        if (left instanceof CtArrayWrite)
        {
            // Update memory
            CtExpression<Integer> index = ((CtArrayWrite<?>) left).getIndexExpression();
            CtTypeReference<?> indexType = getActualType(index);
            Expr indexExpr = (Expr) index.getMetadata("value");

            // Unboxing conversion
            if (!indexType.isPrimitive())
            {
                indexExpr = memory.read(indexType.unbox(), (IntExpr) indexExpr);
            }

            indexExpr = promoteNumericValue(context, indexExpr, indexType);
            memory.writeArray((CtArrayTypeReference) leftReference, (IntExpr) leftValue, indexExpr, rightValue);
        }

        variablesMap.put(leftReference, rightValue);
        currentResult = rightValue; // Assignment returns its value
    }

    @Override
    public <T, A extends T> void visitCtAssignment(CtAssignment<T, A> assignment)
    {
        CtExpression<T> left = assignment.getAssigned();
        CtExpression<A> right = assignment.getAssignment();

        CtTypeReference<?> leftType = getActualType(left);
        CtTypeReference<?> rightType = getActualType(right);

        scan(left);
        Expr leftValue = currentResult;
        checkAssignmentLeft(left);
        scan(right);
        Expr rightValue = currentResult;
        checkAssignmentRight(right);

        visitAssignment(left, leftValue, leftType, rightValue, rightType);
        currentResult = applyCasts(currentResult, assignment.getType(), assignment.getTypeCasts());
        assignment.putMetadata("value", currentResult);
        checkAssignmentResult(assignment);
    }

    @Override
    public <T, A extends T> void visitCtOperatorAssignment(CtOperatorAssignment<T, A> assignment)
    {
        // A compound assignment expression of the form E1 op= E2 is equivalent to E1 = (T) ((E1) op (E2)),
        // where T is the type of E1, except that E1 is evaluated only once.

        CtExpression<T> left = assignment.getAssigned();
        CtExpression<A> right = assignment.getAssignment();

        CtTypeReference<?> leftType = getActualType(left);
        CtTypeReference<?> rightType = getActualType(right);

        scan(left);
        Expr leftValue = currentResult;
        scan(right);
        Expr rightValue = currentResult;

        Expr leftData = leftValue;
        if (left instanceof CtFieldWrite)
        {
             leftData = memory.read(((CtFieldWrite<T>) left).getVariable(), (IntExpr) leftValue);
        }

        if (left instanceof CtArrayWrite)
        {
            CtArrayWrite arrayWrite = (CtArrayWrite) left;
            CtExpression index = arrayWrite.getIndexExpression();
            Expr arrayIndex = (Expr) index.getMetadata("value");
            leftData = memory.readArray((CtArrayTypeReference) arrayWrite.getTarget().getType(), (IntExpr) leftValue, arrayIndex);
        }

        rightValue = calcBinaryOperator(leftData, leftType, rightValue, rightType, assignment.getKind());
        rightType = assignment.getAssigned().getType().unbox(); // Binary operator unboxes its operands

        visitAssignment(left, leftValue, leftType, rightValue, rightType);
        currentResult = applyCasts(currentResult, assignment.getType(), assignment.getTypeCasts());
        assignment.putMetadata("value", currentResult);
    }

    @Override
    public <T> void visitCtLiteral(CtLiteral<T> literal)
    {
        Expr valueExpr;
        if (TypeUtils.isNullType(literal.getType()))
        {
            valueExpr = context.mkInt(Memory.nullPointer());
        }
        else if (TypeUtils.isString(literal.getType()))
        {
            valueExpr = context.mkFreshConst("", context.getIntSort());
            solver.add(context.mkDistinct(valueExpr, context.mkInt(Memory.nullPointer())));
        }
        else
        {
            Object value = ((CtLiteral<?>) literal).getValue();
            valueExpr = applyCasts(makeLiteral(value), literal.getType(), literal.getTypeCasts());
        }

        currentResult = valueExpr;
        literal.putMetadata("value", currentResult);
    }

    @Override
    public <T> void visitCtVariableWrite(CtVariableWrite<T> variableWrite)
    {
        Expr variableValue = variablesMap.get(variableWrite.getVariable());
        if (variableValue != null)
        {
            currentResult = applyCasts(variableValue, variableWrite.getType(), variableWrite.getTypeCasts());
            variableWrite.putMetadata("value", currentResult);
        }
    }

    @Override
    public <T> void visitCtVariableRead(CtVariableRead<T> variableRead)
    {
        Expr variableValue = variablesMap.get(variableRead.getVariable());
        currentResult = applyCasts(variableValue, variableRead.getType(), variableRead.getTypeCasts());
        variableRead.putMetadata("value", currentResult);
    }

    @Override
    public <T> void visitCtThisAccess(CtThisAccess<T> thisAccess)
    {
        currentResult = context.mkInt(Memory.thisPointer());
        thisAccess.putMetadata("value", currentResult);
    }

    @Override
    public <T> void visitCtSuperAccess(CtSuperAccess<T> superAccess)
    {
        currentResult = context.mkInt(Memory.thisPointer());
        superAccess.putMetadata("value", currentResult);
    }

    @Override
    public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead)
    {
        scan(fieldRead.getTarget());
        IntExpr targetExpr = getTargetValue(context, variablesMap, memory, fieldRead.getTarget());
        currentResult = memory.read(fieldRead.getVariable(), targetExpr);
        currentResult = applyCasts(currentResult, fieldRead.getType(), fieldRead.getTypeCasts());
        fieldRead.putMetadata("value", currentResult);
        checkFieldRead(fieldRead);
        visitDereference(targetExpr);
    }

    @Override
    public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite)
    {
        scan(fieldWrite.getTarget());
        currentResult = getTargetValue(context, variablesMap, memory, fieldWrite.getTarget());
        fieldWrite.putMetadata("value", currentResult);
        checkFieldWrite(fieldWrite);
        visitDereference(currentResult);
    }

    @Override
    public <T> void visitCtArrayRead(CtArrayRead<T> arrayRead)
    {
        scan(arrayRead.getTarget());
        IntExpr targetExpr = getTargetValue(context, variablesMap, memory, arrayRead.getTarget());
        CtExpression<Integer> index = arrayRead.getIndexExpression();
        CtTypeReference<?> indexType = getActualType(arrayRead.getIndexExpression());
        scan(index);
        Expr indexExpr = currentResult;
        index.putMetadata("value", currentResult);

        // Unboxing conversion
        if (!indexType.isPrimitive())
        {
            indexExpr = memory.read(indexType.unbox(), (IntExpr) indexExpr);
        }

        indexExpr = promoteNumericValue(context, indexExpr, indexType);
        CtArrayTypeReference<?> arrayType = (CtArrayTypeReference) arrayRead.getTarget().getType();
        currentResult = memory.readArray(arrayType, targetExpr, indexExpr);
        arrayRead.putMetadata("value", currentResult);
        checkArrayRead(arrayRead);
        visitDereference(targetExpr);
    }

    @Override
    public <T> void visitCtArrayWrite(CtArrayWrite<T> arrayWrite)
    {
        scan(arrayWrite.getIndexExpression());
        scan(arrayWrite.getTarget());
        currentResult = getTargetValue(context, variablesMap, memory, arrayWrite.getTarget());
        arrayWrite.putMetadata("value", currentResult);
        checkArrayWrite(arrayWrite);
        visitDereference(currentResult);
    }

    /**
     * Checks if the return value of the binary operator is boolean.
     */
    private boolean isBooleanOperatorKind(BinaryOperatorKind kind)
    {
        switch (kind)
        {
            case OR:
            case AND:
            case EQ:
            case NE:
            case LT:
            case GT:
            case LE:
            case GE:
            case INSTANCEOF:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if the return value of the unary operator is boolean.
     */
    private boolean isBooleanOperatorKind(UnaryOperatorKind kind)
    {
        return kind.equals(UnaryOperatorKind.NOT);
    }

    /**
     * Calculates the value of the binary operation.
     */
    private Expr calcBinaryOperator(Expr leftValue, CtTypeReference<?> leftType,
                                    Expr rightValue, CtTypeReference<?> rightType,
                                    BinaryOperatorKind kind)
    {
        final boolean workWithReferences =
            (kind == BinaryOperatorKind.EQ || kind == BinaryOperatorKind.NE ||
            (kind == BinaryOperatorKind.PLUS && (isString(leftType) || isString(rightType))))
            && !leftType.isPrimitive() && !rightType.isPrimitive();

        if (!workWithReferences)
        {
            // Handle unknown types
            if (!isCalculable(leftType) || leftValue == null)
            {
                return isBooleanOperatorKind(kind) ? context.mkFreshConst("", context.getBoolSort()) : null;
            }

            // Unboxing conversion
            if (!leftType.isPrimitive())
            {
                leftValue = memory.read(leftType.unbox(), (IntExpr) leftValue);
            }

            // Handle unknown types
            if (!isCalculable(rightType) || rightValue == null)
            {
                return isBooleanOperatorKind(kind) ? context.mkFreshConst("", context.getBoolSort()) : null;
            }

            // Unboxing conversion
            if (!rightType.isPrimitive())
            {
                rightValue = memory.read(rightType.unbox(), (IntExpr) rightValue);
            }

            // Binary Numeric Promotion
            if (leftValue instanceof BitVecExpr && rightValue instanceof BitVecExpr)
            {
                Expr[] result = promoteNumericValues(context, leftValue, leftType, rightValue, rightType);
                leftValue = result[0];
                rightValue = result[1];
            }
        }

        switch (kind)
        {
            case AND:
                return context.mkAnd((BoolExpr) leftValue, (BoolExpr) rightValue);
            case OR:
                return context.mkOr((BoolExpr) leftValue, (BoolExpr) rightValue);
            case BITOR:
                if (leftValue instanceof BitVecExpr && rightValue instanceof BitVecExpr)
                {
                    return context.mkBVOR((BitVecExpr) leftValue, (BitVecExpr) rightValue);
                }
                else
                {
                    return context.mkOr((BoolExpr) leftValue, (BoolExpr) rightValue);
                }
            case BITXOR:
                if (leftValue instanceof BitVecExpr && rightValue instanceof BitVecExpr)
                {
                    return context.mkBVXOR((BitVecExpr) leftValue, (BitVecExpr) rightValue);
                }
                else
                {
                    return context.mkDistinct(leftValue, rightValue);
                }
            case BITAND:
                if (leftValue instanceof BitVecExpr && rightValue instanceof BitVecExpr)
                {
                    return context.mkBVAND((BitVecExpr) leftValue, (BitVecExpr) rightValue);
                }
                else
                {
                    return context.mkAnd((BoolExpr) leftValue, (BoolExpr) rightValue);
                }
            case EQ:
                return context.mkEq(leftValue, rightValue);
            case NE:
                return context.mkDistinct(leftValue, rightValue);
            case LT:
                return context.mkBVSLT((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case LE:
                return context.mkBVSLE((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case GT:
                return context.mkBVSGT((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case GE:
                return context.mkBVSGE((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case SL:
                return context.mkBVSHL((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case SR:
                return context.mkBVASHR((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case USR:
                return context.mkBVLSHR((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case PLUS:
                if (leftValue instanceof BitVecExpr && rightValue instanceof BitVecExpr)
                {
                    return context.mkBVAdd((BitVecExpr) leftValue, (BitVecExpr) rightValue);
                }
                else
                {
                    IntExpr result = (IntExpr) context.mkFreshConst("", context.getIntSort());
                    solver.add(context.mkDistinct(result, context.mkInt(Memory.nullPointer())));
                    return result;
                }
            case MINUS:
                return context.mkBVSub((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case MUL:
                return context.mkBVMul((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case DIV:
                return context.mkBVSDiv((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            case MOD:
                return context.mkBVSRem((BitVecExpr) leftValue, (BitVecExpr) rightValue);
            default:
                throw new RuntimeException("Unexpected binary operator");
        }
    }

    @Override
    public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator)
    {
        CtTypeReference<?> leftType = getActualType(operator.getLeftHandOperand());
        CtTypeReference<?> rightType = getActualType(operator.getRightHandOperand());
        CtTypeReference<?> operatorType = getActualType(operator);
        BinaryOperatorKind kind = operator.getKind();

        // TODO: Remove this temporary workaround when spoon bug is fixed: https://github.com/INRIA/spoon/pull/3075
        if (kind == BinaryOperatorKind.PLUS
            && ((leftType != null && isString(leftType))
                || (rightType != null && isString(rightType)
                || (operatorType != null && isString(operatorType)))))
        {
            CtTypeReference stringTypeReference = factory.Type().STRING;

            if (operator.getType() == null)
            {
                operator.setType(stringTypeReference);
            }

            if (leftType == null)
            {
                operator.getLeftHandOperand().setType(stringTypeReference);
                leftType = stringTypeReference;
            }

            if (rightType == null)
            {
                operator.getRightHandOperand().setType(stringTypeReference);
                rightType = stringTypeReference;
            }
        }

        scan(operator.getLeftHandOperand());
        Expr leftValue = currentResult;
        checkBinaryOperatorLeft(kind, operator.getLeftHandOperand());

        // Short circuit evaluation:
        // When visiting right operand of AND, we should add leftValue to the current conditions;
        // When visiting right operand of OR, we should add NOT leftValue to the current conditions;
        BoolExpr prev = currentConditions;
        if (kind == BinaryOperatorKind.AND || kind == BinaryOperatorKind.OR)
        {
            Expr predicateValue = leftValue;
            if (!leftType.isPrimitive()) // Unboxing conversion
            {
                predicateValue = memory.read(leftType.unbox(), (IntExpr) predicateValue);
            }
            BoolExpr res = kind == BinaryOperatorKind.OR ? context.mkNot((BoolExpr) predicateValue) : (BoolExpr) predicateValue;
            currentConditions = currentConditions == null ? res : context.mkAnd(currentConditions, res);
        }
        scan(operator.getRightHandOperand());
        Expr rightValue = currentResult;
        checkBinaryOperatorRight(kind, operator.getRightHandOperand());
        currentConditions = prev;

        currentResult = calcBinaryOperator(leftValue, leftType, rightValue, rightType, kind);
        currentResult = applyCasts(currentResult, operator.getType(), operator.getTypeCasts());
        operator.putMetadata("value", currentResult);
        checkBinaryOperatorResult(operator);
    }

    @Override
    public <T> void visitCtUnaryOperator(CtUnaryOperator<T> operator)
    {
        scan(operator.getOperand());
        Expr operandValue = currentResult;

        UnaryOperatorKind kind = operator.getKind();
        CtTypeReference<?> operandType = getActualType(operator.getOperand());

        // Handle unknown types
        if (!isCalculable(operandType) || operandValue == null)
        {
            currentResult = isBooleanOperatorKind(kind) ? context.mkFreshConst("", context.getBoolSort()) : null;
            operator.putMetadata("value", currentResult);
            return;
        }

        // Unary Numeric Promotion
        if (operandValue instanceof BitVecExpr)
        {
            operandValue = promoteNumericValue(context, operandValue, operandType);
        }

        // Unboxing conversion
        if (!operandType.isPrimitive())
        {
            operandValue = memory.read(operandType.unbox(), (IntExpr) operandValue);
        }

        switch (kind)
        {
            case NOT:
                currentResult = context.mkNot((BoolExpr) operandValue);
                break;
            case NEG:
                currentResult = context.mkBVNeg((BitVecExpr) operandValue);
                break;
            case POS:
                currentResult = operandValue;
                break;
            case COMPL:
                int size = TypeUtils.isLong(operandType) ? 64 : 32;
                currentResult = context.mkBVSub(context.mkBVNeg((BitVecExpr) operandValue), context.mkBV(1, size));
                break;
            case POSTINC:
            case PREINC:
            case POSTDEC:
            case PREDEC:
                CtExpression<T> operand = operator.getOperand();
                Expr prevExpr;
                if (operand instanceof CtArrayWrite)
                {
                    IntExpr targetExpr = getTargetValue(context, variablesMap, memory, ((CtArrayWrite<T>) operand).getTarget());
                    CtExpression<Integer> index = ((CtArrayWrite<T>) operand).getIndexExpression();
                    CtTypeReference<?> indexType = getActualType(index);
                    Expr indexExpr = (Expr) index.getMetadata("value");
                    indexExpr = promoteNumericValue(context, indexExpr, indexType);
                    CtTypeReference<?> arrayType = ((CtArrayWrite<T>) operand).getTarget().getType();
                    prevExpr = memory.readArray((CtArrayTypeReference) arrayType, targetExpr, indexExpr);
                }
                else
                {
                    CtVariableReference<?> variable = ((CtVariableWrite<?>) operand).getVariable();
                    prevExpr = variablesMap.get(variable);
                }

                Expr literalValue = makeLiteral(1);
                boolean isIncrement = kind == UnaryOperatorKind.POSTINC || kind == UnaryOperatorKind.PREINC;
                BinaryOperatorKind binOpKind = isIncrement ? BinaryOperatorKind.PLUS : BinaryOperatorKind.MINUS;
                Expr resExpr = calcBinaryOperator(prevExpr, operandType, literalValue, factory.Type().INTEGER_PRIMITIVE, binOpKind);
                CtTypeReference<?> opType = operand.getType().unbox();
                visitAssignment(operand, operandValue, operandType, resExpr, opType);

                if (kind == UnaryOperatorKind.POSTINC || kind == UnaryOperatorKind.POSTDEC)
                {
                    currentResult = prevExpr;
                }

                break;
            default:
                throw new RuntimeException("Unexpected unary operator");
        }

        currentResult = applyCasts(currentResult, operator.getType(), operator.getTypeCasts());
        operator.putMetadata("value", currentResult);
    }
}
