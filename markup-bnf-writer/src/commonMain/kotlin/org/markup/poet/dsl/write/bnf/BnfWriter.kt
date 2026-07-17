package org.markup.poet.dsl.write.bnf

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.io.Sink
import kotlinx.io.writeString
import org.markup.poet.dsl.grammar.Choice
import org.markup.poet.dsl.grammar.Empty
import org.markup.poet.dsl.grammar.Expr
import org.markup.poet.dsl.grammar.Grammar
import org.markup.poet.dsl.grammar.Optional
import org.markup.poet.dsl.grammar.Range
import org.markup.poet.dsl.grammar.Ref
import org.markup.poet.dsl.grammar.Repeat
import org.markup.poet.dsl.grammar.Rule
import org.markup.poet.dsl.grammar.Seq
import org.markup.poet.dsl.grammar.Terminal

/*
 * BNF-family mapping notes:
 * - one model, three notations ([BnfFlavor]); the flavor is a writer parameter,
 *   never part of the Grammar model
 * - EBNF is the default: it maps 1:1 to the model and desugars nothing
 * - classic BNF has no optional/repetition/grouping syntax, so Optional, Repeat and
 *   nested Choice are desugared into synthetic rules named <rule-opt>, <rule-rep>,
 *   <rule-alt>, emitted after their originating rule. BNF is the only flavor whose
 *   emitted rule count differs from the model's
 * - Grammar.name and Rule.comment are dropped in BNF (no comment syntax), and emitted
 *   as (* ... *) in EBNF and ; ... in ABNF
 * - Range is native only in ABNF (%x41-5A); BNF and EBNF expand it to a choice of
 *   single-character terminals, and reject ranges wider than 256 code points
 * - ABNF quoted strings are case-insensitive (RFC 5234), so terminals containing ASCII
 *   letters are emitted case-sensitive as %s"..." (RFC 7405)
 * - terminals containing both quote characters are split into a concatenation of quoted
 *   runs; no notation in this family has a string escape
 * - rule names are emitted verbatim; names containing punctuation that would break the
 *   notation structurally are rejected with IllegalArgumentException. ISO 14977 permits
 *   only letters, digits and spaces in a meta identifier; hyphens are emitted anyway,
 *   since every EBNF tool in practice accepts them
 */

/** The notation emitted by the BNF writer. */
enum class BnfFlavor {
    /** Classic BNF: `<a> ::= <b> | "c"`. Optionals and repetitions become synthetic rules. */
    BNF,

    /** ISO 14977 EBNF: `a = b | "c";`. Maps 1:1 to the model. */
    EBNF,

    /** ABNF (RFC 5234, with RFC 7405 case-sensitive strings): `a = b / %s"c"`. */
    ABNF,
}

/**
 * Writes the grammar as BNF-family source text in [flavor].
 */
fun Grammar.toBnf(flavor: BnfFlavor = BnfFlavor.EBNF): String =
    bnfChunks(flavor).joinToString("")

/**
 * Writes the grammar as BNF-family source text in [flavor] into [out].
 */
fun Grammar.writeBnfTo(out: Appendable, flavor: BnfFlavor = BnfFlavor.EBNF) {
    bnfChunks(flavor).forEach { out.append(it) }
}

/**
 * Writes the grammar as BNF-family source text in [flavor] into [sink], UTF-8 encoded.
 * The sink is neither flushed nor closed; the caller owns its lifecycle.
 */
fun Grammar.writeBnfTo(sink: Sink, flavor: BnfFlavor = BnfFlavor.EBNF) {
    bnfChunks(flavor).forEach { sink.writeString(it) }
}

/**
 * The grammar as a cold [Flow] of source-text chunks in [flavor].
 *
 * Chunks arrive in order and concatenating all emissions yields exactly
 * [toBnf] for the same flavor. Chunk boundaries are an implementation detail;
 * currently the grammar-name comment is one chunk when present, then one chunk
 * per emitted rule — which in [BnfFlavor.BNF] includes the synthetic rules
 * produced by desugaring.
 */
