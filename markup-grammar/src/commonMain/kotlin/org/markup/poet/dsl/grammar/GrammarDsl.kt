package org.markup.poet.dsl.grammar

@DslMarker
annotation class GrammarDsl

/**
 * Standalone entry point of the grammar DSL.
 *
 * ```kotlin
 * val g = grammar("Arith") {
 *     rule("expr") {
 *         ref("term")
 *         zeroOrMore {
 *             choice {
 *                 +"+"
 *                 +"-"
 *             }
 *             ref("term")
 *         }
 *     }
 *     rule("digit") { range('0', '9') }
 * }
 * ```
 */
fun grammar(name: String? = null, content: GrammarScope.() -> Unit): Grammar =
    GrammarBuilder(name).apply(content).build()

@GrammarDsl
interface GrammarScope {
    /** Adds a production. The body is a concatenation of its elements. */
    fun rule(name: String, comment: String? = null, content: SeqScope.() -> Unit)
}

/** Builds a concatenation; elements are appended in call order. */
@GrammarDsl
interface SeqScope {
    /** Adds a literal terminal, e.g. `+"while"`. */
    operator fun String.unaryPlus()

    /** Adds a reference to the rule [name]; the rule need not be declared. */
    fun ref(name: String)

    /** Adds an inclusive character range, e.g. `range('a', 'z')`. */
    fun range(from: Char, to: Char)

    /** Adds an inclusive code-point range, e.g. `range(0x41, 0x5A)`. */
    fun range(from: Int, to: Int)

    /** Adds the empty string (ε). */
    fun empty()

    fun choice(content: ChoiceScope.() -> Unit)

    fun optional(content: SeqScope.() -> Unit)

    fun zeroOrMore(content: SeqScope.() -> Unit)

    fun oneOrMore(content: SeqScope.() -> Unit)

    /** Adds a bounded repetition; [max] of `null` means unbounded. */
    fun repeat(min: Int = 0, max: Int? = null, content: SeqScope.() -> Unit)
}

/** Builds an alternation; each alternative is added in call order. */
@GrammarDsl
interface ChoiceScope {
    /** Adds an alternative built as a concatenation. */
    fun alt(content: SeqScope.() -> Unit)

    /** Shorthand for a single-terminal alternative, e.g. `+"+"`. */
    operator fun String.unaryPlus()

    /** Shorthand for a single-reference alternative. */
    fun ref(name: String)
}

private class GrammarBuilder(private val name: String?) : GrammarScope {
    private val rules = mutableListOf<Rule>()

    override fun rule(name: String, comment: String?, content: SeqScope.() -> Unit) {
        rules.add(Rule(name = name, expr = SeqBuilder().apply(content).build(), comment = comment))
    }

    fun build() = Grammar(name = name, rules = rules.toList())
}

private class SeqBuilder : SeqScope {
    private val items = mutableListOf<Expr>()

    override fun String.unaryPlus() {
        items.add(Terminal(this))
    }

    override fun ref(name: String) {
        items.add(Ref(name))
    }

    override fun range(from: Char, to: Char) {
        items.add(Range(from.code, to.code))
    }

    override fun range(from: Int, to: Int) {
        items.add(Range(from, to))
    }

    override fun empty() {
        items.add(Empty)
    }

    override fun choice(content: ChoiceScope.() -> Unit) {
        items.add(ChoiceBuilder().apply(content).build())
    }

    override fun optional(content: SeqScope.() -> Unit) {
        items.add(Optional(SeqBuilder().apply(content).build()))
    }

    override fun zeroOrMore(content: SeqScope.() -> Unit) {
        items.add(Repeat(SeqBuilder().apply(content).build(), min = 0, max = null))
    }

    override fun oneOrMore(content: SeqScope.() -> Unit) {
        items.add(Repeat(SeqBuilder().apply(content).build(), min = 1, max = null))
    }

    override fun repeat(min: Int, max: Int?, content: SeqScope.() -> Unit) {
        items.add(Repeat(SeqBuilder().apply(content).build(), min = min, max = max))
    }

    /** Collapses to the sole element when there is one, and to [Empty] when there are none. */
    fun build(): Expr = when (items.size) {
        0 -> Empty
        1 -> items[0]
        else -> Seq(items.toList())
    }
}

private class ChoiceBuilder : ChoiceScope {
    private val alternatives = mutableListOf<Expr>()

    override fun alt(content: SeqScope.() -> Unit) {
        alternatives.add(SeqBuilder().apply(content).build())
    }

    override fun String.unaryPlus() {
        alternatives.add(Terminal(this))
    }

    override fun ref(name: String) {
        alternatives.add(Ref(name))
    }

    /** Collapses to the sole alternative when there is one. */
    fun build(): Expr = when (alternatives.size) {
        0 -> Empty
        1 -> alternatives[0]
        else -> Choice(alternatives.toList())
    }
}
