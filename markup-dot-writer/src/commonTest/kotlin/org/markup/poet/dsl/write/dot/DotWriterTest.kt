package org.markup.poet.dsl.write.dot

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readString
import org.markup.poet.dsl.graph.digraph
import org.markup.poet.dsl.graph.graph
import kotlin.test.Test
import kotlin.test.assertEquals

class DotWriterTest {

    @Test
    fun styledDigraph() {
        val g = digraph("StyledGraph") {
            node("A") {
                shape = "box"
                color = "red"
            }
            node("B") {
                shape = "ellipse"
                color = "blue"
            }
            edge("A", "B") {
                style = "dashed"
                color = "black"
            }
            edge("B", "A") {
                style = "bold"
                color = "orange"
            }
        }

        assertEquals(
            """
            |digraph StyledGraph {
            |  A [shape=box, color=red];
            |  B [shape=ellipse, color=blue];
            |  A -> B [style=dashed, color=black];
            |  B -> A [style=bold, color=orange];
            |}
            |""".trimMargin(),
            g.toDot(),
        )
    }

    @Test
    fun unnamedUndirectedGraphUsesDoubleDash() {
        val g = graph {
            node("A")
            node("B")
            edge("A", "B")
        }

        assertEquals(
            """
            |graph {
            |  A;
            |  B;
            |  A -- B;
            |}
            |""".trimMargin(),
            g.toDot(),
        )
    }

    @Test
    fun emptyGraph() {
        assertEquals("graph {\n}\n", graph {}.toDot())
    }

    @Test
    fun labelsAndNonIdentifierNamesAreQuoted() {
        val g = digraph("my graph") {
            node("node 1") {
                label = "Hello World"
            }
            edge("node 1", "B") {
                label = "goes to"
            }
        }

        assertEquals(
            """
            |digraph "my graph" {
            |  "node 1" [label="Hello World"];
            |  "node 1" -> B [label="goes to"];
            |}
            |""".trimMargin(),
            g.toDot(),
        )
    }

    @Test
    fun quotesInsideValuesAreEscaped() {
        val g = graph {
            node("A") {
                label = "say \"hi\""
            }
        }

        assertEquals(
            """
            |graph {
            |  A [label="say \"hi\""];
            |}
            |""".trimMargin(),
            g.toDot(),
        )
    }

    @Test
    fun writesGraphToSinkAndAppendable() {
        val g = digraph("G") {
            node("A")
            edge("A", "A")
        }

        val buffer = Buffer()
        g.writeDotTo(buffer)
        assertEquals(g.toDot(), buffer.readString())

        val sb = StringBuilder()
        g.writeDotTo(sb)
        assertEquals(g.toDot(), sb.toString())
    }

    @Test
    fun flowChunksConcatenateToStringOutput() = runTest {
        val g = digraph("G") {
            node("A") { shape = "box" }
            node("B")
            edge("A", "B")
        }

        assertEquals(g.toDot(), g.dotFlow().toList().joinToString(""))
    }

    @Test
    fun flowEmitsHeaderNodesEdgesEpilogue() = runTest {
        val g = digraph("G") {
            node("A")
            node("B")
            edge("A", "B")
        }

        val chunks = g.dotFlow().toList()
        assertEquals(5, chunks.size)
        assertEquals("digraph G {\n", chunks[0])
        assertEquals("  A;\n", chunks[1])
        assertEquals("  B;\n", chunks[2])
        assertEquals("  A -> B;\n", chunks[3])
        assertEquals("}\n", chunks[4])
    }
}
