package org.markup.poet.dsl.write.markdown

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.io.Sink
import kotlinx.io.writeString
import org.markup.poet.dsl.document.Block
import org.markup.poet.dsl.document.BlockImage
import org.markup.poet.dsl.document.CodeBlock
import org.markup.poet.dsl.document.ListBlock
import org.markup.poet.dsl.document.ListType
import org.markup.poet.dsl.document.Markup
import org.markup.poet.dsl.document.Paragraph
import org.markup.poet.dsl.document.Section
import org.markup.poet.dsl.document.TableBlock

/*
 * Markdown (GitHub-flavored) mapping notes:
 * - block ids become `<a id="..."></a>` HTML anchor lines (Markdown has no
 *   native anchor syntax; GFM inline HTML renders them as link targets)
 * - table titles become a bold `**title**` line (no native table caption)
 * - image width/height have no native Markdown syntax and are dropped
 */

/**
 * Writes the document as Markdown (GFM) source text.
 */
fun Markup.toMarkdown(): String = markdownChunks().joinToString("")

/**
 * Writes the document as Markdown source text into [out].
 */
fun Markup.writeMarkdownTo(out: Appendable) {
    markdownChunks().forEach { out.append(it) }
}

/**
 * Writes the document as Markdown source text into [sink], UTF-8 encoded.
 * The sink is neither flushed nor closed; the caller owns its lifecycle.
 */
fun Markup.writeMarkdownTo(sink: Sink) {
    markdownChunks().forEach { sink.writeString(it) }
}

/**
 * The document as a cold [Flow] of Markdown source-text chunks.
 *
 * Chunks arrive in document order and concatenating all emissions yields
 * exactly [toMarkdown]. Chunk boundaries are an implementation detail;
 * currently the title line is one chunk and each top-level block is one chunk.
 */
fun Markup.markdownFlow(): Flow<String> = markdownChunks().asFlow()

private fun Markup.markdownChunks(): Sequence<String> = sequence {
    if (title.isNotBlank()) yield("# $title\n")
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

private fun renderAnchor(id: String, out: Appendable) {
    out.append("<a id=\"").append(id).append("\"></a>\n")
}

private fun renderSection(section: Section, out: Appendable) {
    if (section.id.isNotBlank()) {
        renderAnchor(section.id, out)
    }
    out.append("#".repeat(section.level)).append(' ').append(section.title).append('\n')
    section.blocks.forEach { block ->
        out.append('\n')
        renderBlock(block, out)
    }
}

private fun renderCode(block: CodeBlock, out: Appendable) {
    out.append("```").append(block.lang).append('\n')
    out.append(block.code).append('\n')
    out.append("```\n")
}

private fun renderImage(block: BlockImage, out: Appendable) {
    if (block.id.isNotBlank()) {
        renderAnchor(block.id, out)
    }
    val titlePart = if (block.title.isNotBlank()) " \"${block.title}\"" else ""
    val image = "![${block.attributes.alt ?: ""}](${block.path}$titlePart)"
    if (block.link.isNotBlank()) {
        out.append('[').append(image).append("](").append(block.link).append(')')
    } else {
        out.append(image)
    }
    out.append('\n')
}

private fun renderTableBlock(block: TableBlock, out: Appendable) {
    val hasPrefix = block.id.isNotBlank() || block.title.isNotBlank()
    if (block.id.isNotBlank()) {
        renderAnchor(block.id, out)
    }
    if (block.title.isNotBlank()) {
        out.append("**").append(block.title).append("**\n")
    }
    if (hasPrefix) {
        out.append('\n')
    }
    block.table.markdownChunks().forEach { out.append(it) }
}

private fun renderList(block: ListBlock, out: Appendable, level: Int) {
    var index = 0
    block.items.forEach { item ->
        when (item) {
            is ListBlock -> renderList(item, out, level + 1)
            is Paragraph -> {
                index++
                val marker = if (block.type == ListType.ORDERED) "$index." else "-"
                out.append("    ".repeat(level - 1)).append(marker).append(' ')
                    .append(item.text).append('\n')
            }
            else -> renderBlock(item, out)
        }
    }
}
