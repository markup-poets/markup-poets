package org.markup.poet.dsl.table

/**
 * A table: an optional header row and a list of body rows.
 *
 * The model is format-agnostic; writer modules (e.g. markup-asciidoc-writer)
 * turn it into concrete markup.
 */
data class Table(
    val header: TableRow?,
    val rows: List<TableRow>,
)

data class TableRow(val cells: List<String>)
