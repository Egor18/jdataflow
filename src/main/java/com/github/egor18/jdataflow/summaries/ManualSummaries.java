package com.github.egor18.jdataflow.summaries;

import com.github.egor18.jdataflow.scanners.DataFlowScanner;
import com.microsoft.z3.*;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashMap;
import java.util.Map;

import static com.github.egor18.jdataflow.utils.SummaryUtils.getFullSignature;
import static com.github.egor18.jdataflow.utils.TypeUtils.getListSizeReference;

public class ManualSummaries
{
    private DataFlowScanner scanner;

    private Map<String, FunctionSummary> summariesTable = new HashMap<>();

    public Map<String, FunctionSummary> getTable()
    {
        return summariesTable;
    }

    public ManualSummaries(DataFlowScanner scanner)
    {
        this.scanner = scanner;
        initializeTable();
    }

    private void add(String qualifiedTypeName, String methodSignature, FunctionSummary summary)
    {
        qualifiedTypeName = qualifiedTypeName.replaceAll(" ","");
        methodSignature = methodSignature.replaceAll(" ","");
        String signature = getFullSignature(qualifiedTypeName, methodSignature);
        summary.setManual(true);
        summariesTable.put(signature, summary);
    }

    private BitVecExpr getListSize(IntExpr targetExpr)
    {
        Context context = scanner.getContext();
        Solver solver = scanner.getSolver();
        CtTypeReference<?> listSizeReference = getListSizeReference(scanner.getFactory());
        BitVecExpr sizeExpr = (BitVecExpr) scanner.getMemory().read(listSizeReference, targetExpr);
        solver.add(context.mkBVSGE(sizeExpr, context.mkBV(0, 32)));
        return sizeExpr;
    }

    private BoolExpr isListEmpty(IntExpr targetExpr)
    {
        Context context = scanner.getContext();
        BitVecExpr sizeExpr = getListSize(targetExpr);
        return context.mkEq(sizeExpr, context.mkBV(0, 32));
    }

    private BoolExpr isListContains(IntExpr targetExpr, Expr elementExpr)
    {
        Context context = scanner.getContext();
        return (BoolExpr) context.mkITE(isListEmpty(targetExpr),
                           context.mkFalse(),
                           context.mkFreshConst("", context.getBoolSort()));
    }

    private void initializeTable()
    {
        Context context = scanner.getContext();

        add("java.lang.Math", "max(int,int)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSGT((BitVecExpr) args[0].expr, (BitVecExpr) args[1].expr),
                        args[0].expr,
                        args[1].expr
                );
            })
        );

        add("java.lang.Math", "max(long,long)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSGT((BitVecExpr) args[0].expr, (BitVecExpr) args[1].expr),
                        args[0].expr,
                        args[1].expr
                );
            })
        );

        add("java.lang.Math", "min(int,int)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSGT((BitVecExpr) args[0].expr, (BitVecExpr) args[1].expr),
                        args[1].expr,
                        args[0].expr
                );
            })
        );

        add("java.lang.Math", "min(long,long)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSGT((BitVecExpr) args[0].expr, (BitVecExpr) args[1].expr),
                        args[1].expr,
                        args[0].expr
                );
            })
        );

        add("java.lang.Math", "abs(int)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSLT((BitVecExpr) args[0].expr, context.mkBV(0, 32)),
                        context.mkBVNeg((BitVecExpr) args[0].expr),
                        args[0].expr
                );
            })
        );

        add("java.lang.Math", "abs(long)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSLT((BitVecExpr) args[0].expr, context.mkBV(0, 64)),
                        context.mkBVNeg((BitVecExpr) args[0].expr),
                        args[0].expr
                );
            })
        );

        add("java.lang.Object" , "equals(java.lang.Object)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkEq(target.expr, args[0].expr),
                        context.mkTrue(),
                        context.mkFreshConst("", context.getBoolSort())
                );
            })
        );

        add("java.util.List", "add(java.lang.Object)", new FunctionSummary()
            .readOnlyArgument(0)
            .collectionGenericTypeArgument(0)
            .setReturn((target, args) ->
            {
                CtTypeReference<?> listSizeReference = getListSizeReference(scanner.getFactory());
                Expr prevSizeExpr = scanner.getMemory().read(listSizeReference, (IntExpr) target.expr);
                scanner.getMemory().write(listSizeReference, (IntExpr) target.expr, context.mkBVAdd((BitVecExpr) prevSizeExpr, context.mkBV(1, 32)));
                scanner.getMemory().writeArray(target.type, (IntExpr) target.expr, prevSizeExpr, args[0].expr);
                return context.mkTrue();
            })
        );

        add("java.util.List", "set(int,java.lang.Object)", new FunctionSummary()
            .readOnlyArgument(0)
            .readOnlyArgument(1)
            .collectionGenericTypeArgument(1)
            .setReturn((target, args) ->
            {
                Expr prevExpr = scanner.getMemory().readArray(target.type, (IntExpr) target.expr, args[0].expr);
                scanner.getMemory().writeArray(target.type, (IntExpr) target.expr, args[0].expr, args[1].expr);
                return prevExpr;
            })
        );

        add("java.util.List" , "get(int)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return scanner.getMemory().readArray(target.type, (IntExpr) target.expr, args[0].expr);
            })
        );

        add("java.util.List" , "size()", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return getListSize((IntExpr) target.expr);
            })
        );

        add("java.util.List" , "isEmpty()", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return isListEmpty((IntExpr) target.expr);
            })
        );

        add("java.util.List" , "contains(java.lang.Object)", new FunctionSummary()
            .pure()
            .collectionGenericTypeArgument(0)
            .setReturn((target, args) ->
            {
                return isListContains((IntExpr) target.expr, args[0].expr);
            })
        );
    }
}
