package com.github.egor18.jdataflow.memory;

import com.microsoft.z3.*;
import spoon.reflect.reference.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.egor18.jdataflow.utils.TypeUtils.*;

/**
 * Represents a type-indexed memory model (Burstall’s memory model).
 * In this model each data type or field has its own memory array.
 */
public class Memory
{
    // Maps type reference or field reference to the corresponding memory array
    private Map<CtReference, ArrayExpr> memoryMap = new HashMap<>();

    // Holds next free memory address
    private static int memoryCounter = 1;

    // z3 solver context
    private Context context;

    // z3 solver
    private Solver solver;

    // Represents a pointer to 'this'
    private IntExpr thisPointer;

    // Represents a pointer to 'super'
    private IntExpr superPointer;

    public Map<CtReference, ArrayExpr> getMemoryMap()
    {
        return memoryMap;
    }

    public Memory(Context context, Solver solver)
    {
        this.context = context;
        this.solver = solver;
        this.thisPointer = context.mkIntConst("this");
        this.superPointer = context.mkIntConst("super");
        solver.add(context.mkDistinct(thisPointer, nullPointer()));
    }

    public Memory(Memory other)
    {
        this.context = other.context;
        this.solver = other.solver;
        this.memoryMap = new HashMap<>(other.memoryMap);
        this.thisPointer = other.thisPointer;
        this.superPointer = other.superPointer;
    }

    /**
     * Ensures that corresponding memory array is created, or creates it otherwise.
     */
    private void ensureArrayCreated(CtReference reference)
    {
        // Create memory array for the reference if it does not exist
        if (memoryMap.get(reference) == null)
        {
            ArrayExpr memoryArray;
            ArraySort sort;
            if (reference instanceof CtArrayTypeReference)
            {
                ArraySort arraySort = context.mkArraySort(context.mkBitVecSort(32), getTypeSort(context, ((CtArrayTypeReference) reference).getComponentType()));
                sort = context.mkArraySort(context.mkIntSort(), arraySort);
            }
            else if (reference.getSimpleName().equals("#ARRAY_LENGTH"))
            {
                sort = context.mkArraySort(context.getIntSort(), context.mkBitVecSort(32));
            }
            else
            {
                CtTypeReference typeReference = reference instanceof CtVariableReference ? ((CtVariableReference) reference).getType() : (CtTypeReference) reference;
                sort = context.mkArraySort(context.getIntSort(), getTypeSort(context, typeReference));
            }
            memoryArray = (ArrayExpr) context.mkFreshConst(reference + "_mem_array_", sort);
            memoryMap.put(reference, memoryArray);
        }
    }

    /**
     * Reads from the memory array of the specified type at the index targetExpr.
     */
    public Expr read(CtReference type, IntExpr targetExpr)
    {
        ensureArrayCreated(type);
        ArrayExpr memoryArray = memoryMap.get(type);
        return context.mkSelect(memoryArray, targetExpr);
    }

    /**
     * Reads from the memory array of the specified array type at the index targetExpr and at the arrayIndex position.
     */
    public Expr readArray(CtArrayTypeReference type, IntExpr targetExpr, Expr arrayIndex)
    {
        ensureArrayCreated(type);
        ArrayExpr memoryArray = memoryMap.get(type);
        ArrayExpr arrayValue = (ArrayExpr) context.mkSelect(memoryArray, targetExpr);
        return context.mkSelect(arrayValue, arrayIndex);
    }

    /**
     * Writes value to the memory array of the specified type at the index targetExpr.
     */
    public void write(CtReference type, IntExpr targetExpr, Expr value)
    {
        ensureArrayCreated(type);
        ArrayExpr memoryArray = memoryMap.get(type);
        memoryMap.put(type, context.mkStore(memoryArray, targetExpr, value));
    }

    /**
     * Writes value to the memory array of the specified array type at the index targetExpr and at the arrayIndex position.
     */
    public void writeArray(CtArrayTypeReference type, IntExpr targetExpr, Expr arrayIndex, Expr value)
    {
        ensureArrayCreated(type);
        ArrayExpr memoryArray = memoryMap.get(type);
        ArrayExpr oldArrayValue = (ArrayExpr) context.mkSelect(memoryArray, targetExpr);
        ArrayExpr newArrayValue = context.mkStore(oldArrayValue, arrayIndex, value);
        memoryMap.put(type, context.mkStore(memoryArray, targetExpr, newArrayValue));
    }

    /**
     * Returns a pointer to the next free memory address and increments is.
     * Essentially it represents an allocation.
     */
    public IntExpr nextPointer()
    {
        return context.mkInt(memoryCounter++);
    }

    /**
     * Returns null pointer.
     */
    public IntExpr nullPointer()
    {
        return context.mkInt(0);
    }

    /**
     * Returns this pointer.
     */
    public IntExpr thisPointer()
    {
        return thisPointer;
    }

    /**
     * Returns super pointer.
     */
    public IntExpr superPointer()
    {
        return superPointer;
    }

    /**
     * Resets the value of the object of specified type and address.
     * (It resets its fields, but not the value of the reference.)
     */
    public void resetObject(CtTypeReference type, IntExpr address)
    {
        List<CtTypeReference<?>> superclasses = getAllSuperclasses(type);
        String typeName = type.getQualifiedName();

        // Reset fields and array elements
        for (Map.Entry<CtReference, ArrayExpr> entry : memoryMap.entrySet())
        {
            CtReference reference = entry.getKey();
            if (reference instanceof CtFieldReference)
            {
                String fieldDeclaringTypeName = ((CtFieldReference) reference).getDeclaringType().getQualifiedName();
                if (typeName.equals(fieldDeclaringTypeName) || superclasses.stream().anyMatch(t -> t.getQualifiedName().equals(fieldDeclaringTypeName)))
                {
                    if (memoryMap.get(reference) != null)
                    {
                        Sort sort = getTypeSort(context, ((CtFieldReference) reference).getType());
                        write(reference, address, context.mkFreshConst("", sort));
                    }
                }
            }
            else if (reference instanceof CtArrayTypeReference)
            {
                if (((CtArrayTypeReference) reference).getQualifiedName().equals(type.getQualifiedName()))
                {
                    ArrayExpr memoryArray = memoryMap.get(reference);
                    ArrayExpr oldArrayValue = (ArrayExpr) context.mkSelect(memoryArray, address);
                    ArrayExpr newArrayValue = (ArrayExpr) context.mkFreshConst("", oldArrayValue.getSort());
                    memoryMap.put(reference, context.mkStore(memoryArray, address, newArrayValue));
                }
            }
        }

        // Reset calculable value
        if (isCalculable(type))
        {
            Sort sort = getTypeSort(context, type.unbox());
            write(type.unbox(), address, context.mkFreshConst("", sort));
        }
    }

    /**
     * Completely resets memory.
     */
    public void reset()
    {
        memoryMap.clear();
    }

    /**
    * Resets static memory counter.
    */
    public static void resetMemoryCounter()
    {
        memoryCounter = 1;
    }

    /**
     * Resets all mutable elements in memory.
     */
    public void resetMutable()
    {
        memoryMap.entrySet().removeIf(e -> !(e.getKey() instanceof CtFieldReference
                                             && ((CtFieldReference) e.getKey()).isFinal()));
    }
}
