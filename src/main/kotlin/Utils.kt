package org.example

import java.io.InputStream
import kotlin.math.absoluteValue
import kotlin.math.pow

fun getStreamFromResources(fileName: String): InputStream? = object {}.javaClass.getResourceAsStream(fileName)
fun readLinesFromInputStream(inputStream: InputStream?) = inputStream?.bufferedReader()?.readLines()

fun String.hash(): Int = foldIndexed(0) { index, acc, char ->
    acc + char.code * 32f.pow(length - index - 1).toInt()
}.absoluteValue

fun Char.isRomanDigit(): Boolean {
    return when (this) {
        'I', 'V', 'X', 'L', 'C', 'D', 'M' -> true
        else -> false
    }
}

fun areParenthesesBalanced(input: String): Boolean {
    var balance = 0

    for (char in input) {
        when (char) {
            '(' -> balance++
            ')' -> balance--
        }

        // Если баланс стал отрицательным, то скобки не сбалансированы
        if (balance < 0) {
            return false
        }
    }

    // Проверка, что после обхода строки баланс равен нулю
    return balance == 0
}