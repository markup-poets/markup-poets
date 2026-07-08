package org.markup.poet.dsl.write.markdown

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readString
import org.markup.poet.dsl.document.article
import org.markup.poet.dsl.table.table
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownWriterStreamsTest {

    @Test
    fun writesDocumentToSink() {
        val doc = article("Doc") {
            section("S") {
                +"para"
            }
        }

        val buffer = Buffer()
        doc.writeMarkdownTo(buffer)
        assertEquals(doc.toMarkdown(), buffer.readString())
    }

    @Test
    fun writesDocumentToAppendable() {
        val doc = article("Doc") {
            section("S") {
                +"para"
            }
        }

        val sb = StringBuilder()
        doc.writeMarkdownTo(sb)
        assertEquals(doc.toMarkdown(), sb.toString())
    }

    @Test
    fun emptyDocumentWritesNothing() {
        val buffer = Buffer()
        article {}.writeMarkdownTo(buffer)
        assertEquals("", buffer.readString())
        assertEquals("", article {}.toMarkdown())
    }

    @Test
    fun flowChunksConcatenateToStringOutput() = runTest {
        val doc = article("Doc") {
            section("A") {
                +"one"
                code("kotlin") {
                    +"val x = 1"
                }
            }
            section("B") {
                table {
                    header { cell("h") }
                    row("v")
                }
            }
        }

        assertEquals(doc.toMarkdown(), doc.markdownFlow().toList().joinToString(""))
    }

    @Test
    fun flowEmitsTitleThenOneChunkPerTopLevelBlock() = runTest {
        val doc = article("T") {
            section("A") {}
            section("B") {}
        }

        val chunks = doc.markdownFlow().toList()
        assertEquals(3, chunks.size)
        assertEquals("# T\n", chunks[0])
        assertEquals("\n## A\n", chunks[1])
        assertEquals("\n## B\n", chunks[2])
    }

    @Test
    fun standaloneTableWithHeader() {
        val t = table {
            header {
                cell("col 1")
                cell("col 2")
            }
            row("Hello", "World")
        }

        assertEquals(
            """
            || col 1 | col 2 |
            || --- | --- |
            || Hello | World |
            |""".trimMargin(),
            t.toMarkdown(),
        )
    }

    @Test
    fun standaloneHeaderlessTableGetsEmptyHeader() {
        val t = table {
            row("a", "b")
            row("c", "d")
        }

        assertEquals(
            """
            ||  |  |
            || --- | --- |
            || a | b |
            || c | d |
            |""".trimMargin(),
            t.toMarkdown(),
        )
    }

    @Test
    fun pipesInCellsAreEscaped() {
        val t = table {
            header { cell("a|b") }
            row("c|d")
        }

        assertEquals(
            """
            || a\|b |
            || --- |
            || c\|d |
            |""".trimMargin(),
            t.toMarkdown(),
        )
    }

    @Test
    fun standaloneTableSinkAndFlowAgreeWithString() = runTest {
        val t = table {
            header { cell("h") }
            row("v1")
            row("v2")
        }

        val buffer = Buffer()
        t.writeMarkdownTo(buffer)
        assertEquals(t.toMarkdown(), buffer.readString())
        assertEquals(t.toMarkdown(), t.markdownFlow().toList().joinToString(""))
    }

    @Test
    fun tableFlowEmitsHeaderChunkThenRowChunks() = runTest {
        val t = table {
            header { cell("h") }
            row("a")
            row("b")
        }

        val chunks = t.markdownFlow().toList()
        assertEquals(3, chunks.size)
        assertEquals("| h |\n| --- |\n", chunks[0])
        assertEquals("| a |\n", chunks[1])
        assertEquals("| b |\n", chunks[2])
    }
}
