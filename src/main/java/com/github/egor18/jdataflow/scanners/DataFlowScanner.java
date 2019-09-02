package com.github.egor18.jdataflow.scanners;

import com.github.egor18.jdataflow.Configuration;
import com.github.egor18.jdataflow.memory.Memory;
import com.github.egor18.jdataflow.misc.BranchData;
import com.github.egor18.jdataflow.misc.ConditionStatus;
import com.github.egor18.jdataflow.misc.FlagReference;
import com.github.egor18.jdataflow.summaries.interfaces.EffectFunction;
import com.github.egor18.jdataflow.summaries.FunctionSummary;
import com.github.egor18.jdataflow.summaries.ManualSummaries;
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
import java.util.stream.Collectors;

import static com.github.egor18.jdataflow.utils.CommonUtils.getTargetValue;
import static com.github.egor18.jdataflow.utils.PromotionUtils.*;
import static com.github.egor18.jdataflow.utils.SummaryUtils.getFullSignature;
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

    // Indirect modifications (i.e. within inner classes and lambdas) of a current method
    private List<CtElement> currentMethodIndirectModifications = new ArrayList<>();

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

    // Whether the scanner is inside loop entry condition
    private boolean isInsideLoopEntryCondition = false;

    // Table that contains all function summaries (both manual and deduced)
    private Map<String, FunctionSummary> functionSummariesTable;

    // Current function summary
    private FunctionSummary currentFunctionSummary;

    // This stack is used during interprocedural analysis in order not to get stuck inside (mutually) recursive functions
    private Deque<String> functionsCallStack = new ArrayDeque<>();

    // This stack is used when analyzing final fields in order not to get stuck inside (mutually) recursive fields
    private Deque<CtFieldReference> fieldsCallStack = new ArrayDeque<>();

    // Analyzer configuration
    private Configuration config;

    public DataFlowScanner(Factory factory, Configuration config)
    {
        this.factory = factory;
        this.context = new Context();
        this.solver = context.mkSolver();
        this.memory = new Memory(context);
        this.config = config;
        this.functionSummariesTable = new ManualSummaries(this).getTable();
    }

    public BoolExpr getReturnExpr()
    {
        return (BoolExpr) variablesMap.get(returnFlagReference);
    }

    public BoolExpr getBreakExpr()
    {
        return (BoolExpr) variablesMap.get(breakFlagReference);
    }

    public BoolExpr getContinueExpr()
    {
        return (BoolExpr) variablesMap.get(continueFlagReference);
    }

    public BoolExpr getThrowExpr()
    {
        return (BoolExpr) variablesMap.get(throwFlagReference);
    }

    public void setReturnExpr(BoolExpr value)
    {
        variablesMap.put(returnFlagReference, value);
    }

    public void setBreakExpr(BoolExpr value)
    {
        variablesMap.put(breakFlagReference, value);
    }

    public void setContinueExpr(BoolExpr value)
    {
        variablesMap.put(continueFlagReference, value);
    }

    public void setThrowExpr(BoolExpr value)
    {
        variablesMap.put(throwFlagReference, value);
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

    public boolean isInsideLoopEntryCondition()
    {
        return isInsideLoopEntryCondition;
    }

    private void resetControlFlowFlags()
    {
        returnFlagReference = FlagReference.makeFreshReturnReference();
        variablesMap.put(returnFlagReference, context.mkFalse());

        breakFlagReference = FlagReference.makeFreshBreakReference();
        variablesMap.put(breakFlagReference, context.mkFalse());

        continueFlagReference = FlagReference.makeFreshContinueReference();
        variablesMap.put(continueFlagReference, context.mkFalse());

        throwFlagReference = FlagReference.makeFreshThrowReference();
        variablesMap.put(throwFlagReference, context.mkFalse());
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

    private void visitImpure()
    {
        if (currentFunctionSummary != null)
        {
            currentFunctionSummary.setPure(false);
        }
    }

    @Override
    public void visitCtIf(CtIf ifElement)
    {
        BoolExpr conditionExpr = visitCondition(ifElement.getCondition());

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
                isInsideLoopEntryCondition = true;
                visitCondition(loopCondition);
                isInsideLoopEntryCondition = false;
            }
            ResetOnModificationScanner resetScanner = new ResetOnModificationScanner(context, variablesMap, memory);
            resetScanner.scan(loopCondition);
            Arrays.stream(loopBody).forEach(resetScanner::scan);
            iterationConditionExpr = loopCondition == null ? makeFreshBool(context) : visitCondition(loopCondition);
            iterationBranchData = visitBranch(iterationConditionExpr, loopBody);
        }
        else
        {
            ResetOnModificationScanner resetScanner = new ResetOnModificationScanner(context, variablesMap, memory);
            resetScanner.scan(loopCondition);
            Arrays.stream(loopBody).forEach(resetScanner::scan);
            iterationBranchData = visitBranch(context.mkTrue(), loopBody);
            iterationConditionExpr = loopCondition == null ? makeFreshBool(context) : visitCondition(loopCondition);
        }

        // Save information about break, return, throw
        variablesMap.put(breakFlagReference, iterationBranchData.getVariablesMap().get(breakFlagReference));
        variablesMap.put(returnFlagReference, iterationBranchData.getVariablesMap().get(returnFlagReference));
        variablesMap.put(throwFlagReference, iterationBranchData.getVariablesMap().get(throwFlagReference));

        BoolExpr currentBreakExpr = getBreakExpr();
        BoolExpr currentReturnExpr = getReturnExpr();
        BoolExpr currentThrowExpr = getThrowExpr();

        // Reset flow flags after exiting the loop
        variablesMap.put(breakFlagReference, oldBreakExpr);
        variablesMap.put(continueFlagReference, oldContinueExpr);

        // Invert loop condition
        solver.add(context.mkOr(context.mkNot(iterationConditionExpr), currentBreakExpr, currentReturnExpr, currentThrowExpr));
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

    private BoolExpr visitCondition(CtExpression<Boolean> condition)
    {
        scan(condition);
        Expr conditionValue = currentResult;
        condition.putMetadata("value", conditionValue);

        checkCondition(condition);

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
     * If cond is null, a fresh cond will be created for each entry.
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
                BoolExpr arg = cond == null ? (BoolExpr) context.mkFreshConst("", context.mkBoolSort()) : cond;
                T iteExpr = (T) context.mkITE(arg, thenBranchValue, elseBranchValue);
                resultMap.put(reference, iteExpr);
            }
        }
    }

    /**
     * Merges two branches into the current.
     * If cond is null, a fresh cond will be created for each entry.
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
        BoolExpr conditionExpr = visitCondition(conditional.getCondition());

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

        Expr conditionalExpr = context.mkITE(conditionExpr, thenExpr, elseExpr);
        currentResult = applyCasts(conditionalExpr, conditional.getType(), conditional.getTypeCasts());
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

        List<CtCase<? super S>> cases = switchStatement.getCases();
        for (CtCase<? super S> aCase : cases)
        {
            CtExpression<?> caseExpression = aCase.getCaseExpression();
            if (caseExpression == null)
            {
                // Default label
                continue;
            }

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
            checkCondition(caseExpression);
        }

        BoolExpr commonConditionExpr = null;
        for (CtCase<? super S> aCase : cases)
        {
            CtExpression<?> caseExpression = aCase.getCaseExpression();
            BoolExpr conditionExpr = caseExpression == null ? context.mkTrue() : (BoolExpr) caseExpression.getMetadata("value");

            // Connect cases with OR
            commonConditionExpr = (commonConditionExpr == null) ? conditionExpr : context.mkOr(commonConditionExpr, conditionExpr);
            BoolExpr branchExpr = context.mkAnd(commonConditionExpr, context.mkNot(getBreakExpr()));

            BranchData thenBranchData = visitBranch(branchExpr, aCase);
            BranchData elseBranchData = new BranchData(variablesMap, memory);

            mergeBranches(branchExpr, thenBranchData, elseBranchData);
        }

        variablesMap.put(breakFlagReference, breakExpr);
    }

    private void visitTryBranch(CtBlock<?> block)
    {
        // Each variable in try block becomes ITE(freshCond, xTry, xBeforeTry)
        BranchData thenBranchData = visitBranch(context.mkTrue(), block);
        BranchData elseBranchData = new BranchData(variablesMap, memory);
        mergeBranches(null, thenBranchData, elseBranchData);
    }

    private void visitCatchBranches(List<CtCatch> catchers)
    {
        // Only one catcher can be executed at a time
        BoolExpr notPrevCatchersExpr = null;
        for (CtCatch catcher : catchers)
        {
            BoolExpr freshCondExpr = makeFreshBool(context);
            BoolExpr branchExpr = notPrevCatchersExpr == null ? freshCondExpr : context.mkAnd(notPrevCatchersExpr, freshCondExpr);
            BranchData thenBranchData = visitBranch(branchExpr, catcher.getBody());
            BranchData elseBranchData = new BranchData(variablesMap, memory);
            mergeBranches(branchExpr, thenBranchData, elseBranchData);
            notPrevCatchersExpr = notPrevCatchersExpr == null ? context.mkNot(freshCondExpr) : context.mkAnd(notPrevCatchersExpr, context.mkNot(freshCondExpr));
        }
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
        visitCatchBranches(catchers);

        // Finalizer 'ignores' return and throw
        BoolExpr oldReturnExpr = getReturnExpr();
        variablesMap.put(returnFlagReference, context.mkFalse());
        variablesMap.put(throwFlagReference, context.mkFalse());
        scan(finalizer);
        variablesMap.put(returnFlagReference, oldReturnExpr);
        variablesMap.put(throwFlagReference, oldThrowExpr);
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
        setBreakExpr(context.mkTrue());
    }

    @Override
    public void visitCtContinue(CtContinue continueStatement)
    {
        setContinueExpr(context.mkTrue());
    }

    @Override
    public void visitCtThrow(CtThrow throwStatement)
    {
        scan(throwStatement.getThrownExpression());
        setThrowExpr(context.mkTrue());
        visitImpure();

        if (currentFunctionSummary != null)
        {
            // Test code (more complex effects will be deduced in the future)
            if (currentConditions == null && getReturnExpr().isFalse())
            {
                currentFunctionSummary.addEffect((target, args) -> { setThrowExpr(context.mkTrue()); });
            }
        }
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
        visitImpure();
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
        CtExecutableReference<T> executable = invocation.getExecutable();
        CtExecutable<T> executableDeclaration;
        try
        {
            executableDeclaration = executable.getDeclaration();
        }
        catch (Exception e)
        {
            executableDeclaration = null;
        }

        String signature = getFullSignature(executable);

        if (executableDeclaration != null && !config.isInExcludedFile(executableDeclaration))
        {
            if (executableDeclaration instanceof CtMethod)
            {
                CtMethod<T> method = (CtMethod<T>) executableDeclaration;
                if (functionSummariesTable.get(signature) == null)
                {
                    if (isInterproceduralPossible(method) && !functionsCallStack.contains(signature))
                    {
                        scan(method);
                    }
                }
            }
        }

        FunctionSummary summary = functionSummariesTable.get(signature);

        if (summary == null || !summary.isPure())
        {
            visitImpure();
        }

        List<CtExpression<?>> arguments = invocation.getArguments();
        for (CtExpression<?> argument : arguments)
        {
            scan(argument);
            Expr argumentExpr = currentResult;
            if (argumentExpr != null)
            {
                CtTypeReference<?> argumentType = getActualType(argument);
                if (!argumentType.isPrimitive() && !isImmutable(argumentType) && (summary == null || !summary.isPure()))
                {
                    memory.resetObject(argumentType, (IntExpr) argumentExpr);
                }
            }
        }

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
                    if (!isImmutable(targetType) && (summary == null || !summary.isPure()))
                    {
                        memory.resetObject(targetType, (IntExpr) targetExpr);
                    }
                }
            }
        }

        if (summary == null || !summary.isPure())
        {
            IntNum thisExpr = context.mkInt(Memory.thisPointer());
            CtTypeReference thisType = invocation.getParent(CtType.class).getReference();
            memory.resetObject(thisType, thisExpr);

            CtTypeReference superType = thisType.getSuperclass();
            if (superType != null)
            {
                memory.resetObject(superType, thisExpr);
            }

            // Reset all variables that could be possibly modified indirectly
            ResetOnModificationScanner resetScanner = new ResetOnModificationScanner(context, variablesMap, memory);
            for (CtElement indirectModification : currentMethodIndirectModifications)
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

        List<CtTypeReference<?>> formalParameters = invocation.getExecutable().getParameters();
        boolean isEllipsis = arguments.size() != formalParameters.size();

        List<Expr> argsExprs = new ArrayList<>();
        if (summary != null && !isEllipsis) // TODO: handle ellipsis properly
        {
            for (int i = 0; i < arguments.size(); i++)
            {
                CtExpression<?> argument = arguments.get(i);
                CtTypeReference<?> argActualType = getActualType(argument);
                CtTypeReference<?> argFormalType = formalParameters.get(i);
                Expr argExpr = (Expr) argument.getMetadata("value");
                argExpr = applyCasts(argExpr, argActualType, Collections.singletonList(argFormalType));
                argsExprs.add(argExpr);
            }

            for (EffectFunction effect : summary.getEffects())
            {
                effect.apply(targetExpr, argsExprs.toArray(new Expr[0]));
            }
        }

        CtTypeReference<?> returnType = invocation.getType();
        if (!isVoid(returnType))
        {
            Expr returnValue;
            if (summary != null && !isEllipsis) // TODO: handle ellipsis properly
            {
                if (summary.getReturnFunc() != null)
                {
                    returnValue = summary.getReturnFunc().apply(targetExpr, argsExprs.toArray(new Expr[0]));
                }
                else if (summary.getSymbolicReturn() != null)
                {
                    List<Expr> actualArgs = new ArrayList<>();
                    if (!executable.isStatic())
                    {
                        actualArgs.add(targetExpr);
                    }
                    actualArgs.addAll(argsExprs);
                    returnValue = summary.getSymbolicReturn().apply(actualArgs.toArray(new Expr[0]));
                }
                else
                {
                    returnValue = makeFreshConstFromType(context, returnType);
                }
            }
            else
            {
                returnValue = makeFreshConstFromType(context, returnType);
            }

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
    public void visitCtSynchronized(CtSynchronized synchro)
    {
        // There is synchronization so something could be changed from another thread => reset this
        IntNum thisExpr = context.mkInt(Memory.thisPointer());
        CtTypeReference thisType = synchro.getParent(CtType.class).getReference();
        memory.resetObject(thisType, thisExpr);
        scan(synchro.getExpression());
        scan(synchro.getBlock());
        visitImpure();
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

        setReturnExpr(context.mkTrue());
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
    public <T> void visitCtField(CtField<T> field)
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
                    index = makeFreshInt(context);
                    variablesMap.put(declaringType, index);
                }
            }
            else
            {
                index = context.mkInt(Memory.thisPointer());
            }

            memory.write(field.getReference(), (IntExpr) index, defaultExpr);

            // Put the value of a static final field right into it
            if (field.isStatic() && field.isFinal())
            {
                field.putMetadata("value", defaultExpr);
            }
        }
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass)
    {
        System.out.println("Analyzing class: " + ctClass.getQualifiedName());
        List<CtTypeMember> typeMembers = ctClass.getTypeMembers();
        int startNumScopes = solver.getNumScopes();
        solver.push();
        Map<CtReference, Expr> oldValues = new HashMap<>(variablesMap);
        Memory oldMemory = new Memory(memory);
        memory.resetMutable();

        try
        {
            // Before visiting a class, we should visit all of its fields
            typeMembers.forEach(m -> {if (m instanceof CtField) { scan(m); }});
            typeMembers.forEach(m -> {if (!(m instanceof CtField)) { scan(m); }});
        }
        catch (Exception e)
        {
            System.out.println("Failed to analyze class " + ctClass.getQualifiedName() + ":");
            if (config.isNoFailsafe())
            {
                throw e;
            }
            else
            {
                e.printStackTrace();
            }
        }
        finally
        {
            variablesMap = oldValues;
            memory = oldMemory;
            solver.pop(solver.getNumScopes() - startNumScopes);
        }
    }

    @Override
    public void visitCtAnonymousExecutable(CtAnonymousExecutable anonymousExec)
    {
        memory.resetMutable();
        currentResult = null;
        currentConditions = null;
        resetControlFlowFlags();

        scan(anonymousExec.getBody());

        // Save values of static final fields initialized in this static block
        if (anonymousExec.isStatic())
        {
            variablesMap.forEach((ref, value) ->
            {
                if (ref instanceof CtFieldReference
                    && ((CtFieldReference) ref).isStatic()
                    && ((CtFieldReference) ref).isFinal())
                {
                    ref.getDeclaration().putMetadata("value", value);
                }
            });
        }
    }

    private void visitMethod(CtElement body, List<CtParameter<?>> parameters)
    {
        int startNumScopes = solver.getNumScopes();
        solver.push();
        Map<CtReference, Expr> oldValues = new HashMap<>(variablesMap);
        Memory oldMemory = new Memory(memory);
        memory.resetMutable();
        BoolExpr oldConditions = currentConditions;
        CtReference oldReturnFlagReference = returnFlagReference;
        CtReference oldBreakFlagReference = breakFlagReference;
        CtReference oldContinueFlagReference = continueFlagReference;
        CtReference oldThrowFlagReference = throwFlagReference;
        currentResult = null;
        currentConditions = null;
        resetControlFlowFlags();

        for (CtParameter<?> parameter : parameters)
        {
            variablesMap.put(parameter.getReference(), makeFreshConstFromType(context, parameter.getType()));
        }

        try
        {
            scan(body);
        }
        catch (Exception e)
        {
            System.out.println("Failed to analyze method " + body.getParent(CtExecutable.class).getSimpleName() + ":");
            if (config.isNoFailsafe())
            {
                throw e;
            }
            else
            {
                e.printStackTrace();
            }
        }
        finally
        {
            variablesMap = oldValues;
            memory = oldMemory;
            currentConditions = oldConditions;
            returnFlagReference = oldReturnFlagReference;
            breakFlagReference = oldBreakFlagReference;
            continueFlagReference = oldContinueFlagReference;
            throwFlagReference = oldThrowFlagReference;
            solver.pop(solver.getNumScopes() - startNumScopes);
        }
    }

    @Override
    public <T> void visitCtConstructor(CtConstructor<T> constructor)
    {
        System.out.println("Analyzing constructor");
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

    private boolean isInterproceduralPossible(CtMethod<?> method)
    {
        return method.isFinal() || method.isStatic() || method.isPrivate();
    }

    @Override
    public <T> void visitCtMethod(CtMethod<T> method)
    {
        String signature = getFullSignature(method.getReference());
        FunctionSummary summary = functionSummariesTable.get(signature);
        boolean alreadyVisited = summary != null && !summary.isManual();
        if (alreadyVisited)
        {
            return;
        }
        System.out.println("Analyzing method: " + method.getSimpleName());
        List<CtElement> previousMethodIndirectModifications = currentMethodIndirectModifications;
        currentMethodIndirectModifications = collectIndirectModifications(method);
        FunctionSummary previousFunctionSummary = currentFunctionSummary;
        currentFunctionSummary = new FunctionSummary();
        currentFunctionSummary.setPure(true);
        if (method.getBody() == null) // native, abstract, etc.
        {
            visitImpure();
        }
        functionsCallStack.push(signature);

        visitMethod(method.getBody(), method.getParameters());

        CtTypeReference<?> returnType = method.getType();
        if (!isVoid(returnType)
            && currentFunctionSummary.isPure()
            && currentFunctionSummary.getReturnFunc() == null
            && !currentFunctionSummary.isManual())
        {
            List<Sort> argsSorts = new ArrayList<>();
            if (!method.isStatic())
            {
                Sort targetSort = context.getIntSort();
                argsSorts.add(targetSort);
            }
            argsSorts.addAll(method.getParameters().stream().map(p -> getTypeSort(context, p.getType())).collect(Collectors.toList()));
            Sort returnSort = getTypeSort(context, method.getType());
            currentFunctionSummary.setSymbolicReturn(context.mkFreshFuncDecl("", argsSorts.toArray(new Sort[0]), returnSort));
        }

        if (isInterproceduralPossible(method))
        {
            functionSummariesTable.put(signature, currentFunctionSummary);
        }
        currentFunctionSummary = previousFunctionSummary;
        currentMethodIndirectModifications = previousMethodIndirectModifications;
        functionsCallStack.pop();
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

    @Override
    public <T, E extends CtExpression<?>> void visitCtExecutableReferenceExpression(CtExecutableReferenceExpression<T, E> expression)
    {
        // Create new object for this executable reference
        int nextPointer = memory.nextPointer();
        IntExpr executableReferenceValue = context.mkInt(nextPointer);
        currentResult = applyCasts(executableReferenceValue, expression.getType(), expression.getTypeCasts());
        expression.putMetadata("value", currentResult);
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
            visitImpure();
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
            visitImpure();
        }

        if (left instanceof CtArrayWrite)
        {
            CtArrayWrite arrayWrite = (CtArrayWrite) left;
            CtExpression index = arrayWrite.getIndexExpression();
            CtTypeReference<?> indexType = getActualType(index);
            Expr indexExpr = (Expr) index.getMetadata("value");

            // Unboxing conversion
            if (!indexType.isPrimitive())
            {
                indexExpr = memory.read(indexType.unbox(), (IntExpr) indexExpr);
            }

            leftData = memory.readArray((CtArrayTypeReference) arrayWrite.getTarget().getType(), (IntExpr) leftValue, indexExpr);
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
            valueExpr = makeFreshInt(context);
            solver.add(context.mkDistinct(valueExpr, context.mkInt(Memory.nullPointer())));
        }
        else
        {
            Object value = ((CtLiteral<?>) literal).getValue();
            valueExpr = makeLiteral(value);
        }

        currentResult = applyCasts(valueExpr, literal.getType(), literal.getTypeCasts());
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
        Expr thisValue = context.mkInt(Memory.thisPointer());
        currentResult = applyCasts(thisValue, thisAccess.getType(), thisAccess.getTypeCasts());
        thisAccess.putMetadata("value", currentResult);
    }

    @Override
    public <T> void visitCtSuperAccess(CtSuperAccess<T> superAccess)
    {
        Expr superValue = context.mkInt(Memory.thisPointer());
        currentResult = applyCasts(superValue, superAccess.getType(), superAccess.getTypeCasts());
        superAccess.putMetadata("value", currentResult);
    }

    private Expr getFinalStaticFieldExpr(CtFieldReference finalFieldReference, IntExpr targetExpr)
    {
        if (!finalFieldReference.isFinal() || !finalFieldReference.isStatic())
        {
            throw new RuntimeException("The field should be final and static");
        }

        // Handle (mutually) recursive fields
        if (fieldsCallStack.contains(finalFieldReference))
        {
            return memory.read(finalFieldReference, targetExpr);
        }

        CtField fieldDeclaration = finalFieldReference.getDeclaration();
        if (fieldDeclaration == null || config.isInExcludedFile(fieldDeclaration))
        {
            return memory.read(finalFieldReference, targetExpr);
        }

        if (fieldDeclaration.getMetadata("value") == null)
        {
            if (fieldDeclaration.getDefaultExpression() != null)
            {
                fieldsCallStack.push(finalFieldReference);
                scan(fieldDeclaration);
                fieldsCallStack.pop();
            }
            else
            {
                List<CtAnonymousExecutable> staticBlocks = finalFieldReference.getDeclaringType()
                                                                              .getDeclaration()
                                                                              .getElements(new TypeFilter<>(CtAnonymousExecutable.class))
                                                                              .stream()
                                                                              .filter(CtModifiable::isStatic)
                                                                              .collect(Collectors.toList());
                fieldsCallStack.push(finalFieldReference);
                staticBlocks.forEach(this::scan);
                fieldsCallStack.pop();
            }

            return memory.read(finalFieldReference, targetExpr);
        }

        return (Expr) fieldDeclaration.getMetadata("value");
    }

    @Override
    public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead)
    {
        CtExpression<?> target = fieldRead.getTarget();

        // Construct type access for static field declared in anonymous type (null in spoon by default)
        IntExpr targetExpr;
        if (target == null
            && fieldRead.getVariable().isStatic()
            && fieldRead.getVariable().getDeclaringType().isAnonymous())
        {
            target = factory.createTypeAccess(fieldRead.getVariable().getDeclaringType());
        }

        scan(target);

        targetExpr = getTargetValue(context, variablesMap, memory, target);

        if (isCalculable(fieldRead.getType())
            && fieldRead.getVariable().isStatic()
            && fieldRead.getVariable().isFinal()
            && fieldRead.getVariable().getDeclaration() != null)
        {
            currentResult = getFinalStaticFieldExpr(fieldRead.getVariable(), targetExpr);
        }
        else
        {
            currentResult = memory.read(fieldRead.getVariable(), targetExpr);
            visitImpure();
        }

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
        visitImpure();
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
        CtArrayTypeReference<?> arrayType = (CtArrayTypeReference) getActualType(arrayRead.getTarget());
        Expr arrayReadExpr = memory.readArray(arrayType, targetExpr, indexExpr);
        currentResult = applyCasts(arrayReadExpr, arrayRead.getType(), arrayRead.getTypeCasts());
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
        visitImpure();
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
                return isBooleanOperatorKind(kind) ? makeFreshBool(context) : null;
            }

            // Unboxing conversion
            if (!leftType.isPrimitive())
            {
                leftValue = memory.read(leftType.unbox(), (IntExpr) leftValue);
            }

            // Handle unknown types
            if (!isCalculable(rightType) || rightValue == null)
            {
                return isBooleanOperatorKind(kind) ? makeFreshBool(context) : null;
            }

            // Unboxing conversion
            if (!rightType.isPrimitive())
            {
                rightValue = memory.read(rightType.unbox(), (IntExpr) rightValue);
            }

            if (kind == BinaryOperatorKind.SL || kind == BinaryOperatorKind.SR || kind == BinaryOperatorKind.USR)
            {
                // The type of the shift expression is the promoted type of the left-hand operand.
                // If the promoted type of the left-hand operand is int, then only 5 lowest-order bits of the right-hand operand are used as the shift distance.
                // If the promoted type of the left-hand operand is long, then only 6 lowest-order bits of the right-hand operand are used as the shift distance.
                leftValue = promoteNumericValue(context, leftValue, leftType);
                int leftSortSize = ((BitVecExpr) leftValue).getSortSize();
                int rightSortSize = ((BitVecExpr) rightValue).getSortSize();
                boolean isLong = leftSortSize == 64;
                int mask = isLong ? 0b111111 : 0b11111;
                if (rightSortSize > leftSortSize)
                {
                    rightValue = context.mkExtract(leftSortSize - 1, 0, (BitVecExpr) rightValue);
                }
                else if (rightSortSize != leftSortSize)
                {
                    if (isLong)
                    {
                        rightValue = extendToLong(context, (BitVecExpr) rightValue, !TypeUtils.isChar(rightType));
                    }
                    else
                    {
                        rightValue = extendToInteger(context, (BitVecExpr) rightValue, !TypeUtils.isChar(rightType));
                    }
                }
                rightValue = context.mkBVAND((BitVecExpr) rightValue, context.mkBV(mask, leftSortSize));
            }
            else
            {
                // Binary Numeric Promotion
                if (leftValue instanceof BitVecExpr && rightValue instanceof BitVecExpr)
                {
                    Expr[] result = promoteNumericValues(context, leftValue, leftType, rightValue, rightType);
                    leftValue = result[0];
                    rightValue = result[1];
                }
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
                    IntExpr result = makeFreshInt(context);
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
            currentResult = isBooleanOperatorKind(kind) ? makeFreshBool(context) : null;
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

                    // Unboxing conversion
                    if (!indexType.isPrimitive())
                    {
                        indexExpr = memory.read(indexType.unbox(), (IntExpr) indexExpr);
                    }

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
