package com.github.egor18.jdataflow.summaries;

import com.github.egor18.jdataflow.scanners.DataFlowScanner;
import com.microsoft.z3.*;

import java.util.HashMap;
import java.util.Map;

import static com.github.egor18.jdataflow.utils.SummaryUtils.getFullSignature;

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

    private void initializeTable()
    {
        Context context = scanner.getContext();

        add("java.lang.Math", "max(int,int)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSGT((BitVecExpr) args[0], (BitVecExpr) args[1]),
                        args[0],
                        args[1]
                );
            })
        );

        add("java.lang.Math", "max(long,long)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSGT((BitVecExpr) args[0], (BitVecExpr) args[1]),
                        args[0],
                        args[1]
                );
            })
        );

        add("java.lang.Math", "min(int,int)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSGT((BitVecExpr) args[0], (BitVecExpr) args[1]),
                        args[1],
                        args[0]
                );
            })
        );

        add("java.lang.Math", "min(long,long)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSGT((BitVecExpr) args[0], (BitVecExpr) args[1]),
                        args[1],
                        args[0]
                );
            })
        );

        add("java.lang.Math", "abs(int)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSLT((BitVecExpr) args[0], context.mkBV(0, 32)),
                        context.mkBVNeg((BitVecExpr) args[0]),
                        args[0]
                );
            })
        );

        add("java.lang.Math", "abs(long)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkBVSLT((BitVecExpr) args[0], context.mkBV(0, 64)),
                        context.mkBVNeg((BitVecExpr) args[0]),
                        args[0]
                );
            })
        );

        add("java.lang.Object" , "equals(java.lang.Object)", new FunctionSummary()
            .pure()
            .setReturn((target, args) ->
            {
                return context.mkITE(
                        context.mkEq(target, args[0]),
                        context.mkTrue(),
                        context.mkFreshConst("", context.getBoolSort())
                );
            })
        );
    }
}
