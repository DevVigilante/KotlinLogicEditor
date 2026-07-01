package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class BlockType {
    VAR_DECL,      // var name = value
    VAR_ASSIGN,    // name = value
    PRINTLN,       // println(expression)
    IF_ELSE,       // if (condition) { body } else { elseBody }
    REPEAT,        // repeat(times) { body }
    WHILE_LOOP,    // while (condition) { body }
    COMMENT        // // comment text
}

data class Block(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType,
    val varName: String = "",
    val varValue: String = "",     // Represents the literal value, expression, loop count, or condition
    val operand1: String = "",
    val operator: String = "",     // e.g. "+", "-", "==", "<", ">"
    val operand2: String = "",
    val children: List<Block> = emptyList(),
    val elseChildren: List<Block> = emptyList(),
    val commentText: String = ""
) {

    /**
     * Translates this single block (and any nested blocks) into Kotlin code.
     */
    fun toKotlinCode(indentLevel: Int = 0): String {
        val indent = "    ".repeat(indentLevel)
        return when (type) {
            BlockType.COMMENT -> {
                "$indent// ${commentText.ifEmpty { "comment" }}"
            }
            BlockType.VAR_DECL -> {
                val cleanVal = varValue.ifEmpty { "0" }
                "${indent}var $varName = $cleanVal"
            }
            BlockType.VAR_ASSIGN -> {
                val cleanVal = varValue.ifEmpty { "0" }
                "$indent$varName = $cleanVal"
            }
            BlockType.PRINTLN -> {
                val cleanVal = varValue.ifEmpty { "\"\"" }
                "${indent}println($cleanVal)"
            }
            BlockType.IF_ELSE -> {
                val condition = varValue.ifEmpty { "true" }
                val sb = StringBuilder()
                sb.append("${indent}if ($condition) {\n")
                if (children.isEmpty()) {
                    sb.append("$indent    todo()\n")
                } else {
                    children.forEach { sb.append(it.toKotlinCode(indentLevel + 1)).append("\n") }
                }
                sb.append("$indent}")
                if (elseChildren.isNotEmpty()) {
                    sb.append(" else {\n")
                    elseChildren.forEach { sb.append(it.toKotlinCode(indentLevel + 1)).append("\n") }
                    sb.append("$indent}")
                }
                sb.toString()
            }
            BlockType.REPEAT -> {
                val times = varValue.ifEmpty { "1" }
                val sb = StringBuilder()
                sb.append("${indent}repeat($times) {\n")
                if (children.isEmpty()) {
                    sb.append("$indent    todo()\n")
                } else {
                    children.forEach { sb.append(it.toKotlinCode(indentLevel + 1)).append("\n") }
                }
                sb.append("$indent}")
                sb.toString()
            }
            BlockType.WHILE_LOOP -> {
                val condition = varValue.ifEmpty { "true" }
                val sb = StringBuilder()
                sb.append("${indent}while ($condition) {\n")
                if (children.isEmpty()) {
                    sb.append("$indent    todo()\n")
                } else {
                    children.forEach { sb.append(it.toKotlinCode(indentLevel + 1)).append("\n") }
                }
                sb.append("$indent}")
                sb.toString()
            }
        }
    }

    companion object {
        // Serialization helpers
        fun toJson(blocks: List<Block>): String {
            val array = JSONArray()
            blocks.forEach { array.put(blockToMap(it)) }
            return array.toString()
        }

        fun fromJson(jsonStr: String): List<Block> {
            if (jsonStr.isEmpty()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<Block>()
                for (i in 0 until array.length()) {
                    list.add(mapToBlock(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun blockToMap(block: Block): JSONObject {
            val json = JSONObject()
            json.put("id", block.id)
            json.put("type", block.type.name)
            json.put("varName", block.varName)
            json.put("varValue", block.varValue)
            json.put("operand1", block.operand1)
            json.put("operator", block.operator)
            json.put("operand2", block.operand2)
            json.put("commentText", block.commentText)

            val childrenArr = JSONArray()
            block.children.forEach { childrenArr.put(blockToMap(it)) }
            json.put("children", childrenArr)

            val elseArr = JSONArray()
            block.elseChildren.forEach { elseArr.put(blockToMap(it)) }
            json.put("elseChildren", elseArr)

            return json
        }

        private fun mapToBlock(json: JSONObject): Block {
            val id = json.optString("id", UUID.randomUUID().toString())
            val type = BlockType.valueOf(json.optString("type", BlockType.COMMENT.name))
            val varName = json.optString("varName", "")
            val varValue = json.optString("varValue", "")
            val operand1 = json.optString("operand1", "")
            val operator = json.optString("operator", "")
            val operand2 = json.optString("operand2", "")
            val commentText = json.optString("commentText", "")

            val children = mutableListOf<Block>()
            val childrenArr = json.optJSONArray("children")
            if (childrenArr != null) {
                for (i in 0 until childrenArr.length()) {
                    children.add(mapToBlock(childrenArr.getJSONObject(i)))
                }
            }

            val elseChildren = mutableListOf<Block>()
            val elseArr = json.optJSONArray("elseChildren")
            if (elseArr != null) {
                for (i in 0 until elseArr.length()) {
                    elseChildren.add(mapToBlock(elseArr.getJSONObject(i)))
                }
            }

            return Block(
                id = id,
                type = type,
                varName = varName,
                varValue = varValue,
                operand1 = operand1,
                operator = operator,
                operand2 = operand2,
                children = children,
                elseChildren = elseChildren,
                commentText = commentText
            )
        }

        /**
         * Helper to create a clean template program for Fibonacci Sequence.
         */
        fun getFibonacciTemplate(): List<Block> {
            return listOf(
                Block(type = BlockType.COMMENT, commentText = "Fibonacci Sequence Challenge"),
                Block(type = BlockType.VAR_DECL, varName = "n", varValue = "10"),
                Block(type = BlockType.VAR_DECL, varName = "t1", varValue = "0"),
                Block(type = BlockType.VAR_DECL, varName = "t2", varValue = "1"),
                Block(type = BlockType.PRINTLN, varValue = "\"Fibonacci Series:\""),
                Block(
                    type = BlockType.REPEAT,
                    varValue = "n",
                    children = listOf(
                        Block(type = BlockType.PRINTLN, varValue = "t1"),
                        Block(type = BlockType.VAR_DECL, varName = "sum", varValue = "t1 + t2"),
                        Block(type = BlockType.VAR_ASSIGN, varName = "t1", varValue = "t2"),
                        Block(type = BlockType.VAR_ASSIGN, varName = "t2", varValue = "sum")
                    )
                )
            )
        }

        /**
         * Helper to create a clean template program for FizzBuzz.
         */
        fun getFizzBuzzTemplate(): List<Block> {
            return listOf(
                Block(type = BlockType.COMMENT, commentText = "FizzBuzz Loop Challenge"),
                Block(type = BlockType.VAR_DECL, varName = "i", varValue = "1"),
                Block(
                    type = BlockType.WHILE_LOOP,
                    varValue = "i <= 20",
                    children = listOf(
                        Block(
                            type = BlockType.IF_ELSE,
                            varValue = "i % 15 == 0",
                            children = listOf(
                                Block(type = BlockType.PRINTLN, varValue = "\"FizzBuzz\"")
                            ),
                            elseChildren = listOf(
                                Block(
                                    type = BlockType.IF_ELSE,
                                    varValue = "i % 3 == 0",
                                    children = listOf(
                                        Block(type = BlockType.PRINTLN, varValue = "\"Fizz\"")
                                    ),
                                    elseChildren = listOf(
                                        Block(
                                            type = BlockType.IF_ELSE,
                                            varValue = "i % 5 == 0",
                                            children = listOf(
                                                Block(type = BlockType.PRINTLN, varValue = "\"Value is: Buzz\"")
                                            ),
                                            elseChildren = listOf(
                                                Block(type = BlockType.PRINTLN, varValue = "i")
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        Block(type = BlockType.VAR_ASSIGN, varName = "i", varValue = "i + 1")
                    )
                )
            )
        }

        /**
         * Helper to create a clean template program for Even/Odd checker.
         */
        fun getEvenOddTemplate(): List<Block> {
            return listOf(
                Block(type = BlockType.COMMENT, commentText = "Check if number is Even or Odd"),
                Block(type = BlockType.VAR_DECL, varName = "num", varValue = "7"),
                Block(
                    type = BlockType.IF_ELSE,
                    varValue = "num % 2 == 0",
                    children = listOf(
                        Block(type = BlockType.PRINTLN, varValue = "num.toString() + \" is EVEN\"")
                    ),
                    elseChildren = listOf(
                        Block(type = BlockType.PRINTLN, varValue = "num.toString() + \" is ODD\"")
                    )
                )
            )
        }
    }
}
