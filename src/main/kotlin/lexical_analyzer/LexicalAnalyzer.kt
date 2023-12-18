package org.example.lexical_analyzer

import org.example.isRomanDigit

class LexicalAnalyzer {

    private enum class State {
        START,
        IDENTIFIER,
        NUMBER,
        ASSIGN,
        COMMENT,
//        PARENTHESIS,
    }

    private var currentState = State.START
    private var lexemeBuffer: String = ""
    private val results = mutableListOf<AnalyzerResult>()

    fun analyze(text: String): List<AnalyzerResult> {
        text.forEachIndexed { index, char ->
            when (currentState) {
                State.START -> {
                    clearBuffer()
                    when {
                        char in listOf(' ', '\n', '\t', '\r') -> {}

                        char.isRomanDigit() -> {
                            addToBuffer(char)
                            currentState = State.NUMBER
                        }

                        char.isLetter() -> {
                            addToBuffer(char)
                            currentState = State.IDENTIFIER
                        }


                        char == '{' -> {
                            if (text.substring(startIndex = index + 1).substringBefore('{').contains('}')) {
                                currentState = State.COMMENT
                            } else {
                                reportError(index, "Незакрытый комментарий!")
                            }
                        }

                        char == '(' || char == ')' -> {
                            addToBuffer(char)
                            addLexeme(index, lexemeBuffer, LexemeType.PARENTHESIS)
                            currentState = State.START
                        }

                        char == ':' -> {
                            addToBuffer(char)
                            currentState = State.ASSIGN
                        }

                        else -> {
                            addToBuffer(char)
                            addLexeme(
                                index,
                                lexemeBuffer,
                                KNOWN_LEXEMES.find { it.value == lexemeBuffer }?.type ?: LexemeType.DELIMITER
                            )
                            currentState = State.START
                        }
                    }
                }

                State.IDENTIFIER -> {
                    if (char.isLetterOrDigit()) {
                        addToBuffer(char)
                    } else {
                        addLexeme(
                            index = index,
                            value = lexemeBuffer,
                            type = KNOWN_LEXEMES.find { it.value == lexemeBuffer }?.type ?: LexemeType.IDENTIFIER
                        )
                        if (!char.isWhitespace()) {
                            addLexeme(
                                index = index,
                                value = char.toString(),
                                type = KNOWN_LEXEMES.find { it.value == char.toString() }?.type ?: LexemeType.DELIMITER
                            )
                        }
                        currentState = State.START
                    }
                }

                State.NUMBER -> {
                    if (char.isRomanDigit()) {
                        addToBuffer(char)
                    } else {
                        addLexeme(index = index, value = lexemeBuffer.removeSuffix("."), type = LexemeType.CONSTANT)
                        if (!char.isWhitespace()) {
                            addLexeme(
                                index = index,
                                value = char.toString(),
                                type = KNOWN_LEXEMES.find { it.value == char.toString() }?.type ?: LexemeType.DELIMITER
                            )
                        }
                        currentState = State.START
                    }
                }

                State.ASSIGN -> {
                    if (char == '=') {
                        addToBuffer(char)
                        addLexeme(index, lexemeBuffer, LexemeType.ASSIGN_SIGN)
                    } else {
                        addLexeme(index, lexemeBuffer, LexemeType.DELIMITER)
                        addLexeme(index, char.toString(), LexemeType.DELIMITER)
                    }
                    currentState = State.START
                }

                State.COMMENT -> {
                    if (char == '}')
                        currentState = State.START
                }


            }
        }
        // print("Исходный код: \n\n\n $text \n\n\n")
        // results.map(::checkResult).forEach(::println)
        return results.map(::checkResult)
    }

    private fun checkResult(result: AnalyzerResult): AnalyzerResult {
        val lexeme = result as? Lexeme ?: return result
        return if (lexeme.type == LexemeType.DELIMITER && KNOWN_LEXEMES.none { it.value == lexeme.value }) {
            Error(
                position = lexeme.position ?: 0,
                value = "Неизвестная лексема вида ${lexeme.type.asString} - ${lexeme.value}"
            )
        } else lexeme
    }

    private fun clearBuffer() {
        lexemeBuffer = ""
    }

    private fun addToBuffer(char: Char) {
        lexemeBuffer += char
    }

    private fun addLexeme(index: Int, value: String, type: LexemeType) {
        results.add(Lexeme(index, value, type))
    }

    private fun reportError(position: Int, value: String) {
        results.add(Error(position, value))
    }

    companion object {
        val KNOWN_LEXEMES = listOf(
            Lexeme(value = "if", type = LexemeType.CONDITIONAL_OPERATOR),
            Lexeme(value = "then", type = LexemeType.CONDITIONAL_OPERATOR),
            Lexeme(value = "else", type = LexemeType.CONDITIONAL_OPERATOR),
            Lexeme(value = ";", type = LexemeType.DELIMITER),
            Lexeme(value = "+", type = LexemeType.OPERATORS_SIGN),
            Lexeme(value = "-", type = LexemeType.OPERATORS_SIGN),
            Lexeme(value = "*", type = LexemeType.OPERATORS_SIGN),
            Lexeme(value = "/", type = LexemeType.OPERATORS_SIGN),
            Lexeme(value = "<", type = LexemeType.COMPARISON_SIGN),
            Lexeme(value = ">", type = LexemeType.COMPARISON_SIGN),
            Lexeme(value = "=", type = LexemeType.COMPARISON_SIGN),
            Lexeme(value = "(", type = LexemeType.PARENTHESIS),
            Lexeme(value = ")", type = LexemeType.PARENTHESIS),
            Lexeme(value = ":=", type = LexemeType.ASSIGN_SIGN),
        )
    }
}