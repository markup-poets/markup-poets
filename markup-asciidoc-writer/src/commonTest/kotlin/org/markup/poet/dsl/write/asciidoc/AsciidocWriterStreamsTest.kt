package org.markup.poet.dsl.write.asciidoc

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readString
import org.markup.poet.dsl.document.article
import org.markup.poet.dsl.table.table
import kotlin.test.Test
import kotlin.test.assertEquals

class AsciidocWriterStreamsTest {

    @Test
    fun writesDocumentToSink() {
        val doc = article("Doc") {
            section("S") {
                +"para"
            }
        }

        val buffer = Buffer()
        doc.writeAsciidocTo(buffer)
        assertEquals(doc.toAsciidoc(), buffer.readString())
    }

    @Test
    fun writesDocumentToAppendable() {
        val doc = article("Doc") {
            section("S") {
                +"para"
            }
        }

        val sb = StringBuilder()
        doc.writeAsciidocTo(sb)
        assertEquals(doc.toAsciidoc(), sb.toString())
    }

    @Test
    fun emptyDocumentWritesNothing() {
        val buffer = Buffer()
        article {}.writeAsciidocTo(buffer)
        assertEquals("", buffer.readString())
        assertEquals("", article {}.toAsciidoc())
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

        assertEquals(doc.toAsciidoc(), doc.asciidocFlow().toList().joinToString(""))
    }

    @Test
    fun flowEmitsTitleThenOneChunkPerTopLevelBlock() = runTest {
        val doc = article("T") {
            section("A") {}
            section("B") {}
        }

        val chunks = doc.asciidocFlow().toList()
        assertEquals(3, chunks.size)
        assertEquals("= T\n", chunks[0])
        assertEquals("\n== A\n", chunks[1])
        assertEquals("\n== B\n", chunks[2])
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
            ||===
            ||col 1|col 2
            |
            || Hello
            || World
            ||===
            |""".trimMargin(),
            t.toAsciidoc(),
        )
    }

    @Test
    fun standaloneHeaderlessTableGetsColsAttribute() {
        val t = table {
            row("a", "b")
            row("c", "d")
        }

        assertEquals(
            """
            |[cols="1,1"]
            ||===
            || a
            || b
            |
            || c
            || d
            ||===
            |""".trimMargin(),
            t.toAsciidoc(),
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
        t.writeAsciidocTo(buffer)
        assertEquals(t.toAsciidoc(), buffer.readString())
        assertEquals(t.toAsciidoc(), t.asciidocFlow().toList().joinToString(""))
    }

    @Test
    fun tableFlowEmitsPrologueRowsEpilogue() = runTest {
        val t = table {
            header { cell("h") }
            row("a")
            row("b")
        }

        val chunks = t.asciidocFlow().toList()
        assertEquals(4, chunks.size)
        assertEquals("|===\n|h\n", chunks[0])
        assertEquals("\n| a\n", chunks[1])
        assertEquals("\n| b\n", chunks[2])
        assertEquals("|===\n", chunks[3])
    }
}
