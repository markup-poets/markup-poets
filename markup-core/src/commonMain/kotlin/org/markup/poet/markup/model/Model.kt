package org.markup.poet.markup.model

/**
 * A markup document: an ordered list of top-level blocks, with an optional
 * document title (empty string means "no title").
 */
data class Markup(val blocks: List<Block>, val title: String = "")

/**
 * A block element of a markup document.
 *
 * The model is renderer-agnostic: it describes document structure only.
 * Renderers (e.g. markup-asciidoc-renderer) map blocks to a concrete syntax.
 */
sealed class Block {
    /** Optional element id; empty string means "no id". */
    abstract val id: String
}

data class Section(
    override val id: String,
    val title: String,
    val level: Int,
    val blocks: List<Block>,
) : Block()

data class Paragraph(
    override val id: String,
    val text: String,
) : Block()

data class CodeBlock(
    override val id: String,
    val lang: String,
    val code: String,
) : Block()

enum class ListType { ORDERED, UNORDERED }

/**
 * A list block. [items] holds [Paragraph]s for plain items and nested
 * [ListBlock]s for sub-lists.
 */
data class ListBlock(
    override val id: String,
    val type: ListType,
    val items: List<Block>,
) : Block()

data class TableRow(val cells: List<String>)

data class TableBlock(
    override val id: String,
    val title: String,
    val header: TableRow?,
    val rows: List<TableRow>,
) : Block() {
    val columnCount: Int
        get() = maxOf(header?.cells?.size ?: 0, rows.maxOfOrNull { it.cells.size } ?: 0)
}

data class ImageAttributes(
    val width: Int? = null,
    val height: Int? = null,
    val alt: String? = null,
)

data class BlockImage(
    override val id: String,
    val path: String,
    val title: String,
    val link: String,
    val attributes: ImageAttributes,
) : Block()
