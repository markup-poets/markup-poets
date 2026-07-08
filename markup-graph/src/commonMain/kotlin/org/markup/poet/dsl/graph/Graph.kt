package org.markup.poet.dsl.graph

/**
 * A graph: nodes and edges, directed or undirected, with optional
 * graph-level attributes (e.g. `rankdir` for DOT layouts).
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
    val attrs: Map<String, String> = emptyMap(),
)

data class Node(
    val id: String,
    val style: NodeStyle? = null,
    val attrs: Map<String, String> = emptyMap(),
)

data class Edge(
    val from: String,
    val to: String,
    val style: EdgeStyle? = null,
    val attrs: Map<String, String> = emptyMap(),
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
