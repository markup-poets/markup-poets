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
    /** Sets a graph-level attribute, e.g. `attr("rankdir", "LR")`. */
    fun attr(name: String, value: String)

    fun node(id: String, style: NodeScope.() -> Unit = {})

    /** Adds an edge by node ids; the ids need not be declared via [node]. */
    fun edge(from: String, to: String, style: EdgeScope.() -> Unit = {})
}

@GraphDsl
interface NodeScope {
    var shape: String?
    var color: String?
    var label: String?

    /** Sets an arbitrary attribute beyond the typed ones, e.g. `attr("penwidth", "2")`. */
    fun attr(name: String, value: String)
}

@GraphDsl
interface EdgeScope {
    var style: String?
    var color: String?
    var label: String?

    /** Sets an arbitrary attribute beyond the typed ones, e.g. `attr("weight", "3")`. */
    fun attr(name: String, value: String)
}

private class GraphBuilder(private val name: String?, private val directed: Boolean) : GraphScope {
    private val attrs = linkedMapOf<String, String>()
    private val nodes = mutableListOf<Node>()
    private val edges = mutableListOf<Edge>()

    override fun attr(name: String, value: String) {
        attrs[name] = value
    }

    override fun node(id: String, style: NodeScope.() -> Unit) {
        val builder = NodeStyleBuilder().apply(style)
        nodes.add(Node(id = id, style = builder.buildStyle(), attrs = builder.buildAttrs()))
    }

    override fun edge(from: String, to: String, style: EdgeScope.() -> Unit) {
        val builder = EdgeStyleBuilder().apply(style)
        edges.add(Edge(from = from, to = to, style = builder.buildStyle(), attrs = builder.buildAttrs()))
    }

    fun build() = Graph(
        name = name,
        directed = directed,
        nodes = nodes.toList(),
        edges = edges.toList(),
        attrs = attrs.toMap(),
    )
}

private class NodeStyleBuilder : NodeScope {
    override var shape: String? = null
    override var color: String? = null
    override var label: String? = null
    private val attrs = linkedMapOf<String, String>()

    override fun attr(name: String, value: String) {
        attrs[name] = value
    }

    fun buildStyle(): NodeStyle? =
        if (shape == null && color == null && label == null) null
        else NodeStyle(shape = shape, color = color, label = label)

    fun buildAttrs(): Map<String, String> = attrs.toMap()
}

private class EdgeStyleBuilder : EdgeScope {
    override var style: String? = null
    override var color: String? = null
    override var label: String? = null
    private val attrs = linkedMapOf<String, String>()

    override fun attr(name: String, value: String) {
        attrs[name] = value
    }

    fun buildStyle(): EdgeStyle? =
        if (style == null && color == null && label == null) null
        else EdgeStyle(style = style, color = color, label = label)

    fun buildAttrs(): Map<String, String> = attrs.toMap()
}
