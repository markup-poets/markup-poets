package org.markup.poet.markup.asciidoc

import org.markup.poet.markup.model.Block
import org.markup.poet.markup.model.BlockImage
import org.markup.poet.markup.model.CodeBlock
import org.markup.poet.markup.model.ImageAttributes
import org.markup.poet.markup.model.ListBlock
import org.markup.poet.markup.model.ListType
import org.markup.poet.markup.model.Markup
import org.markup.poet.markup.model.Paragraph
import org.markup.poet.markup.model.Section
import org.markup.poet.markup.model.TableBlock

/**
 * Renders the document as AsciiDoc source text.
 */
fun Markup.renderAsciidoc(): String = buildString { renderAsciidocTo(this) }

/**
 * Renders the document as AsciiDoc source text into [out].
 * A non-blank [Markup.title] becomes the `=` document title line;
 * top-level blocks are separated by a blank line.
 */
fun Markup.renderAsciidocTo(out: Appendable) {
    if (title.isNotBlank()) {
        out.append("= ").append(title).append('\n')
        blocks.forEach { block ->
            out.append('\n')
            renderBlock(block, out)
        }
    } else {
        blocks.forEachIndexed { index, block ->
            if (index > 0) out.append('\n')
            renderBlock(block, out)
        }
    }
}

private fun renderBlock(block: Block, out: Appendable) {
    when (block) {
        is Section -> renderSection(block, out)
        is Paragraph -> out.append(block.text).append('\n')
        is CodeBlock -> renderCode(block, out)
        is BlockImage -> renderImage(block, out)
        is TableBlock -> renderTable(block, out)
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

private fun renderTable(block: TableBlock, out: Appendable) {
    if (block.id.isNotBlank()) {
        out.append("[#").append(block.id).append("]\n")
    }
    if (block.title.isNotBlank()) {
        out.append('.').append(block.title).append('\n')
    }
    val header = block.header
    if (header == null) {
        val cols = List(block.columnCount) { "1" }.joinToString(",")
        out.append("[cols=\"").append(cols).append("\"]\n")
    }
    out.append("|===\n")
    if (header != null) {
        header.cells.forEach { cell -> out.append('|').append(cell) }
        out.append('\n')
    }
    block.rows.forEachIndexed { index, row ->
        if (index > 0 || header != null) out.append('\n')
        row.cells.forEach { cell ->
            out.append("| ").append(cell).append('\n')
        }
    }
    out.append("|===\n")
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
