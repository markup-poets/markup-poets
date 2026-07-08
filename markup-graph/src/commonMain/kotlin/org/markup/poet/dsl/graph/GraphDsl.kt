package org.markup.poet.dsl.graph

@DslMarker
annotation class GraphDsl

/**
 * Standalone entry point of the graph DSL: an undirected graph.
 *
 * ```kotlin
 * val g = graph("MyGraph") {
 *     node("A") { shape = "box" }
 *     node("B")
 *     edge("A", "B") { style = "dashed" }
 * }
 * ```
 */
fun graph(name: String? = null, content: GraphScope.() -> Unit): Graph =
    GraphBuilder(name = name, directed = false).apply(content).build()

/**
 * Standalone entry point of the graph DSL: a directed graph.
 */
fun digraph(name: String? = null, content: GraphScope.() -> Unit): Graph =
    GraphBuilder(name = name, directed = true).apply(content).build()

@GraphDsl
interface GraphScope {
    fun node(id: String, style: NodeScope.() -> Unit = {})

    /** Adds an edge by node ids; the ids need not be declared via [node]. */
    fun edge(from: String, to: String, style: EdgeScope.() -> Unit = {})
}

@GraphDsl
interface NodeScope {
    var shape: String?
    var color: String?
    var label: String?
}

@GraphDsl
interface EdgeScope {
    var style: String?
    var color: String?
    var label: String?
}

private class GraphBuilder(private val name: String?, private val directed: Boolean) : GraphScope {
    private val nodes = mutableListOf<Node>()
    private val edges = mutableListOf<Edge>()

    override fun node(id: String, style: NodeScope.() -> Unit) {
        nodes.add(Node(id = id, style = NodeStyleBuilder().apply(style).build()))
    }

    override fun edge(from: String, to: String, style: EdgeScope.() -> Unit) {
        edges.add(Edge(from = from, to = to, style = EdgeStyleBuilder().apply(style).build()))
    }

    fun build() = Graph(name = name, directed = directed, nodes = nodes.toList(), edges = edges.toList())
}

private class NodeStyleBuilder : NodeScope {
    override var shape: String? = null
    override var color: String? = null
    override var label: String? = null

    fun build(): NodeStyle? =
        if (shape == null && color == null && label == null) null
        else NodeStyle(shape = shape, color = color, label = label)
}

private class EdgeStyleBuilder : EdgeScope {
    override var style: String? = null
    override var color: String? = null
    override var label: String? = null

    fun build(): EdgeStyle? =
        if (style == null && color == null && label == null) null
        else EdgeStyle(style = style, color = color, label = label)
}
