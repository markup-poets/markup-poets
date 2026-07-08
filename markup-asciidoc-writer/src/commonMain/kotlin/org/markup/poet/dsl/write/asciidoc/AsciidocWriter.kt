package org.markup.poet.dsl.write.asciidoc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.io.Sink
import kotlinx.io.writeString
import org.markup.poet.dsl.document.Block
import org.markup.poet.dsl.document.BlockImage
import org.markup.poet.dsl.document.CodeBlock
import org.markup.poet.dsl.document.ImageAttributes
import org.markup.poet.dsl.document.ListBlock
import org.markup.poet.dsl.document.ListType
import org.markup.poet.dsl.document.Markup
import org.markup.poet.dsl.document.Paragraph
import org.markup.poet.dsl.document.Section
import org.markup.poet.dsl.document.TableBlock

/**
 * Writes the document as AsciiDoc source text.
 */
fun Markup.toAsciidoc(): String = asciidocChunks().joinToString("")

/**
 * Writes the document as AsciiDoc source text into [out].
 */
fun Markup.writeAsciidocTo(out: Appendable) {
    asciidocChunks().forEach { out.append(it) }
}

/**
 * Writes the document as AsciiDoc source text into [sink], UTF-8 encoded.
 * The sink is neither flushed nor closed; the caller owns its lifecycle.
 */
fun Markup.writeAsciidocTo(sink: Sink) {
    asciidocChunks().forEach { sink.writeString(it) }
}

/**
 * The document as a cold [Flow] of AsciiDoc source-text chunks.
 *
 * Chunks arrive in document order and concatenating all emissions yields
 * exactly [toAsciidoc]. Chunk boundaries are an implementation detail;
 * currently the title line is one chunk and each top-level block is one chunk.
 */
fun Markup.asciidocFlow(): Flow<String> = asciidocChunks().asFlow()

private fun Markup.asciidocChunks(): Sequence<String> = sequence {
    if (title.isNotBlank()) yield("= $title\n")
    blocks.forEachIndexed { index, block ->
        val separator = if (index > 0 || title.isNotBlank()) "\n" else ""
        yield(separator + buildString { renderBlock(block, this) })
    }
}

internal fun renderBlock(block: Block, out: Appendable) {
    when (block) {
        is Section -> renderSection(block, out)
        is Paragraph -> out.append(block.text).append('\n')
        is CodeBlock -> renderCode(block, out)
        is BlockImage -> renderImage(block, out)
        is TableBlock -> renderTableBlock(block, out)
        is ListBlock -> renderList(block, out, level = 1)
    }
}

private fun renderSection(section: Section, out: Appendable) {
    if (section.id.isNotBlank()) {
        out.append("[#").append(section.id).append("]\n")
    }
    out.append("=".repeat(section.level)).append(' ').append(section.title).append('\n')
    section.blocks.forEach { block ->
        out.append('\n')
        renderBlock(block, out)
    }
}

private fun renderCode(block: CodeBlock, out: Appendable) {
    if (block.lang.isNotBlank()) {
        out.append("[source,").append(block.lang).append("]\n")
    } else {
        out.append("[source]\n")
    }
    out.append("----\n")
    out.append(block.code).append('\n')
    out.append("----\n")
}

private fun renderImage(block: BlockImage, out: Appendable) {
    if (block.id.isNotBlank()) {
        out.append("[#").append(block.id).append("]\n")
    }
    if (block.title.isNotBlank()) {
        out.append('.').append(block.title).append('\n')
    }
    if (block.link.isNotBlank()) {
        out.append("[link=").append(block.link).append("]\n")
    }
    out.append("image::").append(block.path)
        .append('[').append(positionalAttributes(block.attributes)).append("]\n")
}

private fun positionalAttributes(attributes: ImageAttributes): String {
    val alt = attributes.alt
    val width = attributes.width
    val height = attributes.height
    return buildList {
        if (alt != null) add(alt)
        if (width != null && height != null) {
            if (alt == null) add("")
            add(width.toString())
            add(height.toString())
        }
    }.joinToString(",")
}

private fun renderTableBlock(block: TableBlock, out: Appendable) {
    if (block.id.isNotBlank()) {
        out.append("[#").append(block.id).append("]\n")
    }
    if (block.title.isNotBlank()) {
        out.append('.').append(block.title).append('\n')
    }
    block.table.asciidocChunks().forEach { out.append(it) }
}

private fun renderList(block: ListBlock, out: Appendable, level: Int) {
    val marker = if (block.type == ListType.ORDERED) "." else "*"
    block.items.forEach { item ->
        when (item) {
            is ListBlock -> renderList(item, out, level + 1)
            is Paragraph -> out.append(marker.repeat(level)).append(' ').append(item.text).append('\n')
            else -> renderBlock(item, out)
        }
    }
}
