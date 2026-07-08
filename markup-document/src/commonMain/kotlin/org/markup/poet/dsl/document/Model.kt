package org.markup.poet.dsl.document

import org.markup.poet.dsl.table.Table

/**
 * A markup document: an ordered list of top-level blocks, with an optional
 * document title (empty string means "no title").
 */
data class Markup(val blocks: List<Block>, val title: String = "")

/**
 * A block element of a markup document.
 *
 * The model is format-agnostic: it describes document structure only.
 * Writer modules (e.g. markup-asciidoc-writer) map blocks to a concrete syntax.
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

/**
 * A table block: a standalone [Table] from the markup-table sub-DSL,
 * placed in a document with an optional id and title.
 */
data class TableBlock(
    override val id: String,
    val title: String,
    val table: Table,
) : Block()

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
