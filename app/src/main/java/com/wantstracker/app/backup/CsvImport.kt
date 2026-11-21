package com.wantstracker.app.backup

import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status

data class ImportRow(
    val name: String,
    val price: Double,
    val quarter: QuarterKey?,
    val status: Status,
    val category: String,
    val note: String
)

data class ParseResult(val rows: List<ImportRow>, val warnings: List<String>)

/**
 * One want per line: name, price, quarter, status, category, note
 * e.g.  iPhone 15, 79900, Q2 2025, bought, Tech
 */
object CsvImport {
    fun parse(text: String): ParseResult {
        val rows = mutableListOf<ImportRow>()
        val warnings = mutableListOf<String>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        lines.forEachIndexed { index, line ->
            val delim = when {
                line.contains('\t') -> '\t'
                line.count { it == ';' } > line.count { it == ',' } -> ';'
                line.contains('|') && !line.contains(',') -> '|'
                else -> ','
            }
            val cells = splitLine(line, delim)
            val first = cells.firstOrNull()?.trim().orEmpty()
            if (index == 0 && first.lowercase() in setOf("name", "item", "want", "title")) return@forEachIndexed
            if (first.isBlank()) {
                warnings.add("Line ${index + 1}: no name, skipped")
                return@forEachIndexed
            }
            val price = cells.getOrNull(1)?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 0.0
            val qText = cells.getOrNull(2).orEmpty().trim()
            val quarter = QuarterKey.parse(qText)
            if (qText.isNotEmpty() && quarter == null && !qText.equals("unplanned", ignoreCase = true)) {
                warnings.add("Line ${index + 1}: couldn't read quarter \"$qText\" (use e.g. Q2 2025), imported as unplanned")
            }
            val status = Status.fromText(cells.getOrNull(3).orEmpty())
            val category = cells.getOrNull(4)?.trim().orEmpty()
            val note = cells.getOrNull(5)?.trim().orEmpty()
            rows.add(ImportRow(first, price, quarter, status, category, note))
        }
        return ParseResult(rows, warnings)
    }

    private fun splitLine(line: String, delim: Char): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == delim && !inQuotes -> {
                    out.add(cur.toString())
                    cur.clear()
                }
                else -> cur.append(c)
            }
            i++
        }
        out.add(cur.toString())
        return out
    }
}
