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

            var delimiterIndex = lexemeList.indexOfFirst { it.type == LexemeType.DELIMITER } + 1

            if (delimiterIndex == -1)
                reportError(value = "${lexemeList.first().position}: начатое выражение не заканчивается разделителем <;>")
            if (delimiterIndex == 0 ) delimiterIndex = lexemeList.size

            val lexemeListBeforeDelimiter: List<Lexeme> = lexemeList.subList(0, delimiterIndex)
            lexemeList =
                when (lexemeListBeforeDelimiter.assumeNodeType()) {
                    Node.Type.CONDITIONAL -> {
                        // if ... then ... else ...
                        // Ищём законченную условную конструкцию и анализируем всё внутри
                        val conditionalLexemes = findConditionalBlock(lexemeList)
                        startNode.addNode(analyzeConditional(conditionalLexemes))
                        lexemeList.drop(conditionalLexemes.lastIndex + 1)
                    }

                    Node.Type.ASSIGN -> {
                        // :=
                        // Обрабатывам присвоение
                        startNode.addNode(analyzeAssign(lexemeListBeforeDelimiter))
                        lexemeList.drop(delimiterIndex + 1)
                    }

                    Node.Type.PARENTHESIS -> {
                        // ( ... )
                        val lexemes = findParenthesisBlock(lexemeList)
                        val nodes = analyzeParenthesis(lexemes)
                        startNode.addNode(nodes[0])
                        startNode.addNode(nodes[1])
                        startNode.addNode(nodes[2])
                        lexemeList.drop(delimiterIndex + 1)
                    }
                    Node.Type.OPERATOR -> {
                        // a + a, a * III, III / III, ...
                        // Ищём операцию
                        val nodes = analyzeOperation(lexemeList)
                        startNode.addNode(nodes[0])
                        startNode.addNode(nodes[1])
                        startNode.addNode(nodes[2])
                        lexemeList.drop(lexemeList.size)
                    }

                    Node.Type.CONSTANT_OR_NUMBER -> TODO()
                    null -> TODO()
                }
        }
        return startNode
    }

    private fun findConditionalBlock(lexemes: List<Lexeme>): List<Lexeme> {
        val lastElse = lexemes.findLast { it.value == "else" }
        return if (lastElse?.position != null) {
            lexemes.subList(
                fromIndex = 0,
                toIndex = lexemes.indexOfFirst { lexeme -> lexeme.type == LexemeType.DELIMITER && lexeme.position !== null && lexeme.position > lastElse.position } + 1
            )
        } else {
            lexemes.subList(
                fromIndex = 0,
                toIndex = lexemes.indexOfFirst { lexeme -> lexeme.type == LexemeType.DELIMITER } + 1
            )
        }
    }

    private fun findOperationBlock(lexemes: List<Lexeme>): List<Lexeme> {
        val lastElse = lexemes.findLast { it.value == "else" }
        return if (lastElse?.position != null) {
            lexemes.subList(
                fromIndex = 0,
                toIndex = lexemes.indexOfFirst { lexeme -> lexeme.type == LexemeType.DELIMITER && lexeme.position !== null && lexeme.position > lastElse.position } + 1
            )
        } else {
            lexemes.subList(
                fromIndex = 0,
                toIndex = lexemes.indexOfFirst { lexeme -> lexeme.type == LexemeType.DELIMITER } + 1
            )
        }
    }

    private fun findParenthesisBlock(lexemes: List<Lexeme>): List<Lexeme> {
        val lastCloseParenthesis = lexemes.find { it.value == ")" }
        val lastLexemeIndex =
            if (lastCloseParenthesis?.position == null)
                lexemes.indexOfFirst { lexeme -> lexeme.type == LexemeType.DELIMITER } + 1
            else
                lexemes.indexOfFirst { lexeme -> lexeme.type == LexemeType.DELIMITER && lexeme.position !== null && lexeme.position > lastCloseParenthesis.position } + 1

        return lexemes.subList(0, lastLexemeIndex)
    }

    private fun analyzeAssign(lexemes: List<Lexeme>): Node {
        val firstLexeme = lexemes.first() // a
        if (firstLexeme.type != LexemeType.IDENTIFIER)
            reportError(value = "${firstLexeme.position}: выражение должно начинаться с идентификатора")

        val secondLexeme = lexemes[1] // :=
        if (secondLexeme.type != LexemeType.ASSIGN_SIGN)
            reportError(value = "${secondLexeme.position}: вместо ${secondLexeme.value} ожидалась операция присваивания")

        val lastLexemes = lexemes.subList(2, lexemes.size)      // a | a + III | (a) | a + (a)
        val thirdLexeme = lastLexemes.first()                   // a | ( | V
        if (thirdLexeme.type != LexemeType.IDENTIFIER && thirdLexeme.type != LexemeType.CONSTANT && thirdLexeme.type != LexemeType.PARENTHESIS)
            reportError(value = "${thirdLexeme.position}: переменной может присваиваться только другая переменная или константа")

        val firstNode = Node(children = mutableListOf(Node(lexeme = firstLexeme)))
        val secondNode = Node(lexeme = secondLexeme)

        // Нода №3 может быть
        // 1. Константой или пересенной
        // 2. Операцией
        // 3. Скобкой
        val thirdNode = when (lastLexemes.assumeNodeType()) {
            Node.Type.CONSTANT_OR_NUMBER -> Node(children = mutableListOf(Node(lexeme = thirdLexeme)))
            Node.Type.OPERATOR -> analyzeSyntax(lastLexemes, Node())
            Node.Type.PARENTHESIS -> analyzeSyntax(lastLexemes, Node())
            Node.Type.ASSIGN -> TODO()
            Node.Type.CONDITIONAL -> TODO()
            null -> Node(children = mutableListOf(Node(lexeme = thirdLexeme)))
        }
        return Node(children = mutableListOf(firstNode, secondNode, thirdNode))
    }

    // TODO
    private fun analyzeParenthesis(lexemes: List<Lexeme>): MutableList<Node> {

        // Проверяем наличие открывающейся скобки
        val firstLexeme = lexemes.first()
        if (firstLexeme.value != "(")
            reportError(value = "${firstLexeme.position}: конструкция должна начинаться с '(' ")

        // Проверяем наличие закрывабщейся скобки
        if (lexemes.none { it.value == ")" })
            reportError(value = "${firstLexeme.position}: за '(' должно следовать ключевое слово ')' ")

        val middleLexemes = lexemes.subList(1, lexemes.indexOfFirst { it.value == ")" })
        val secondLexeme = middleLexemes.first()
        val middleNode = when (middleLexemes.assumeNodeType()) {
            Node.Type.CONSTANT_OR_NUMBER -> Node(children = mutableListOf(Node(lexeme = secondLexeme)))
            Node.Type.OPERATOR -> analyzeSyntax(middleLexemes, Node())
            Node.Type.PARENTHESIS -> TODO()
            Node.Type.ASSIGN -> TODO()
            Node.Type.CONDITIONAL -> TODO()
            null -> Node(children = mutableListOf(Node(lexeme = secondLexeme)))
        }

        val lastLexeme = lexemes.last { it.value == ")" }


        val firstNode = Node(lexeme = firstLexeme)
        val lastNode = Node(lexeme = lastLexeme)

        return mutableListOf(firstNode, middleNode, lastNode)
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

        // add "if condition ..."
        conditionalNode.addNode(analyzeComparison(lexemes.subList(1, lexemes.indexOfFirst { it.value == "then" })))
        // add "then"
        conditionalNode.addNode(Node(lexeme = lexemes.find { it.value == "then" }))

        // Есть "else"
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
        }
        // Нету "else"
        else {
            val conditionalBlock = lexemes.subList(
                fromIndex = lexemes.indexOfFirst { it.value == "then" } + 1,
                toIndex = lexemes.lastIndex + 1
            )
            analyzeSyntax(conditionalBlock, conditionalNode)
        }
        return conditionalNode
    }

    // Анализ a < a, a = III, III > VVV
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

    /* Анализ a + a, a * III, III / III, ... */
    private fun analyzeOperation(lexemes: List<Lexeme>): MutableList<Node> {
        val firstLexeme = lexemes.first()
        if (firstLexeme.type != LexemeType.IDENTIFIER && firstLexeme.type != LexemeType.CONSTANT && firstLexeme.type != LexemeType.PARENTHESIS)
            reportError(value = "${firstLexeme.position}: операции производятся над идентификаторами или константами или выражением в скобках")

        val secondLexeme = lexemes[1]
        if (secondLexeme.type != LexemeType.OPERATORS_SIGN)
            reportError(value = "${secondLexeme.position}: вместо ${secondLexeme.value} ожидался знак операции")


        val lastLexemes = lexemes.subList(2, lexemes.size)      // a | a + III | (a) | a + (a)
        val thirdLexeme = lastLexemes.first()
        if (thirdLexeme.type != LexemeType.IDENTIFIER && thirdLexeme.type != LexemeType.CONSTANT && thirdLexeme.type != LexemeType.PARENTHESIS)
            reportError(value = "${thirdLexeme.position}: операции производятся над идентификаторами или константами или выражением в скобках")

        val firstNode = Node(children = mutableListOf(Node(lexeme = firstLexeme)))
        val secondNode = Node(lexeme = secondLexeme)

        val thirdNode = when (lastLexemes.assumeNodeType()) {
            Node.Type.CONSTANT_OR_NUMBER -> Node(children = mutableListOf(Node(lexeme = thirdLexeme)))
            Node.Type.OPERATOR -> analyzeSyntax(lastLexemes, Node())
            Node.Type.PARENTHESIS -> TODO()
            Node.Type.ASSIGN -> TODO()
            Node.Type.CONDITIONAL -> TODO()
            null -> Node(children = mutableListOf(Node(lexeme = thirdLexeme)))
        }

        return mutableListOf(firstNode, secondNode, thirdNode)
    }

    private fun reportError(
        prefix: String = "Ошибка на позиции ",
        value: String
    ) {
        throw UnsupportedOperationException(prefix + value)
    }
}