fun Grammar.bnfFlow(flavor: BnfFlavor = BnfFlavor.EBNF): Flow<String> =
    bnfChunks(flavor).asFlow()

private fun Grammar.bnfChunks(flavor: BnfFlavor): Sequence<String> = sequence {
    val syntax = syntaxFor(flavor)
    name?.let { commentLine(it, syntax)?.let { line -> yield(line) } }
    val declared = rules.map { it.name }.toSet()
    rules.forEach { rule ->
        if (flavor == BnfFlavor.BNF) {
            val desugarer = Desugarer(declared)
            val expr = desugarer.desugarTop(rule.name, rule.expr)
            yield(renderRule(rule.name, expr, rule.comment, syntax, flavor))
            desugarer.synthetics().forEach { synthetic ->
                yield(renderRule(synthetic.name, synthetic.expr, null, syntax, flavor))
            }
        } else {
            yield(renderRule(rule.name, rule.expr, rule.comment, syntax, flavor))
        }
    }
}

/** The punctuation that distinguishes one notation in the family from another. */
private data class Syntax(
    val defines: String,
    val alternate: String,
    val concat: String,
    val terminator: String,
    val commentOpen: String?,
    val commentClose: String,
)

private fun syntaxFor(flavor: BnfFlavor): Syntax = when (flavor) {
    BnfFlavor.BNF -> Syntax("::=", " | ", " ", "", null, "")
    BnfFlavor.EBNF -> Syntax("=", " | ", ", ", ";", "(*", "*)")
    BnfFlavor.ABNF -> Syntax("=", " / ", " ", "", ";", "")
}

private fun commentLine(text: String, syntax: Syntax): String? {
    val open = syntax.commentOpen ?: return null
    return buildString {
        append(open).append(' ').append(text)
        if (syntax.commentClose.isNotEmpty()) append(' ').append(syntax.commentClose)
        append('\n')
    }
}

private fun renderRule(
    name: String,
    expr: Expr,
    comment: String?,
    syntax: Syntax,
    flavor: BnfFlavor,
): String = buildString {
    comment?.let { commentLine(it, syntax)?.let { line -> append(line) } }
    append(ruleName(name, flavor))
    append(' ').append(syntax.defines).append(' ')
    append(render(expr, syntax, flavor, P_CHOICE))
    append(syntax.terminator)
    append('\n')
}

// Precedence: a choice binds looser than a concatenation, which binds looser than an atom.
private const val P_CHOICE = 0
private const val P_SEQ = 1
private const val P_ATOM = 2

private fun render(expr0: Expr, syntax: Syntax, flavor: BnfFlavor, minPrec: Int): String {
    // A range outside ABNF has no native syntax and becomes a choice of characters,
    // so it must be re-precedenced as whatever it expands to.
    val expr = if (expr0 is Range && flavor != BnfFlavor.ABNF) expandRange(expr0) else expr0

    val runs = if (expr is Terminal) terminalRuns(expr.text, flavor) else null
    val prec = when {
        runs != null -> if (runs.size > 1) P_SEQ else P_ATOM
        expr is Choice -> P_CHOICE
        expr is Seq -> P_SEQ
        else -> P_ATOM
    }

    val text = when (expr) {
        is Empty -> EMPTY_STRING
        is Terminal -> runs!!.joinToString(syntax.concat)
        is Ref -> ruleName(expr.name, flavor)
        is Range -> abnfRange(expr)
        is Seq -> expr.items.joinToString(syntax.concat) { render(it, syntax, flavor, P_SEQ) }
        is Choice -> expr.alternatives.joinToString(syntax.alternate) { render(it, syntax, flavor, P_SEQ) }
        is Optional -> renderOptional(expr, syntax, flavor)
        is Repeat -> renderRepeat(expr, syntax, flavor)
    }

    return if (prec < minPrec) "($text)" else text
}

private const val EMPTY_STRING = "\"\""

private fun renderOptional(expr: Optional, syntax: Syntax, flavor: BnfFlavor): String = when (flavor) {
    // Brackets delimit, so the body needs no parentheses of its own.
    BnfFlavor.EBNF, BnfFlavor.ABNF -> "[" + render(expr.item, syntax, flavor, P_CHOICE) + "]"
    BnfFlavor.BNF -> error("BNF desugars Optional into a synthetic rule before rendering")
}

