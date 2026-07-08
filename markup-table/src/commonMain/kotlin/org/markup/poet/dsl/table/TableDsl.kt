package org.markup.poet.dsl.table

@DslMarker
annotation class TableDsl

/**
 * Standalone entry point of the table DSL.
 *
 * ```kotlin
 * val t = table {
 *     header {
 *         cell("col 1")
 *         cell("col 2")
 *     }
 *     row("Hello", "World")
 * }
 * ```
 */
fun table(content: TableScope.() -> Unit): Table = TableBuilder().apply(content).build()

@TableDsl
interface TableScope {
    /** Defines the header row. At most one header; a later call replaces it. */
    fun header(content: RowScope.() -> Unit)

    fun row(content: RowScope.() -> Unit)

    fun row(vararg cells: String)
}

@TableDsl
interface RowScope {
    fun cell(content: String)
}

private class TableBuilder : TableScope {
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

    fun build() = Table(header = header, rows = rows.toList())
}

private class RowBuilder : RowScope {
    private val cells = mutableListOf<String>()

    override fun cell(content: String) {
        cells.add(content)
    }

    fun build() = TableRow(cells.toList())
}
