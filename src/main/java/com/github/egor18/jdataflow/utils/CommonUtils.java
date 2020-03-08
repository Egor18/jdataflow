package com.github.egor18.jdataflow.utils;

import com.github.egor18.jdataflow.memory.Memory;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.*;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.github.egor18.jdataflow.utils.PromotionUtils.promoteNumericValue;
import static com.github.egor18.jdataflow.utils.TypeUtils.getActualType;
import static com.github.egor18.jdataflow.utils.TypeUtils.makeFreshInt;

public final class CommonUtils
{
    private CommonUtils() {}

    /**
     * Gets target expression value by going from left to right.
     * For example, 'a.b' is calculated as follows: memory[T2.b][memory[T1.a][a]]
     */
    public static IntExpr getTargetValue(Context context, Map<CtReference, Expr> variablesMap, Memory memory, CtExpression<?> target)
    {
        Deque<CtExpression> targets = new ArrayDeque<>();
        while (target instanceof CtTargetedExpression)
        {
            targets.addFirst(target);
            target = ((CtTargetedExpression) target).getTarget();
        }

        if (target != null)
        {
            targets.addFirst(target);
        }

        // Traverse all targets left to right
        IntExpr targetValue = null;
        for (CtExpression t : targets)
        {
            if (t instanceof CtFieldRead)
            {
                if (targetValue == null)
                {
                    return makeFreshInt(context);
                }
                targetValue = (IntExpr) memory.read(((CtFieldRead) t).getVariable(), targetValue);
            }
            else if (t instanceof CtArrayRead)
            {
                if (targetValue == null)
                {
                    return makeFreshInt(context);
                }
                CtArrayRead arrayRead = (CtArrayRead) t;
                CtExpression index = arrayRead.getIndexExpression();
                CtTypeReference<?> indexType = getActualType(index);
                Expr arrayIndex = (Expr) index.getMetadata("value");
                if (arrayIndex == null)
                {
                    arrayIndex = context.mkFreshConst("", context.mkBitVecSort(32));
                }
                else if (!getActualType(index).isPrimitive())
                {
                    // Unboxing conversion
                    arrayIndex = memory.read(getActualType(index).unbox(), (IntExpr) arrayIndex);
                }
                arrayIndex = promoteNumericValue(context, arrayIndex, indexType);
                targetValue = (IntExpr) memory.readArray(getActualType(arrayRead.getTarget()), targetValue, arrayIndex);
            }
            else if (t instanceof CtThisAccess || t instanceof CtSuperAccess)
            {
                targetValue = memory.thisPointer();
            }
            else if (t instanceof CtVariableRead)
            {
                targetValue = (IntExpr) variablesMap.get(((CtVariableRead) t).getVariable());
            }
            else if (t instanceof CtTypeAccess)
            {
                CtTypeReference accessedType = ((CtTypeAccess) t).getAccessedType();
                targetValue = (IntExpr) variablesMap.get(accessedType);
                if (targetValue == null)
                {
                    targetValue = makeFreshInt(context);
                    variablesMap.put(accessedType, targetValue);
                }
            }
            else
            {
                // Impure functions and other unknown stuff
                targetValue = makeFreshInt(context);
            }
        }

        return targetValue != null ? targetValue : makeFreshInt(context);
    }

    /**
     * Returns true if the body of this method is one statement returning a corresponding field
     */
    public static boolean isGetter(CtMethod<?> method)
    {
        if (method.getBody() == null)
        {
            return false;
        }

        if (method.isStatic())
        {
            return false;
        }

        if (!method.getSimpleName().toLowerCase().startsWith("get"))
        {
            return false;
        }

        if (method.getBody().getStatements().size() != 1)
        {
            return false;
        }

        CtStatement statement = method.getBody().getStatement(0);
        if (!(statement instanceof CtReturn))
        {
            return false;
        }

        CtExpression returnedExpression = ((CtReturn) statement).getReturnedExpression();
        if (!(returnedExpression instanceof CtFieldRead))
        {
            return false;
        }

        if (((CtFieldRead) returnedExpression).getVariable() == null)
        {
            return false;
        }

        if (!returnedExpression.getTypeCasts().isEmpty())
        {
            return false;
        }

        if (!method.getType().equals(returnedExpression.getType()))
        {
            return false;
        }

        String getName = method.getSimpleName().toLowerCase().split("get", 2)[1];
        String varName = ((CtFieldRead) returnedExpression).getVariable().getSimpleName().toLowerCase();

        return getName.equals(varName);
    }

    /**
     * Returns true if the body of this method is one statement setting a corresponding field
     */
    public static boolean isSetter(CtMethod<?> method)
    {
        if (method.getBody() == null)
        {
            return false;
        }

        if (method.isStatic())
        {
            return false;
        }

        if (!method.getSimpleName().toLowerCase().startsWith("set"))
        {
            return false;
        }

        if (method.getBody().getStatements().size() != 1)
        {
            return false;
        }

        if (method.getParameters().size() != 1)
        {
            return false;
        }

        CtStatement statement = method.getBody().getStatement(0);
        if (!(statement instanceof CtAssignment))
        {
            return false;
        }

        CtExpression<?> assignment = ((CtAssignment) statement).getAssignment();
        if (!(assignment instanceof CtVariableRead))
        {
            return false;
        }

        if (!((CtVariableRead) assignment).getVariable().equals(method.getParameters().get(0).getReference()))
        {
            return false;
        }

        CtExpression<?> assigned = ((CtAssignment) statement).getAssigned();

        if (!(assigned instanceof CtFieldWrite))
        {
            return false;
        }

        if (!assigned.getTypeCasts().isEmpty())
        {
            return false;
        }

        if (!assigned.getType().equals(method.getParameters().get(0).getType()))
        {
            return false;
        }

        String setName = method.getSimpleName().toLowerCase().split("set", 2)[1];
        String varName = ((CtFieldWrite<?>) assigned).getVariable().getSimpleName().toLowerCase();

        return setName.equals(varName);
    }

    public static CtFieldReference getGetterFieldReference(CtMethod<?> method)
    {
        return ((CtFieldRead) ((CtReturn) method.getBody().getStatement(0)).getReturnedExpression()).getVariable();
    }

    public static CtFieldReference getSetterFieldReference(CtMethod<?> method)
    {
        return ((CtFieldWrite) (((CtAssignment) method.getBody().getStatement(0)).getAssigned())).getVariable();
    }

    public static String getTimeStamp()
    {
        return "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]";
    }

    public static void println(String str)
    {
        System.out.println(getTimeStamp() + " " + str);
    }

    public static boolean isEllipsisFunction(CtExecutableReference<?> executable)
    {
        List<CtTypeReference<?>> parameters = executable.getParameters();
        if (parameters.isEmpty())
        {
            return false;
        }

        if (!(parameters.get(parameters.size() - 1) instanceof CtArrayTypeReference))
        {
            return false;
        }

        CtExecutable<?> executableDeclaration;
        try
        {
            executableDeclaration = executable.getDeclaration();
        }
        catch (Exception e)
        {
            executableDeclaration = null;
        }

        if (executableDeclaration == null)
        {
            return false;
        }

        List<CtParameter<?>> declaredParameters = executableDeclaration.getParameters();
        return declaredParameters.get(declaredParameters.size() - 1).isVarArgs();
    }
}