private fun renderRepeat(expr: Repeat, syntax: Syntax, flavor: BnfFlavor): String {
    require(expr.min >= 0) { "Repeat.min must not be negative: ${expr.min}" }
    expr.max?.let {
        require(it >= expr.min) { "Repeat.max (${expr.max}) must not be less than min (${expr.min})" }
    }
    return when (flavor) {
        BnfFlavor.ABNF -> renderAbnfRepeat(expr, syntax, flavor)
        BnfFlavor.EBNF -> renderEbnfRepeat(expr, syntax, flavor)
        BnfFlavor.BNF -> error("BNF desugars Repeat into a synthetic rule before rendering")
    }
}

private fun renderAbnfRepeat(expr: Repeat, syntax: Syntax, flavor: BnfFlavor): String {
    val item = render(expr.item, syntax, flavor, P_ATOM)
    val min = expr.min
    val max = expr.max
    return when {
        max == null && min == 0 -> "*$item"
        max == null -> "$min*$item"
        max == 0 -> EMPTY_STRING
        min == max -> "$min$item"
        min == 0 -> "*$max$item"
        else -> "$min*$max$item"
    }
}

private fun renderEbnfRepeat(expr: Repeat, syntax: Syntax, flavor: BnfFlavor): String {
    val min = expr.min
    val max = expr.max
    if (max == 0) return EMPTY_STRING

    // `{ x }` is ISO 14977's zero-or-more; a mandatory prefix is spelled out before it,
    // and a bounded tail becomes nested optionals — the notation has no min/max form.
    val repetition = { "{" + render(expr.item, syntax, flavor, P_CHOICE) + "}" }
    val prefix = { count: Int ->
        val atom = render(expr.item, syntax, flavor, P_ATOM)
        if (count == 1) atom else "$count * $atom"
    }

    if (max == null) {
        return when (min) {
            0 -> repetition()
            else -> prefix(min) + syntax.concat + repetition()
        }
    }
    if (min == max) return prefix(min)

    val tail = nestedOptionals(expr.item, max - min, syntax, flavor)
    return if (min == 0) tail else prefix(min) + syntax.concat + tail
}

/** `[x]`, `[x, [x]]`, `[x, [x, [x]]]`, ... — the EBNF idiom for an upper bound. */
private fun nestedOptionals(item: Expr, depth: Int, syntax: Syntax, flavor: BnfFlavor): String {
    val atom = render(item, syntax, flavor, P_SEQ)
    var result = ""
    repeat(depth) { level ->
        result = if (level == 0) "[$atom]" else "[$atom${syntax.concat}$result]"
    }
    return result
}

private fun ruleName(name: String, flavor: BnfFlavor): String {
    require(name.isNotEmpty()) { "Rule name must not be empty" }
    return when (flavor) {
        BnfFlavor.BNF -> {
            require('<' !in name && '>' !in name) {
                "BNF rule name must not contain angle brackets: '$name'"
            }
            "<$name>"
        }

        BnfFlavor.EBNF -> {
            require(name.none { it in EBNF_RESERVED }) {
                "EBNF meta identifier must not contain ISO 14977 punctuation ($EBNF_RESERVED): '$name'"
            }
            name
        }

        BnfFlavor.ABNF -> {
            require(name[0].isAsciiAlpha() && name.all { it.isAsciiAlpha() || it.isAsciiDigit() || it == '-' }) {
                "ABNF rulename must match ALPHA *(ALPHA / DIGIT / \"-\"): '$name'"
            }
            name
        }
    }
}

private const val EBNF_RESERVED = "=;|,()[]{}\"'"

