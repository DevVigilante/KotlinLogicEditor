package com.example.interpreter

import com.example.model.Block
import com.example.model.BlockType
import kotlinx.coroutines.delay
import java.util.regex.Pattern

class KotlinInterpreter(
    private val onConsoleOutput: (String) -> Unit,
    private val onBlockHighlight: (String?) -> Unit,
    private val onVariablesChanged: (Map<String, Any>) -> Unit,
    private val isRunningCheck: () -> Boolean
) {
    private val environment = mutableMapOf<String, Any>()
    private var totalInstructionsExecuted = 0
    private val maxInstructions = 10000 // Infinite loop protection

    suspend fun executeProgram(blocks: List<Block>, stepDelayMs: Long) {
        environment.clear()
        totalInstructionsExecuted = 0
        onVariablesChanged(emptyMap())
        onBlockHighlight(null)

        try {
            executeBlocks(blocks, stepDelayMs)
            onConsoleOutput("\n[Program Finished Successfully]")
        } catch (e: IllegalStateException) {
            onConsoleOutput("\n[Runtime Error: ${e.message}]")
        } catch (e: Exception) {
            onConsoleOutput("\n[Error: ${e.localizedMessage ?: "Execution failed"}]")
        } finally {
            onBlockHighlight(null)
        }
    }

    private suspend fun executeBlocks(blocks: List<Block>, stepDelayMs: Long) {
        for (block in blocks) {
            if (!isRunningCheck()) return

            if (totalInstructionsExecuted++ > maxInstructions) {
                throw IllegalStateException("Infinite loop or maximum execution limit exceeded ($maxInstructions steps).")
            }

            // Visual Highlight feedback
            onBlockHighlight(block.id)
            if (stepDelayMs > 0) {
                delay(stepDelayMs)
            }

            when (block.type) {
                BlockType.COMMENT -> {
                    // Comments do nothing during execution
                }
                BlockType.VAR_DECL -> {
                    val name = block.varName.trim()
                    if (name.isEmpty()) throw IllegalStateException("Variable declaration missing a name.")
                    val value = evaluateExpression(block.varValue, environment)
                    environment[name] = value
                    onVariablesChanged(HashMap(environment))
                }
                BlockType.VAR_ASSIGN -> {
                    val name = block.varName.trim()
                    if (name.isEmpty()) throw IllegalStateException("Variable assignment missing a name.")
                    if (!environment.containsKey(name)) {
                        throw IllegalStateException("Variable '$name' must be declared with 'var' before assignment.")
                    }
                    val value = evaluateExpression(block.varValue, environment)
                    environment[name] = value
                    onVariablesChanged(HashMap(environment))
                }
                BlockType.PRINTLN -> {
                    val value = evaluateExpression(block.varValue, environment)
                    onConsoleOutput(value.toString() + "\n")
                }
                BlockType.IF_ELSE -> {
                    val conditionVal = evaluateExpression(block.varValue, environment)
                    val conditionIsTrue = when (conditionVal) {
                        is Boolean -> conditionVal
                        is Number -> conditionVal.toDouble() != 0.0
                        else -> conditionVal.toString().isNotEmpty() && conditionVal.toString() != "false"
                    }

                    if (conditionIsTrue) {
                        executeBlocks(block.children, stepDelayMs)
                    } else if (block.elseChildren.isNotEmpty()) {
                        executeBlocks(block.elseChildren, stepDelayMs)
                    }
                }
                BlockType.REPEAT -> {
                    val timesVal = evaluateExpression(block.varValue, environment)
                    val times = when (timesVal) {
                        is Number -> timesVal.toInt()
                        else -> timesVal.toString().toIntOrNull() ?: 0
                    }

                    for (i in 0 until times) {
                        if (!isRunningCheck()) return
                        executeBlocks(block.children, stepDelayMs)
                    }
                }
                BlockType.WHILE_LOOP -> {
                    while (true) {
                        if (!isRunningCheck()) return
                        val conditionVal = evaluateExpression(block.varValue, environment)
                        val conditionIsTrue = when (conditionVal) {
                            is Boolean -> conditionVal
                            is Number -> conditionVal.toDouble() != 0.0
                            else -> conditionVal.toString().isNotEmpty() && conditionVal.toString() != "false"
                        }

                        if (!conditionIsTrue) break
                        executeBlocks(block.children, stepDelayMs)

                        if (totalInstructionsExecuted++ > maxInstructions) {
                            throw IllegalStateException("Infinite loop protection triggered in while loop.")
                        }
                    }
                }
            }
        }
    }

    /**
     * Sophisticated, recursive expression evaluator for visual block-programming.
     */
    fun evaluateExpression(exprStr: String, env: Map<String, Any>): Any {
        val trimmed = exprStr.trim()
        if (trimmed.isEmpty()) return ""

        // Quick literal matches
        if (trimmed == "true") return true
        if (trimmed == "false") return false
        
        // String literal in double quotes, e.g. "hello world"
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // Numeric literal
        trimmed.toIntOrNull()?.let { return it }
        trimmed.toDoubleOrNull()?.let { return it }

        // If it's a single word, check if it's a variable in scope
        val wordPattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$")
        if (wordPattern.matcher(trimmed).matches()) {
            if (env.containsKey(trimmed)) {
                return env[trimmed] ?: ""
            }
            throw IllegalStateException("Undefined variable '$trimmed'. Ensure it is declared first.")
        }

        // Parse operators in reverse order of precedence:
        // 1. Logic OR/AND: ||, &&
        // 2. Comparisons: ==, !=, <=, >=, <, >
        // 3. Add/Sub: +, -
        // 4. Mul/Div/Mod: *, /, %

        // We must check operators outside of quotes to avoid breaking strings.
        val operators = listOf(
            "||", "&&",
            "==", "!=", "<=", ">=", "<", ">",
            "+", "-",
            "*", "/", "%"
        )

        for (op in operators) {
            val index = findOperatorIndex(trimmed, op)
            if (index != -1) {
                val leftStr = trimmed.substring(0, index).trim()
                val rightStr = trimmed.substring(index + op.length).trim()

                val leftVal = evaluateExpression(leftStr, env)
                val rightVal = evaluateExpression(rightStr, env)

                return applyOperator(leftVal, op, rightVal)
            }
        }

        // If there is string-to-string conversion helper, e.g. x.toString()
        if (trimmed.endsWith(".toString()")) {
            val inner = trimmed.substring(0, trimmed.length - 11).trim()
            return evaluateExpression(inner, env).toString()
        }

        // If it's something unrecognized, return raw string
        return trimmed
    }

    /**
     * Finds the index of an operator, ignoring content inside double quotes.
     */
    private fun findOperatorIndex(str: String, op: String): Int {
        var inQuotes = false
        var i = 0
        while (i < str.length) {
            val char = str[i]
            if (char == '"') {
                inQuotes = !inQuotes
            }
            if (!inQuotes) {
                if (str.startsWith(op, i)) {
                    // Make sure it's not a partial operator match, like '=' inside '==' or '<' inside '<='
                    if (op == "=" && str.startsWith("==", i)) {
                        i += 2
                        continue
                    }
                    if (op == "<" && str.startsWith("<=", i)) {
                        i += 2
                        continue
                    }
                    if (op == ">" && str.startsWith(">=", i)) {
                        i += 2
                        continue
                    }
                    return i
                }
            }
            i++
        }
        return -1
    }

    private fun applyOperator(left: Any, op: String, right: Any): Any {
        // Handle logical operations
        if (op == "&&") {
            return toBool(left) && toBool(right)
        }
        if (op == "||") {
            return toBool(left) || toBool(right)
        }

        // Handle string concatenation first if op is "+" and either operand is string
        if (op == "+" && (left is String || right is String)) {
            return left.toString() + right.toString()
        }

        // Try numeric conversions
        val leftNum = toDouble(left)
        val rightNum = toDouble(right)

        if (leftNum != null && rightNum != null) {
            return when (op) {
                "+" -> if (left is Int && right is Int) left + right else leftNum + rightNum
                "-" -> if (left is Int && right is Int) left - right else leftNum - rightNum
                "*" -> if (left is Int && right is Int) left * right else leftNum * rightNum
                "/" -> {
                    if (rightNum == 0.0) throw IllegalStateException("Division by zero error.")
                    if (left is Int && right is Int) left / right else leftNum / rightNum
                }
                "%" -> {
                    if (rightNum == 0.0) throw IllegalStateException("Division by zero (modulo) error.")
                    if (left is Int && right is Int) left % right else leftNum % rightNum
                }
                "==" -> leftNum == rightNum
                "!=" -> leftNum != rightNum
                "<" -> leftNum < rightNum
                ">" -> leftNum > rightNum
                "<=" -> leftNum <= rightNum
                ">=" -> leftNum >= rightNum
                else -> throw IllegalStateException("Unknown numeric operator: '$op'")
            }
        }

        // Fallback generic object comparisons
        return when (op) {
            "==" -> left == right
            "!=" -> left != right
            else -> throw IllegalStateException("Cannot apply operator '$op' between type ${left.javaClass.simpleName} and ${right.javaClass.simpleName}.")
        }
    }

    private fun toBool(value: Any): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toDouble() != 0.0
            else -> value.toString().lowercase() == "true"
        }
    }

    private fun toDouble(value: Any): Double? {
        if (value is Number) return value.toDouble()
        return value.toString().toDoubleOrNull()
    }
}
