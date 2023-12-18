package org.example.syntax_analyzer

import org.example.lexical_analyzer.AnalyzerResult
import org.example.lexical_analyzer.Error
import org.example.lexical_analyzer.Lexeme
import org.example.lexical_analyzer.LexemeType

class SyntacticalAnalyzer {

    fun analyze(source: List<AnalyzerResult>): Node {
        val errors = source.filterIsInstance<Error>()
        if (errors.isNotEmpty()) {
            reportError(
                prefix = "На этапе лексического анализа были найдены ошибки:\n",
                value = errors.joinToString("\n") { it.toString() }
            )
        }
        return analyzeSyntax(source.filterIsInstance<Lexeme>(), Node())
    }

    private fun analyzeSyntax(sourceLexemes: List<Lexeme>, startNode: Node): Node {
        var lexemeList = sourceLexemes
        while (lexemeList.isNotEmpty()) {

            val delimiterIndex = lexemeList.indexOfFirst { it.type == LexemeType.DELIMITER }

            if (delimiterIndex == -1)
                reportError(value = "${lexemeList.first().position}: начатое выражение не заканчивается разделителем <;>")

            val lexemeListBeforeDelimiter = lexemeList.subList(0, delimiterIndex)
            lexemeList =
                if (lexemeListBeforeDelimiter.none { it.type == LexemeType.CONDITIONAL_OPERATOR }) {
                    // Если работаем не с условной конструкцией в коде
                    startNode.addNode(analyzeExpression(lexemeListBeforeDelimiter))
                    lexemeList.drop(delimiterIndex + 1)
                } else {
                    // Ищём законченную условную конструкцию и анализируем всё внутри
                    val conditionalLexemes = locateConditionalBlock(lexemeList)
                    startNode.addNode(analyzeConditional(conditionalLexemes))
                    lexemeList.drop(conditionalLexemes.lastIndex + 1)
                }
        }
        return startNode
    }

    private fun locateConditionalBlock(lexemes: List<Lexeme>): List<Lexeme> {
        val lastElse = lexemes.findLast { it.value == "else" }
        if (lastElse?.position != null) {
            return lexemes.subList(
                fromIndex = 0,
                toIndex = lexemes.indexOfFirst { lexeme ->
                    lexeme.type == LexemeType.DELIMITER
                            &&
                    lexeme.position !== null
                            &&
                    lexeme.position > lastElse.position
                } + 1
            )
        } else {
            return lexemes.subList(
                fromIndex = 0,
                toIndex = lexemes.indexOfFirst { lexeme -> lexeme.type == LexemeType.DELIMITER } + 1
            )
        }
    }

    private fun locateParenthesisBlock(lexemes: List<Lexeme>): List<Lexeme> {
        val lastElse = lexemes.findLast { it.value == ")" }
        if (lastElse?.position != null) {
            return lexemes.subList(
                fromIndex = 0,
                toIndex = lexemes.indexOfFirst { lexeme ->
                    lexeme.type == LexemeType.DELIMITER
                            &&
                            lexeme.position !== null
                            &&
                            lexeme.position > lastElse.position
                } + 1
            )
        } else {
            return lexemes.subList(
                fromIndex = 0,
                toIndex = lexemes.indexOfFirst { lexeme -> lexeme.type == LexemeType.DELIMITER } + 1
            )
        }
    }

