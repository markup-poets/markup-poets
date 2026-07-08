package org.markup.poet.dsl.document

import org.markup.poet.dsl.table.TableDsl
import org.markup.poet.dsl.table.TableScope
import org.markup.poet.dsl.table.table as buildTable

@DslMarker
annotation class MarkupDsl

/**
 * Entry point of the markup DSL. Sections start at level 1.
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

/**
 * Entry point of the markup DSL with a document title. [title] becomes the
 * document title and sections start at level 2, matching the AsciiDoc
 * convention where level 1 (`=`) is the document title.
 */
fun article(title: String, content: ArticleScope.() -> Unit): Markup =
    ArticleBuilder(title).apply(content).build()

// Document scopes carry @TableDsl in addition to @MarkupDsl so that they are
// suppressed as implicit receivers inside nested table { } lambdas — without
// it, `table { row { +"oops" } }` would resolve against the outer section.

@MarkupDsl
@TableDsl
interface ArticleScope {
    fun section(title: String = "", id: String = "", content: SectionScope.() -> Unit)
}

@MarkupDsl
@TableDsl
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

    /** Adds a table built with the markup-table sub-DSL. */
    fun table(title: String = "", id: String = "", content: TableScope.() -> Unit)

    fun list(type: ListType = ListType.UNORDERED, id: String = "", content: ListScope.() -> Unit)

    fun text(s: String)

    fun text(id: String, s: String)

    operator fun String.unaryPlus() = text(this)
}

@MarkupDsl
@TableDsl
interface ListScope {
    fun item(s: String)

    fun list(type: ListType = ListType.UNORDERED, id: String = "", content: ListScope.() -> Unit)

    operator fun String.unaryPlus() = item(this)
}

@MarkupDsl
@TableDsl
interface CodeScope {
    fun text(s: String)

    operator fun String.unaryPlus() = text(this)
}

private class ArticleBuilder(private val title: String = "") : ArticleScope {
    private val blocks = mutableListOf<Block>()
    private val sectionLevel = if (title.isBlank()) 1 else 2

    override fun section(title: String, id: String, content: SectionScope.() -> Unit) {
        blocks.add(SectionBuilder(id = id, title = title, level = sectionLevel).apply(content).build())
    }

    fun build() = Markup(blocks.toList(), title)
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
        blocks.add(TableBlock(id = id, title = title, table = buildTable(content)))
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
