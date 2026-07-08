package org.markup.poet.dsl.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphAlgorithmsTest {

    @Test
    fun topologicalSortOrdersDag() {
        val g = digraph {
            edge("build", "test")
            edge("test", "deploy")
            edge("build", "lint")
        }

        assertEquals(listOf("build", "test", "lint", "deploy"), g.topologicalSortOrNull())
        assertTrue(g.isAcyclic())
    }

    @Test
    fun cyclicGraphReturnsNull() {
        val g = digraph {
            edge("a", "b")
            edge("b", "c")
            edge("c", "a")
        }

        assertNull(g.topologicalSortOrNull())
        assertFalse(g.isAcyclic())
    }

    @Test
    fun selfLoopIsCyclic() {
        val g = digraph {
            edge("a", "a")
        }

        assertFalse(g.isAcyclic())
    }

    @Test
    fun disconnectedNodesKeepDeclarationOrder() {
        val g = digraph {
            node("z")
            node("a")
            edge("a", "b")
        }

        assertEquals(listOf("z", "a", "b"), g.topologicalSortOrNull())
    }

    @Test
    fun nodeIdsIncludeImplicitEdgeNodes() {
        val g = digraph {
            node("a")
            edge("a", "b")
            edge("c", "b")
        }

        assertEquals(setOf("a", "b", "c"), g.nodeIds)
    }

    @Test
    fun emptyGraphIsAcyclic() {
        assertEquals(emptyList(), digraph {}.topologicalSortOrNull())
        assertTrue(digraph {}.isAcyclic())
    }

    @Test
    fun undirectedGraphIsRejected() {
        val g = graph {
            edge("a", "b")
        }

        assertFailsWith<IllegalArgumentException> { g.topologicalSortOrNull() }
        assertFailsWith<IllegalArgumentException> { g.isAcyclic() }
    }
}
