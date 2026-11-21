package com.wantstracker.app.backup

import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status

data class ImportRow(
    val name: String, val price: Double, val quarter: QuarterKey?, val status: Status,
    val category: String, val note: String, val link: String = "", val priority: String = "Want"
)
data class ParseResult(val rows: List<ImportRow>, val warnings: List<String>)

object CsvImport {
    fun parseMatrix(matrix: List<List<String>>): ParseResult {
        fun quote(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
        return parse(matrix.joinToString("\n") { row -> row.joinToString(",") { quote(it) } })
    }

    fun parse(text: String): ParseResult {
        val rows = mutableListOf<ImportRow>()
        val warnings = mutableListOf<String>()
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParseResult(rows, warnings)
        val first = splitLine(lines.first(), ',')
        val hasHeader = first.firstOrNull()?.trim()?.lowercase() in setOf("name", "title", "item")
        val headers = if (hasHeader) first.map { it.trim().lowercase() } else emptyList()
        (if (hasHeader) lines.drop(1) else lines).forEachIndexed { index, line ->
            val delimiter = when {
                line.contains('\t') -> '\t'
                line.count { it == ';' } > line.count { it == ',' } -> ';'
                line.contains('|') && !line.contains(',') -> '|'
                else -> ','
            }
            val cells = splitLine(line, delimiter)
            fun field(names: Set<String>, fallback: Int): String {
                val idx = if (hasHeader) headers.indexOfFirst { it in names } else fallback
                return cells.getOrNull(idx)?.trim().orEmpty()
            }
            val name = field(setOf("name","title","item"), 0)
            if (name.isBlank()) { warnings.add("Row ${index + 1}: missing name, skipped"); return@forEachIndexed }
            val price = field(setOf("price"), 1).replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            val year = field(setOf("planned year","year"), -1).toIntOrNull()
            val qText = field(setOf("planned quarter","quarter"), 2)
            val quarter = QuarterKey.parse(qText) ?: if (year != null && qText.matches(Regex("[1-4]"))) QuarterKey(year, qText.toInt()) else null
            val status = Status.fromText(field(setOf("status"), 3))
            val category = field(setOf("category"), 4)
            val note = field(setOf("notes","note"), 5)
            val link = field(setOf("link","url"), 6)
            val priority = field(setOf("priority"), 7).ifBlank { "Want" }
            rows.add(ImportRow(name, price, quarter, status, category, note, link, priority))
        }
        return ParseResult(rows, warnings)
    }
    private fun splitLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>(); val cur = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { cur.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == delimiter && !quoted -> { out.add(cur.toString()); cur.clear() }
                else -> cur.append(ch)
            }
            i++
        }
        out.add(cur.toString()); return out
    }
}
