package org.markup.poet.dsl.write.asciidoc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.io.Sink
import kotlinx.io.writeString
import org.markup.poet.dsl.table.Table

/**
 * Writes a standalone table as AsciiDoc source text.
 */
fun Table.toAsciidoc(): String = asciidocChunks().joinToString("")

/**
 * Writes a standalone table as AsciiDoc source text into [out].
 */
fun Table.writeAsciidocTo(out: Appendable) {
    asciidocChunks().forEach { out.append(it) }
}

/**
 * Writes a standalone table as AsciiDoc source text into [sink], UTF-8 encoded.
 * The sink is neither flushed nor closed; the caller owns its lifecycle.
 */
fun Table.writeAsciidocTo(sink: Sink) {
    asciidocChunks().forEach { sink.writeString(it) }
}

/**
 * The table as a cold [Flow] of AsciiDoc source-text chunks.
 *
 * Chunks arrive in order and concatenating all emissions yields exactly
 * [toAsciidoc]. Chunk boundaries are an implementation detail; currently the
 * prologue (`[cols]`/`|===`/header) is one chunk, then one chunk per row,
 * then the closing `|===`.
 */
fun Table.asciidocFlow(): Flow<String> = asciidocChunks().asFlow()

internal fun Table.asciidocChunks(): Sequence<String> = sequence {
    val columnCount = maxOf(header?.cells?.size ?: 0, rows.maxOfOrNull { it.cells.size } ?: 0)
    yield(
        buildString {
            if (header == null) {
                val cols = List(columnCount) { "1" }.joinToString(",")
                append("[cols=\"").append(cols).append("\"]\n")
            }
            append("|===\n")
            header?.let { headerRow ->
                headerRow.cells.forEach { cell -> append('|').append(cell) }
                append('\n')
            }
        }
    )
    rows.forEachIndexed { index, row ->
        yield(
            buildString {
                if (index > 0 || header != null) append('\n')
                row.cells.forEach { cell ->
                    append("| ").append(cell).append('\n')
                }
            }
        )
    }
    yield("|===\n")
}
