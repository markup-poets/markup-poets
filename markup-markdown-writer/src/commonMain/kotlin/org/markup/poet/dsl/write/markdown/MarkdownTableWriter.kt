package org.markup.poet.dsl.write.markdown

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.io.Sink
import kotlinx.io.writeString
import org.markup.poet.dsl.table.Table

/**
 * Writes a standalone table as Markdown (GFM) source text.
 * GFM tables require a header row; a headerless [Table] gets empty header cells.
 */
fun Table.toMarkdown(): String = markdownChunks().joinToString("")

/**
 * Writes a standalone table as Markdown source text into [out].
 */
fun Table.writeMarkdownTo(out: Appendable) {
    markdownChunks().forEach { out.append(it) }
}

/**
 * Writes a standalone table as Markdown source text into [sink], UTF-8 encoded.
 * The sink is neither flushed nor closed; the caller owns its lifecycle.
 */
fun Table.writeMarkdownTo(sink: Sink) {
    markdownChunks().forEach { sink.writeString(it) }
}

/**
 * The table as a cold [Flow] of Markdown source-text chunks.
 *
 * Chunks arrive in order and concatenating all emissions yields exactly
 * [toMarkdown]. Chunk boundaries are an implementation detail; currently the
 * header row plus delimiter row is one chunk, then one chunk per body row.
 */
fun Table.markdownFlow(): Flow<String> = markdownChunks().asFlow()

internal fun Table.markdownChunks(): Sequence<String> = sequence {
    val columnCount = maxOf(header?.cells?.size ?: 0, rows.maxOfOrNull { it.cells.size } ?: 0)
    yield(
        buildString {
            val headerCells = header?.cells.orEmpty()
            val padded = headerCells + List(columnCount - headerCells.size) { "" }
            appendRow(padded)
            append('|')
            repeat(columnCount) { append(" --- |") }
            append('\n')
        }
    )
    rows.forEach { row ->
        yield(buildString { appendRow(row.cells) })
    }
}

private fun StringBuilder.appendRow(cells: List<String>) {
    append('|')
    cells.forEach { cell ->
        append(' ').append(cell.replace("|", "\\|")).append(" |")
    }
    append('\n')
}
