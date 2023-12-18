package org.example.lexical_analyzer

sealed interface AnalyzerResult
data class Lexeme(
    val position: Int? = null,
    val value: String,
    val type: LexemeType
): AnalyzerResult {
    override fun toString(): String {
        return "$value - ${type.asString}"
    }
}

enum class LexemeType(val asString: String) {
    CONDITIONAL_OPERATOR("Оператор условия"), // [if, else, then]
    DELIMITER("Разделитель"), // ;
    IDENTIFIER("Идентификатор"),
    COMPARISON_SIGN("Знак сравнения"),
    OPERATORS_SIGN("Знак операции"), // [+, -, *, /]
    PARENTHESIS("Скобка"), // [(, )]
    CONSTANT("Константа"),
    ASSIGN_SIGN("Знак присваивания")
}

data class Error(
    val position: Int,
    val value: String
): AnalyzerResult {
    override fun toString(): String {
        return "Ошибка на позиции $position: $value"
    }
}
