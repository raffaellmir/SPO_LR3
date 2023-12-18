package org.example.syntax_analyzer

import org.example.lexical_analyzer.Lexeme
import org.example.lexical_analyzer.LexemeType


data class Node(
    val lexeme: Lexeme? = null,
    val children: MutableList<Node> = mutableListOf()
) {
    fun addNode(node: Node) {
        children.add(node)
    }

    enum class Type {
        ASSIGN,
        PARENTHESIS,
        OPERATOR,
        CONSTANT_OR_NUMBER,
        CONDITIONAL
    }
}

fun List<Lexeme>.assumeNodeType(): Node.Type? {
    return if (first().value == "(")
        Node.Type.PARENTHESIS
    else if (first().type == LexemeType.IDENTIFIER && get(1).type == LexemeType.ASSIGN_SIGN)
        Node.Type.ASSIGN
    else if (first().type == LexemeType.CONDITIONAL_OPERATOR)
        Node.Type.CONDITIONAL
    else if (any { it.value == "+" || it.value == "-" || it.value == "*" || it.value == "/" })
        Node.Type.OPERATOR
    else if (size == 1 && first().type == LexemeType.CONSTANT || first().type == LexemeType.IDENTIFIER)
        Node.Type.CONSTANT_OR_NUMBER
    else
        null
}