private fun Char.isAsciiAlpha(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

/**
 * Splits a terminal into the quoted pieces that represent it. More than one piece
 * means the terminal could not be written as a single literal and is emitted as a
 * concatenation — no notation in this family has a string escape.
 */
private fun terminalRuns(text: String, flavor: BnfFlavor): List<String> = when (flavor) {
    BnfFlavor.BNF, BnfFlavor.EBNF -> quotedRuns(text)
    BnfFlavor.ABNF -> abnfRuns(text)
}

private fun quotedRuns(text: String): List<String> {
    if (text.isEmpty()) return listOf(EMPTY_STRING)
    if ('"' !in text) return listOf("\"$text\"")
    if ('\'' !in text) return listOf("'$text'")
    // Both quote characters appear: split into runs that each avoid one of them.
    return text.runsBy { it == '"' }.map { run ->
        if (run[0] == '"') "'$run'" else "\"$run\""
    }
}

private fun abnfRuns(text: String): List<String> {
    if (text.isEmpty()) return listOf(EMPTY_STRING)
    val runs = mutableListOf<String>()
    var current = mutableListOf<Int>()
    var quotable: Boolean? = null

    fun flush() {
        if (current.isEmpty()) return
        runs.add(if (quotable == true) abnfString(current) else abnfHex(current))
        current = mutableListOf()
    }

    text.toCodePoints().forEach { cp ->
        val isQuotable = cp.isAbnfQuotable()
        if (quotable != null && isQuotable != quotable) flush()
        quotable = isQuotable
        current.add(cp)
    }
    flush()
    return runs
}

/** ABNF quoted strings admit only `%x20-21 / %x23-7E` — printable ASCII, minus the quote. */
private fun Int.isAbnfQuotable(): Boolean = this in 0x20..0x21 || this in 0x23..0x7E

private fun abnfString(codePoints: List<Int>): String {
    val text = codePoints.joinToString("") { codePointToString(it) }
    // RFC 5234 quoted strings are case-insensitive, which the model's terminals are not.
    return if (text.any { it.isAsciiAlpha() }) "%s\"$text\"" else "\"$text\""
}

private fun abnfHex(codePoints: List<Int>): String =
    "%x" + codePoints.joinToString(".") { it.toHex() }

private fun abnfRange(range: Range): String {
    require(range.from <= range.to) {
        "Range.from (${range.from}) must not exceed Range.to (${range.to})"
    }
    return "%x${range.from.toHex()}-${range.to.toHex()}"
}

private fun Int.toHex(): String = toString(16).uppercase().padStart(2, '0')

private const val RANGE_EXPANSION_LIMIT = 256

/**
 * Expands a range into a choice of single-character terminals, for the notations
 * that have no range syntax. Capped: a wide range would otherwise silently produce
 * an unusably large grammar.
 */
private fun expandRange(range: Range): Expr {
    require(range.from <= range.to) {
        "Range.from (${range.from}) must not exceed Range.to (${range.to})"
    }
    val width = range.to - range.from + 1
    require(width <= RANGE_EXPANSION_LIMIT) {
        "Only ABNF has a native range syntax; BNF and EBNF expand ranges to a choice of " +
            "characters, which is capped at $RANGE_EXPANSION_LIMIT code points but this range " +
            "spans $width. Use BnfFlavor.ABNF, or write the rule by hand."
    }
    val terminals = (range.from..range.to).map { Terminal(codePointToString(it)) }
    return if (terminals.size == 1) terminals[0] else Choice(terminals)
}

/**
 * Rewrites a rule into classic BNF, which has no optional, repetition or grouping
 * syntax, collecting the synthetic rules the rewrite needs. Synthetic names derive
 * from the originating rule, with a counter appended on collision, so that output
 * is deterministic.
 */
private class Desugarer(private val taken: Set<String>) {
    private val generated = mutableListOf<Rule>()

    fun desugarTop(base: String, expr: Expr): Expr = desugar(base, expr, top = true)

    fun synthetics(): List<Rule> = generated.toList()

    private fun desugar(base: String, expr: Expr, top: Boolean): Expr = when (expr) {
        is Empty, is Terminal, is Ref -> expr
        is Range -> desugar(base, expandRange(expr), top)
        is Seq -> seqOf(expr.items.map { desugar(base, it, top = false) })
        // A choice is expressible in BNF only as a whole right-hand side, so a nested
        // one has to become a rule of its own.
        is Choice ->
            if (top) Choice(expr.alternatives.map { desugar(base, it, top = false) })
            else synth(base, "alt") { Choice(expr.alternatives.map { desugar(base, it, top = false) }) }

        is Optional -> synth(base, "opt") { Choice(listOf(desugar(base, expr.item, top = false), Empty)) }
        is Repeat -> desugarRepeat(base, expr)
    }

    private fun desugarRepeat(base: String, expr: Repeat): Expr {
        require(expr.min >= 0) { "Repeat.min must not be negative: ${expr.min}" }
        expr.max?.let {
            require(it >= expr.min) { "Repeat.max ($it) must not be less than min (${expr.min})" }
        }
        return when {
            expr.max == null && expr.min <= 1 -> synthRepetition(base, expr.item, optional = expr.min == 0)
            // A mandatory prefix, then an unbounded tail.
            expr.max == null ->
                desugar(base, seqOf(List(expr.min) { expr.item } + Repeat(expr.item, 0, null)), top = false)

            else -> desugar(base, boundedChoice(expr), top = false)
        }
    }

    /** `<r-rep> ::= x <r-rep> | ""` (or `| x` when at least one is required). */
    private fun synthRepetition(base: String, item: Expr, optional: Boolean): Expr =
        synth(base, "rep") { name ->
            val inner = desugar(base, item, top = false)
            Choice(listOf(seqOf(listOf(inner, Ref(name))), if (optional) Empty else inner))
        }

    /** A bounded repetition is a choice over each permitted count — linear in `max - min`. */
    private fun boundedChoice(expr: Repeat): Expr {
        val max = expr.max!!
        val alternatives = (expr.min..max).map { count ->
            if (count == 0) Empty else seqOf(List(count) { expr.item })
        }
        return if (alternatives.size == 1) alternatives[0] else Choice(alternatives)
    }

    private fun synth(base: String, suffix: String, body: (String) -> Expr): Ref {
        val name = fresh(base, suffix)
        // Reserve the slot before building the body, so nested synthetics see this name
        // as taken and land after their parent.
        val index = generated.size
        generated.add(Rule(name, Empty))
        generated[index] = Rule(name, body(name))
        return Ref(name)
    }

    private fun fresh(base: String, suffix: String): String {
        val candidate = "$base-$suffix"
        if (isFree(candidate)) return candidate
        var n = 2
        while (!isFree("$candidate-$n")) n++
        return "$candidate-$n"
    }

    private fun isFree(name: String): Boolean =
        name !in taken && generated.none { it.name == name }
}

private fun seqOf(items: List<Expr>): Expr {
    val flat = items.flatMap { if (it is Seq) it.items else listOf(it) }
    return when (flat.size) {
        0 -> Empty
        1 -> flat[0]
        else -> Seq(flat)
    }
}

private fun codePointToString(codePoint: Int): String =
    if (codePoint <= 0xFFFF) {
        codePoint.toChar().toString()
    } else {
        val v = codePoint - 0x10000
        charArrayOf(
            (0xD800 + (v shr 10)).toChar(),
            (0xDC00 + (v and 0x3FF)).toChar(),
        ).concatToString()
    }

/** Named to avoid shadowing `java.lang.String.codePoints` on the JVM target. */
private fun String.toCodePoints(): List<Int> {
    val out = mutableListOf<Int>()
    var i = 0
    while (i < length) {
        val c = this[i]
        val next = if (i + 1 < length) this[i + 1] else null
        if (c.isHighSurrogate() && next != null && next.isLowSurrogate()) {
            out.add(0x10000 + ((c.code - 0xD800) shl 10) + (next.code - 0xDC00))
            i += 2
        } else {
            out.add(c.code)
            i++
        }
    }
    return out
}

/** Splits into maximal runs of characters agreeing on [selector]. */
private fun String.runsBy(selector: (Char) -> Boolean): List<String> {
    val runs = mutableListOf<String>()
    var start = 0
    for (i in 1..length) {
        if (i == length || selector(this[i]) != selector(this[start])) {
            runs.add(substring(start, i))
            start = i
        }
    }
    return runs
}
