package com.github.egor18.jdataflow.warning;

public enum WarningKind
{
    EXCEPTION("Exception occurred during the analysis: %s"),
    TIMEOUT("Timed out analyzing element"),
    ALWAYS_TRUE("Condition '%s' is always true when reached"),
    ALWAYS_FALSE("Condition '%s' is always false when reached"),
    NULL_DEREFERENCE("Null dereference occurs in the '%s' expression"),
    NULL_DEREFERENCE_INTERPROCEDURAL("Null dereference occurs inside the '%s' invocation"),
    ARRAY_INDEX_IS_OUT_OF_BOUNDS("Array index '%s' is out of bounds"),
    RESULT_IGNORED("Result of '%s' is ignored"),
    ;

    String message;

    WarningKind(String message)
    {
        this.message = message;
    }
}
