package com.github.egor18.jdataflow.summaries;

import com.github.egor18.jdataflow.misc.ConditionStatus;
import com.github.egor18.jdataflow.scanners.DataFlowScanner;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtTry;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Infers various facts for function summaries from source code.
 */
public class InterproceduralAnalyzer
{
    private DataFlowScanner scanner;
    private Context context;

    public InterproceduralAnalyzer(DataFlowScanner scanner)
    {
        this.scanner = scanner;
        this.context = scanner.getContext();
    }

    public void inferThrowEffect(CtElement throwingElement)
    {
        FunctionSummary currentFunctionSummary = scanner.getCurrentFunctionSummary();

        if (currentFunctionSummary == null)
        {
            return;
        }

        if (throwingElement.getParent(CtTry.class) != null || throwingElement.getParent(CtLoop.class) != null)
        {
            return;
        }

        CtExecutable<?> parentExecutable = throwingElement.getParent(CtExecutable.class);
        if (!(parentExecutable instanceof CtMethod))
        {
            return;
        }

        final Expr[] currentFunctionParameters = parentExecutable.getParameters()
                                                                 .stream()
                                                                 .map(p -> (Expr) p.getMetadata("value"))
                                                                 .toArray(Expr[]::new);

        final BoolExpr currentFunctionConditions = Optional.ofNullable(scanner.getCurrentConditions()).orElse(context.mkTrue());
        final BoolExpr currentFunctionReturnExpr = scanner.getReturnExpr();
        final BoolExpr currentFunctionThrowExpr = scanner.getThrowExpr();

        currentFunctionSummary.addEffect((target, args) ->
        {
            Expr[] argsExprs = Arrays.stream(args).map(a -> a.expr).toArray(Expr[]::new);
            BoolExpr throwConditionExpr = (BoolExpr) currentFunctionConditions.substitute(currentFunctionParameters, argsExprs);
            BoolExpr returnConditionExpr = (BoolExpr) currentFunctionReturnExpr.substitute(currentFunctionParameters, argsExprs);
            BoolExpr throwExpr = (BoolExpr) currentFunctionThrowExpr.substitute(currentFunctionParameters, argsExprs);
            BoolExpr condExpr = context.mkAnd(throwConditionExpr, context.mkNot(returnConditionExpr));
            scanner.setThrowExpr((BoolExpr) context.mkITE(condExpr, throwExpr, scanner.getThrowExpr()));
            inferThrowEffect(scanner.getCurrentInvocation());
        });
    }

    /**
     * Returns true if the definition of any of the parameters contains expr.
     */
    private boolean isParameter(Expr expr, Expr[] parameters)
    {
        // TODO: Do not go too deep
        if (expr.isNumeral())
        {
            return false;
        }

        if (expr.isConst())
        {
            return Arrays.asList(parameters).contains(expr);
        }

        if (expr.isITE())
        {
            Expr[] iteArgs = expr.getArgs();

            if (!isParameter(iteArgs[1], parameters) && !isParameter(iteArgs[2], parameters))
            {
                return false;
            }

            Expr simplifiedCondArg = iteArgs[0].simplify();
            return isParameter(simplifiedCondArg, parameters) || simplifiedCondArg.isNumeral();
        }

        return false;
    }

    public void inferParameterDereferenceEffect(CtElement dereference, Expr targetExpr)
    {
        inferParameterDereferenceEffectImpl(dereference, targetExpr, null);
    }

    private void inferParameterDereferenceEffectImpl(CtElement dereference, Expr targetExpr, BoolExpr previousImplicationExpr)
    {
        FunctionSummary currentFunctionSummary = scanner.getCurrentFunctionSummary();

        if (currentFunctionSummary == null)
        {
            return;
        }

        if (dereference.getParent(CtTry.class) != null || dereference.getParent(CtLoop.class) != null)
        {
            return;
        }

        CtExecutable<?> parentExecutable = dereference.getParent(CtExecutable.class);
        if (!(parentExecutable instanceof CtMethod))
        {
            return;
        }

        final Expr[] currentFunctionParameters = parentExecutable.getParameters()
                                                                 .stream()
                                                                 .map(p -> (Expr) p.getMetadata("value"))
                                                                 .toArray(Expr[]::new);

        if (previousImplicationExpr == null)
        {
            if (!isParameter(targetExpr, currentFunctionParameters))
            {
                return;
            }
        }

        final BoolExpr currentFunctionConditions = Optional.ofNullable(scanner.getCurrentConditions()).orElse(context.mkTrue());
        final BoolExpr currentFunctionReturnExpr = scanner.getReturnExpr();
        final BoolExpr currentFunctionThrowExpr = scanner.getThrowExpr();

        final BiFunction<FunctionTarget, FunctionArgument[], BoolExpr> getImplicationExpr = (target, args) ->
        {
            Expr[] argsExprs = Arrays.stream(args).map(a -> a.expr).toArray(Expr[]::new);
            BoolExpr dereferenceConditionExpr = (BoolExpr) currentFunctionConditions.substitute(currentFunctionParameters, argsExprs);
            BoolExpr returnConditionExpr = (BoolExpr) currentFunctionReturnExpr.substitute(currentFunctionParameters, argsExprs);
            BoolExpr throwExpr = (BoolExpr) currentFunctionThrowExpr.substitute(currentFunctionParameters, argsExprs);
            BoolExpr condExpr = context.mkAnd(scanner.getState(), dereferenceConditionExpr, context.mkNot(returnConditionExpr), context.mkNot(throwExpr));

            BoolExpr implicationExpr;
            if (previousImplicationExpr == null)
            {
                Expr actualParameterExpr = targetExpr.substitute(currentFunctionParameters, argsExprs);
                BoolExpr notNullExpr = context.mkDistinct(actualParameterExpr, scanner.getMemory().nullPointer());
                implicationExpr = context.mkImplies(condExpr, notNullExpr);
            }
            else
            {
                BoolExpr actualPreviousImplicationExpr = (BoolExpr) previousImplicationExpr.substitute(currentFunctionParameters, argsExprs);
                implicationExpr = context.mkImplies(condExpr, actualPreviousImplicationExpr);
            }

            return implicationExpr;
        };

        currentFunctionSummary.addEffect((target, args) ->
        {
            BoolExpr implicationExpr = getImplicationExpr.apply(target, args);

            scanner.getSolver().add(implicationExpr);

            CtExecutable<?> currentInvocationParentExecutable = scanner.getCurrentInvocation().getParent(CtExecutable.class);
            if (!(currentInvocationParentExecutable instanceof CtMethod))
            {
                return;
            }

            final Expr[] currentInvocationParentExecutableParameters =
                currentInvocationParentExecutable.getParameters()
                                                 .stream()
                                                 .map(p -> (Expr) p.getMetadata("value"))
                                                 .toArray(Expr[]::new);

            if (Arrays.stream(args).noneMatch(arg -> isParameter(arg.expr, currentInvocationParentExecutableParameters)))
            {
                return;
            }

            inferParameterDereferenceEffectImpl(scanner.getCurrentInvocation(), targetExpr, implicationExpr);
        });

        currentFunctionSummary.addNullDereferenceCondition((target, args) ->
        {
            BoolExpr implicationExpr = getImplicationExpr.apply(target, args);

            BoolExpr condExpr = (BoolExpr) implicationExpr.getArgs()[0];
            if (scanner.checkCond(condExpr) == ConditionStatus.ALWAYS_FALSE)
            {
                return context.mkFalse(); // Unreachable
            }

            return context.mkNot(implicationExpr);
        });
    }
}
