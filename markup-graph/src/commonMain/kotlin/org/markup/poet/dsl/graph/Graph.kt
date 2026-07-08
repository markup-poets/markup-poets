package org.markup.poet.dsl.graph

/**
 * A graph: nodes and edges, directed or undirected.
 *
 * The model is format-agnostic; writer modules (e.g. markup-dot-writer)
 * turn it into concrete syntax. Edges reference nodes by id; an edge may
 * reference ids with no matching [Node] entry (implicit nodes, as in DOT).
 */
data class Graph(
    val name: String?,
    val directed: Boolean,
    val nodes: List<Node>,
    val edges: List<Edge>,
)

data class Node(
    val id: String,
    val style: NodeStyle? = null,
)

data class Edge(
    val from: String,
    val to: String,
    val style: EdgeStyle? = null,
)

data class NodeStyle(
    val shape: String? = null,
    val color: String? = null,
    val label: String? = null,
)

data class EdgeStyle(
    val style: String? = null,
    val color: String? = null,
    val label: String? = null,
)
