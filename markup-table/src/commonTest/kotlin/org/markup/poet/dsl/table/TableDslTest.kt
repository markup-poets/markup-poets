package org.markup.poet.dsl.table

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TableDslTest {

    @Test
    fun tableWithoutHeader() {
        val t = table {
            row("a", "b")
            row {
                cell("c")
                cell("d")
            }
        }

        assertNull(t.header)
        assertEquals(listOf(TableRow(listOf("a", "b")), TableRow(listOf("c", "d"))), t.rows)
    }

    @Test
    fun tableWithHeader() {
        val t = table {
            header {
                cell("col 1")
                cell("col 2")
            }
            row("x", "y")
        }

        assertEquals(TableRow(listOf("col 1", "col 2")), t.header)
        assertEquals(listOf(TableRow(listOf("x", "y"))), t.rows)
    }

    @Test
    fun laterHeaderCallReplacesEarlier() {
        val t = table {
            header { cell("old") }
            header { cell("new") }
        }

        assertEquals(TableRow(listOf("new")), t.header)
    }

    @Test
    fun rowVarargAndRowBuilderAreEquivalent() {
        val vararg = table { row("a", "b") }
        val builder = table {
            row {
                cell("a")
                cell("b")
            }
        }

        assertEquals(vararg, builder)
    }
}
