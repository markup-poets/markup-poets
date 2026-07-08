package org.markup.poet.dsl.write.dot

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.io.Sink
import kotlinx.io.writeString
import org.markup.poet.dsl.graph.Edge
import org.markup.poet.dsl.graph.Graph
import org.markup.poet.dsl.graph.Node

/**
 * Writes the graph as Graphviz DOT source text.
 */
fun Graph.toDot(): String = dotChunks().joinToString("")

/**
 * Writes the graph as DOT source text into [out].
 */
fun Graph.writeDotTo(out: Appendable) {
    dotChunks().forEach { out.append(it) }
}

/**
 * Writes the graph as DOT source text into [sink], UTF-8 encoded.
 * The sink is neither flushed nor closed; the caller owns its lifecycle.
 */
fun Graph.writeDotTo(sink: Sink) {
    dotChunks().forEach { sink.writeString(it) }
}

/**
 * The graph as a cold [Flow] of DOT source-text chunks.
 *
 * Chunks arrive in order and concatenating all emissions yields exactly
 * [toDot]. Chunk boundaries are an implementation detail; currently the
 * `graph {`/`digraph {` header is one chunk, then one chunk per node,
 * one per edge, then the closing `}`.
 */
fun Graph.dotFlow(): Flow<String> = dotChunks().asFlow()

private fun Graph.dotChunks(): Sequence<String> = sequence {
    yield(
        buildString {
            append(if (directed) "digraph" else "graph")
            name?.let { append(' ').append(dotId(it)) }
            append(" {\n")
        }
    )
    nodes.forEach { node ->
        yield(buildString { renderNode(node, this) })
    }
    edges.forEach { edge ->
        yield(buildString { renderEdge(edge, directed, this) })
    }
    yield("}\n")
}

private fun renderNode(node: Node, out: Appendable) {
    out.append("  ").append(dotId(node.id))
    node.style?.let { style ->
        val attributes = listOfNotNull(
            style.shape?.let { "shape=${dotId(it)}" },
            style.color?.let { "color=${dotId(it)}" },
            style.label?.let { "label=${dotId(it)}" },
        )
        out.append(" [").append(attributes.joinToString(", ")).append(']')
    }
    out.append(";\n")
}

private fun renderEdge(edge: Edge, directed: Boolean, out: Appendable) {
    out.append("  ").append(dotId(edge.from))
        .append(if (directed) " -> " else " -- ")
        .append(dotId(edge.to))
    edge.style?.let { style ->
        val attributes = listOfNotNull(
            style.style?.let { "style=${dotId(it)}" },
            style.color?.let { "color=${dotId(it)}" },
            style.label?.let { "label=${dotId(it)}" },
        )
        out.append(" [").append(attributes.joinToString(", ")).append(']')
    }
    out.append(";\n")
}

private val BARE_ID = Regex("[A-Za-z_][A-Za-z0-9_]*|-?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)")

/**
 * Renders a DOT identifier: bare when it is a valid DOT ID
 * (alphanumeric/underscore not starting with a digit, or a numeral),
 * double-quoted with escaping otherwise.
 */
private fun dotId(id: String): String =
    if (BARE_ID.matches(id)) id
    else "\"" + id.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