    private fun analyzeExpression(lexemes: List<Lexeme>): Node {
        if (lexemes.size < 3) {
            reportError(value = "${lexemes.first().position}: незаконченное выражение")
        }
        if (lexemes.first().type != LexemeType.IDENTIFIER) {
            reportError(value = "${lexemes.first().position}: выражение должно начинаться с идентификатора")
        }
        val secondLexeme = lexemes[1]
        if (secondLexeme.type != LexemeType.ASSIGN_SIGN) {
            reportError(value = "${secondLexeme.position}: вместо ${secondLexeme.value} ожидалась операция присваивания")
        }
        val thirdLexeme = lexemes[2]
        if (thirdLexeme.type != LexemeType.IDENTIFIER && thirdLexeme.type != LexemeType.CONSTANT && thirdLexeme.type != LexemeType.PARENTHESIS) {
            reportError(value = "${thirdLexeme.position}: переменной может присваиваться только другая переменная или константа")
        }
        if (lexemes.size > 3) {
            reportError(value = "${lexemes.last().position}: превышено максимальное количество лексем в выражении присваивания")
        }
        val firstNode = Node(
            children = mutableListOf(Node(lexeme = lexemes.first()))
        )
        val secondNode = Node(
            lexeme = secondLexeme
        )
        val thirdNode = Node(
            children = mutableListOf(Node(lexeme = thirdLexeme))
        )
        return Node(children = mutableListOf(firstNode, secondNode, thirdNode))
    }

    /* Анализ if ... then ... else ... */
    private fun analyzeConditional(lexemes: List<Lexeme>): Node {
        val conditionalNode = Node()

        if (lexemes.first().value != "if")
            reportError(value = "${lexemes.first().position}: условная конструкция должна начинаться с if")

        // add "if"
        conditionalNode.addNode(Node(lexeme = lexemes.first()))

        if (lexemes.none { it.value == "then" })
            reportError(value = "${lexemes.first().position}: за предикатом должно следовать ключевое слово then")

        // add "if condition"
        conditionalNode.addNode(analyzeComparison(lexemes.subList(1, lexemes.indexOfFirst { it.value == "then" })))
        // add "then"
        conditionalNode.addNode(Node(lexeme = lexemes.find { it.value == "then" }))

        if (lexemes.any { it.value == "else" }) {
            val conditionalBlock = lexemes.subList(
                fromIndex = lexemes.indexOfFirst { it.value == "then" } + 1,
                toIndex = lexemes.indexOfLast { it.value == "else" }
            )
            val innerConditionalNode = Node()
            conditionalNode.addNode(analyzeSyntax(conditionalBlock, innerConditionalNode))
            conditionalNode.addNode(Node(lexeme = lexemes.find { it.value == "else" }))
            val outerElseBlock = lexemes.subList(
                fromIndex = lexemes.indexOfLast { it.value == "else" } + 1,
                toIndex = lexemes.lastIndex + 1
            )
            analyzeSyntax(outerElseBlock, conditionalNode)
        } else {
            val conditionalBlock = lexemes.subList(
                fromIndex = lexemes.indexOfFirst { it.value == "then" } + 1,
                toIndex = lexemes.lastIndex + 1
            )
            analyzeSyntax(conditionalBlock, conditionalNode)
        }
        return conditionalNode
    }

    private fun analyzeComparison(lexemes: List<Lexeme>): Node {
        if (lexemes.size != 3) {
            reportError(value = "${lexemes.first().position}: некорректная форма конструкции сравнения двух элементов")
        }
        if (lexemes.first().type != LexemeType.IDENTIFIER && lexemes.first().type != LexemeType.CONSTANT) {
            reportError(value = "${lexemes.first().position}: сравниваться должны идентификаторы или константы")
        }
        val secondLexeme = lexemes[1]
        if (secondLexeme.type != LexemeType.COMPARISON_SIGN) {
            reportError(value = "${secondLexeme.position}: вместо ${secondLexeme.value} ожидался знак сравнения")
        }
        val thirdLexeme = lexemes[2]
        if (thirdLexeme.type != LexemeType.IDENTIFIER && thirdLexeme.type != LexemeType.CONSTANT) {
            reportError(value = "${thirdLexeme.position}: сравниваться должны идентификаторы или константы")
        }
        val firstNode = Node(
            children = mutableListOf(Node(lexeme = lexemes.first()))
        )
        val secondNode = Node(
            lexeme = secondLexeme
        )
        val thirdNode = Node(
            children = mutableListOf(Node(lexeme = thirdLexeme))
        )
        return Node(children = mutableListOf(firstNode, secondNode, thirdNode))
    }

    private fun reportError(
        prefix: String = "Ошибка на позиции ",
        value: String
    ) {
        throw UnsupportedOperationException(prefix + value)
    }
}