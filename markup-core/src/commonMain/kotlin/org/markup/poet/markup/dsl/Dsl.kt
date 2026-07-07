package org.markup.poet.markup.dsl

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
import org.markup.poet.markup.model.TableRow

@DslMarker
annotation class MarkupPoetDsl

/**
 * Entry point of the markup DSL.
 *
 * ```kotlin
 * val doc = article {
 *     section("Title") {
 *         +"A paragraph."
 *     }
 * }
 * ```
 */
fun article(content: ArticleScope.() -> Unit): Markup = ArticleBuilder().apply(content).build()

@MarkupPoetDsl
interface ArticleScope {
    fun section(title: String = "", id: String = "", content: SectionScope.() -> Unit)
}

@MarkupPoetDsl
interface SectionScope {
    fun section(title: String = "", id: String = "", content: SectionScope.() -> Unit)

    fun code(lang: String? = null, id: String = "", content: CodeScope.() -> Unit)

    fun image(
        path: String,
        id: String = "",
        title: String = "",
        link: String = "",
        width: Int? = null,
        height: Int? = null,
        alt: String? = null,
    )

    fun table(title: String = "", id: String = "", content: TableScope.() -> Unit)

    fun list(type: ListType = ListType.UNORDERED, id: String = "", content: ListScope.() -> Unit)

    fun text(s: String)

    fun text(id: String, s: String)

    operator fun String.unaryPlus() = text(this)
}

@MarkupPoetDsl
interface ListScope {
    fun item(s: String)

    fun list(type: ListType = ListType.UNORDERED, id: String = "", content: ListScope.() -> Unit)

    operator fun String.unaryPlus() = item(this)
}

@MarkupPoetDsl
interface CodeScope {
    fun text(s: String)

    operator fun String.unaryPlus() = text(this)
}

@MarkupPoetDsl
interface TableScope {
    /** Defines the header row. At most one header; a later call replaces it. */
    fun header(content: RowScope.() -> Unit)

    fun row(content: RowScope.() -> Unit)

    fun row(vararg cells: String)
}

@MarkupPoetDsl
interface RowScope {
    fun cell(content: String)
}

private class ArticleBuilder : ArticleScope {
    private val blocks = mutableListOf<Block>()

    override fun section(title: String, id: String, content: SectionScope.() -> Unit) {
        blocks.add(SectionBuilder(id = id, title = title, level = 1).apply(content).build())
    }

    fun build() = Markup(blocks.toList())
}

private class SectionBuilder(
    private val id: String,
    private val title: String,
    private val level: Int,
) : SectionScope {
    private val blocks = mutableListOf<Block>()

    override fun section(title: String, id: String, content: SectionScope.() -> Unit) {
        blocks.add(SectionBuilder(id = id, title = title, level = level + 1).apply(content).build())
    }

    override fun code(lang: String?, id: String, content: CodeScope.() -> Unit) {
        blocks.add(CodeBuilder(lang = lang, id = id).apply(content).build())
    }

    override fun image(
        path: String,
        id: String,
        title: String,
        link: String,
        width: Int?,
        height: Int?,
        alt: String?,
    ) {
        blocks.add(
            BlockImage(
                id = id,
                path = path,
                title = title,
                link = link,
                attributes = ImageAttributes(width = width, height = height, alt = alt),
            )
        )
    }

    override fun table(title: String, id: String, content: TableScope.() -> Unit) {
        blocks.add(TableBuilder(id = id, title = title).apply(content).build())
    }

    override fun list(type: ListType, id: String, content: ListScope.() -> Unit) {
        blocks.add(ListBuilder(id = id, type = type).apply(content).build())
    }

    override fun text(s: String) {
        blocks.add(Paragraph(id = "", text = s))
    }

    override fun text(id: String, s: String) {
        blocks.add(Paragraph(id = id, text = s))
    }

    fun build() = Section(id = id, title = title, level = level, blocks = blocks.toList())
}

private class CodeBuilder(private val lang: String?, private val id: String) : CodeScope {
    private val code = StringBuilder()

    override fun text(s: String) {
        code.append(s)
    }

    fun build() = CodeBlock(id = id, lang = lang ?: "", code = code.toString())
}

private class ListBuilder(private val id: String, private val type: ListType) : ListScope {
    private val items = mutableListOf<Block>()

    override fun item(s: String) {
        items.add(Paragraph(id = "", text = s))
    }

    override fun list(type: ListType, id: String, content: ListScope.() -> Unit) {
        items.add(ListBuilder(id = id, type = type).apply(content).build())
    }

    fun build() = ListBlock(id = id, type = type, items = items.toList())
}

private class TableBuilder(private val id: String, private val title: String) : TableScope {
    private var header: TableRow? = null
    private val rows = mutableListOf<TableRow>()

    override fun header(content: RowScope.() -> Unit) {
        header = RowBuilder().apply(content).build()
    }

    override fun row(content: RowScope.() -> Unit) {
        rows.add(RowBuilder().apply(content).build())
    }

    override fun row(vararg cells: String) {
        rows.add(TableRow(cells.toList()))
    }

    fun build() = TableBlock(id = id, title = title, header = header, rows = rows.toList())
}

private class RowBuilder : RowScope {
    private val cells = mutableListOf<String>()

    override fun cell(content: String) {
        cells.add(content)
    }

    fun build() = TableRow(cells.toList())
}
