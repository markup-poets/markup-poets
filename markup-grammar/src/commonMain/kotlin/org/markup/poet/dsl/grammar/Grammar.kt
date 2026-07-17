package org.markup.poet.dsl.grammar

/**
 * A grammar: an ordered list of named production rules.
 *
 * The model is notation-agnostic; writer modules (e.g. markup-bnf-writer)
 * turn it into concrete syntax. Rules reference each other by name; a [Ref]
 * may name a rule with no matching [Rule] entry (undefined nonterminal), in
 * the same spirit as an edge referencing an undeclared node in markup-graph.
 */
data class Grammar(
    val name: String? = null,
    val rules: List<Rule>,
)

/**
 * A single production: [name] is defined as [expr].
 *
 * [comment] is emitted only by notations that have a comment syntax; classic
 * BNF has none and drops it.
 */
data class Rule(
    val name: String,
    val expr: Expr,
    val comment: String? = null,
)

/**
 * The right-hand side of a production.
 *
 * There is deliberately no grouping node: grouping is a printing concern, not
 * a semantic one, so writers parenthesize by precedence ([Choice] binds looser
 * than [Seq], which binds looser than an atom). A group node would make two
 * structurally identical grammars compare unequal.
 */
sealed interface Expr

/** The empty string (ε) — matches without consuming input. */
data object Empty : Expr

/**
 * A literal terminal, as exact text.
 *
 * The text is matched case-sensitively. Notations whose string literals are
 * case-insensitive (ABNF) must say so explicitly in their output.
 */
data class Terminal(val text: String) : Expr

/** A reference to a rule by name (a nonterminal). */
data class Ref(val name: String) : Expr

/**
 * An inclusive range of Unicode code points, e.g. `Range('A'.code, 'Z'.code)`.
 *
 * Native only in ABNF (`%x41-5A`); notations without range syntax expand it
 * into a choice of single-character terminals.
 */
data class Range(val from: Int, val to: Int) : Expr

/** Concatenation, in order. */
data class Seq(val items: List<Expr>) : Expr

/** Alternation, in order. */
data class Choice(val alternatives: List<Expr>) : Expr

/**
 * Zero or one occurrence of [item].
 *
 * Semantically `Repeat(item, 0, 1)`, kept as its own node because all three
 * BNF-family notations have a dedicated optional syntax (`[x]`) that reads
 * better than their bounded-repetition form.
 */
data class Optional(val item: Expr) : Expr

/**
 * Repetition of [item], at least [min] and at most [max] times, where a `max`
 * of `null` means unbounded.
 *
 * `Repeat(x)` is zero-or-more, `Repeat(x, min = 1)` is one-or-more, and
 * `Repeat(x, 2, 4)` is two-to-four — covering every ABNF repetition form
 * (`*x`, `1*x`, `2*4x`, `3x`) in one node.
 */
data class Repeat(val item: Expr, val min: Int = 0, val max: Int? = null) : Expr
