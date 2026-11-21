package com.wantstracker.app.backup

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Small OOXML reader/writer for plain-text wishlist spreadsheets; no third-party dependency. */
object XlsxSheet {
    fun write(output: OutputStream, rows: List<List<String>>) {
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, body: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            entry("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Wants" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            entry("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            val sheet = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
                rows.forEachIndexed { ri, row ->
                    append("<row r=\"\${ri + 1}\">")
                    row.forEachIndexed { ci, value ->
                        val ref = columnName(ci + 1) + (ri + 1)
                        append("<c r=\"\$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">\${escape(value)}</t></is></c>")
                    }
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            }
            entry("xl/worksheets/sheet1.xml", sheet)
        }
    }

    fun read(input: InputStream): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (!e.isDirectory && (e.name == "xl/sharedStrings.xml" || e.name.startsWith("xl/worksheets/sheet"))) {
                    entries[e.name] = zip.readBytes()
                }
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
        val shared = entries["xl/sharedStrings.xml"]?.let(::readSharedStrings).orEmpty()
        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: entries.entries.firstOrNull { it.key.startsWith("xl/worksheets/sheet") }?.value
            ?: error("This Excel file has no readable first worksheet")
        return readSheet(sheetBytes, shared)
    }

    private fun readSharedStrings(bytes: ByteArray): List<String> {
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        val result = mutableListOf<String>()
        var inSi = false
        var current = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") { inSi = true; current = StringBuilder() }
                    if (inSi && parser.name == "t") current.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> if (parser.name == "si" && inSi) {
                    result.add(current.toString()); inSi = false
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun readSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        val rows = mutableListOf<List<String>>()
        var row: MutableMap<Int, String>? = null
        var col = 0
        var type = ""
        var value = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> row = mutableMapOf()
                    "c" -> {
                        col = columnIndex(parser.getAttributeValue(null, "r").orEmpty())
                        type = parser.getAttributeValue(null, "t").orEmpty()
                        value = ""
                    }
                    "v", "t" -> if (row != null) value += parser.nextText()
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "c" -> {
                        val decoded = if (type == "s") shared.getOrNull(value.toIntOrNull() ?: -1).orEmpty() else value
                        row?.set(col, decoded)
                    }
                    "row" -> {
                        val r = row.orEmpty()
                        val width = (r.keys.maxOrNull() ?: -1) + 1
                        rows.add((0 until width).map { r[it].orEmpty() })
                        row = null
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun columnName(number: Int): String {
        var n = number
        val out = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            out.insert(0, ('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return out.toString()
    }

    private fun columnIndex(ref: String): Int {
        var n = 0
        ref.takeWhile { it.isLetter() }.uppercase().forEach { n = n * 26 + (it - 'A' + 1) }
        return (n - 1).coerceAtLeast(0)
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
}