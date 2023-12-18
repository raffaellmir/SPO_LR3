package org.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.bonsai.core.Bonsai
import cafe.adriel.bonsai.core.BonsaiStyle
import cafe.adriel.bonsai.core.node.Branch
import cafe.adriel.bonsai.core.node.Leaf
import cafe.adriel.bonsai.core.tree.Tree
import cafe.adriel.bonsai.core.tree.TreeScope
import org.example.lexical_analyzer.AnalyzerResult
import org.example.lexical_analyzer.LexicalAnalyzer
import org.example.syntax_analyzer.SyntacticalAnalyzer
import org.example.syntax_analyzer.Node
import org.example.tables.BinaryTreeTable
import tables.SimpleRehashTable
import kotlin.time.measureTime


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ЛР3",
        state = rememberWindowState(width = 1000.dp, height = 1000.dp)
    ) {
        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize())
            { testAnalyzer() }
        }
    }
}


/*

a := III;
a := III + b;
b := (III);
c := (a * ( III + b ) );
c := (a * b) + III;

if c > d
    then c := III;
    else c := VVV;

*/
@Composable
fun testAnalyzer() {
    val sourceCode = """
       
       a := a * b / c + III;
        
    """.trimIndent()
    val lexicalResults = analyzeLexemes(sourceCode)
    analyzeSyntax(lexicalResults)
        .onSuccess { drawNode(node = it) }
        .onFailure { println(it.printStackTrace()) }
}

@Composable
fun TreeScope.drawBranch(node: Node) {
    if (node.children.isEmpty()) {
        Leaf(node.lexeme?.value ?: "E")
    } else {
        Branch(node.lexeme ?: "E") {
            node.children.forEach { drawBranch(it) }
        }
    }
}

@Composable
fun drawNode(node: Node) {
    val tree = Tree<String> {
        drawBranch(node)
    }
    Bonsai(
        tree = tree,
        modifier = Modifier.wrapContentSize(),
        style = BonsaiStyle(
            nodeNameStartPadding = 12.dp,
            nodePadding = PaddingValues(all = 12.dp),
            nodeNameTextStyle = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight(1000),
            )
        )
    )
}

private fun analyzeSyntax(lexicalResults: List<AnalyzerResult>) = runCatching {
    val syntaxAnalyzer = SyntacticalAnalyzer()
    syntaxAnalyzer.analyze(lexicalResults)
}

private fun analyzeLexemes(sourceCode: String): List<AnalyzerResult> {
    val lexicalAnalyzer = LexicalAnalyzer()
    return lexicalAnalyzer.analyze(sourceCode)
}

private fun testTables() {
    val identifiers = readLinesFromInputStream(inputStream = getStreamFromResources(IDENTIFIERS_FILE_NAME))
    if (identifiers == null) {
        println("Не удалось прочесть файл. Завершаем исполнение программы...")
        return
    }

    // Создаём таблицы для дальнейшего заполнения
    val simpleRehashTable = SimpleRehashTable(TABLE_SIZE)
    val binaryTreeTable = BinaryTreeTable()

    // Измеряем время, необходимое для заполнения таблиц элементами из файла
    val timeToFillSimpleRehashTable =
        measureTime { identifiers.forEach { simpleRehashTable.insertElement(it) } }.inWholeMicroseconds
    val timeToFillBinaryTreeTable = measureTime { binaryTreeTable.fill(identifiers) }.inWholeMicroseconds
    println("Время, необходимое для заполнения таблицы \"Простое рехэширование\": $timeToFillSimpleRehashTable мкс")
    println("Время, необходимое для заполнения таблицы \"Бинарное дерево\": $timeToFillBinaryTreeTable мкс")

    // Тестируем поиск по таблице
    simpleRehashTable.testSearch()
    binaryTreeTable.testSearch()

    // Пользовательский поиск
    while (true) {
        print("Введите строку для поиска, или последовательность 'STOP_PROGRAM', чтобы завершить выполнение: ")
        val input: String = readln()
        if (input == "STOP_PROGRAM") return
        val simpleRehashTableAttempts = simpleRehashTable.findElement(input)
        if (simpleRehashTableAttempts != null) {
            println("Количество попыток найти элемент для таблицы с простым рехешированием: $simpleRehashTableAttempts")
        }
        val binaryTreeTableAttempts = binaryTreeTable.findElement(input)
        if (binaryTreeTableAttempts != null) {
            println("Количество попыток найти элемент для бинарного дерева: $binaryTreeTableAttempts")
        }
        println()
    }
}

private const val TABLE_SIZE = 2200
private const val IDENTIFIERS_FILE_NAME = "/identifiers2000.txt"