package com.tencent.devops.common.expression.expression.functions

import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

class StartsWith : Function() {
    override val traceFullyRealized: Boolean
        get() = false

    override fun createNode(): Function = StartsWith()

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val left = parameters[0].evaluate(context)
        if (left.isPrimitive) {
            val leftString = left.convertToString()

            val right = parameters[1].evaluate(context)
            if (right.isPrimitive) {
                val rightString = right.convertToString()
                return Pair(null, leftString.startsWith(rightString, ignoreCase = false))
            }
        }

        return Pair(null, false)
    }
}
