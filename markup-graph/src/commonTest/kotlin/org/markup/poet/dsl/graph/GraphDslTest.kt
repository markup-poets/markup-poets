package org.markup.poet.dsl.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphDslTest {

    @Test
    fun undirectedGraphWithPlainNodes() {
        val g = graph("G") {
            node("A")
            node("B")
            edge("A", "B")
        }

        assertEquals("G", g.name)
        assertFalse(g.directed)
        assertEquals(listOf(Node("A"), Node("B")), g.nodes)
        assertEquals(listOf(Edge("A", "B")), g.edges)
    }

    @Test
    fun digraphIsDirected() {
        val g = digraph {
            edge("A", "B")
        }

        assertNull(g.name)
        assertTrue(g.directed)
    }

    @Test
    fun nodeStyleIsCapturedAndPlainNodeHasNoStyle() {
        val g = graph {
            node("A") {
                shape = "box"
                color = "red"
                label = "Start"
            }
            node("B")
        }

        assertEquals(NodeStyle(shape = "box", color = "red", label = "Start"), g.nodes[0].style)
        assertNull(g.nodes[1].style)
    }

    @Test
    fun edgeStyleIsCapturedAndPlainEdgeHasNoStyle() {
        val g = digraph {
            edge("A", "B") {
                style = "dashed"
                color = "black"
                label = "next"
            }
            edge("B", "C")
        }

        assertEquals(EdgeStyle(style = "dashed", color = "black", label = "next"), g.edges[0].style)
        assertNull(g.edges[1].style)
    }

    @Test
    fun attrsAreCapturedAtAllLevels() {
        val g = digraph {
            attr("rankdir", "LR")
            node("A") {
                shape = "box"
                attr("penwidth", "2")
            }
            edge("A", "B") {
                attr("weight", "3")
            }
        }

        assertEquals(mapOf("rankdir" to "LR"), g.attrs)
        assertEquals(mapOf("penwidth" to "2"), g.nodes[0].attrs)
        assertEquals(NodeStyle(shape = "box"), g.nodes[0].style)
        assertEquals(mapOf("weight" to "3"), g.edges[0].attrs)
        assertNull(g.edges[0].style)
    }

    @Test
    fun edgesMayReferenceUndeclaredNodes() {
        val g = digraph {
            edge("X", "Y")
        }

        assertEquals(0, g.nodes.size)
        assertEquals(listOf(Edge("X", "Y")), g.edges)
    }
}
