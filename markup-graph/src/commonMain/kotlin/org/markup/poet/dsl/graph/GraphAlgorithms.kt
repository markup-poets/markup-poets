package org.markup.poet.dsl.graph

/**
 * All node ids in the graph: declared [Graph.nodes] plus ids referenced
 * only by edges (implicit nodes), in first-appearance order.
 */
val Graph.nodeIds: Set<String>
    get() = buildSet {
        nodes.forEach { add(it.id) }
        edges.forEach {
            add(it.from)
            add(it.to)
        }
    }

/**
 * Topologically sorts a directed graph using Kahn's algorithm.
 *
 * Returns the node ids in an order where every edge points from an earlier
 * to a later entry, or `null` if the graph contains a cycle. Nodes with
 * equal precedence keep declaration order, so the result is deterministic.
 *
 * @throws IllegalArgumentException if the graph is undirected
 */
fun Graph.topologicalSortOrNull(): List<String>? {
    require(directed) { "Topological sort requires a directed graph" }
    val ids = nodeIds
    val indegree = ids.associateWithTo(linkedMapOf()) { 0 }
    val adjacency = mutableMapOf<String, MutableList<String>>()
    edges.forEach { edge ->
        adjacency.getOrPut(edge.from) { mutableListOf() }.add(edge.to)
        indegree[edge.to] = indegree.getValue(edge.to) + 1
    }

    val queue = ArrayDeque(ids.filter { indegree.getValue(it) == 0 })
    val result = mutableListOf<String>()
    while (queue.isNotEmpty()) {
        val id = queue.removeFirst()
        result.add(id)
        adjacency[id]?.forEach { next ->
            val remaining = indegree.getValue(next) - 1
            indegree[next] = remaining
            if (remaining == 0) queue.add(next)
        }
    }

    return if (result.size == ids.size) result else null
}

/**
 * Whether a directed graph is acyclic (a DAG).
 *
 * @throws IllegalArgumentException if the graph is undirected
 */
fun Graph.isAcyclic(): Boolean = topologicalSortOrNull() != null
