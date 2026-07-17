package org.markup.poet.dsl.write.bnf

import org.markup.poet.dsl.grammar.grammar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BnfWriterTest {

    /** One grammar, three notations — the clearest statement of what this writer does. */
    private val arithmetic = grammar {
        rule("expr") {
            ref("term")
            zeroOrMore {
                choice {
                    +"+"
                    +"-"
                }
                ref("term")
            }
        }
    }

    @Test
    fun arithmeticAsEbnfIsTheDefault() {
        assertEquals(
            """
            |expr = term, {("+" | "-"), term};
            |""".trimMargin(),
            arithmetic.toBnf(),
        )
        assertEquals(arithmetic.toBnf(BnfFlavor.EBNF), arithmetic.toBnf())
    }

    @Test
    fun arithmeticAsAbnf() {
        assertEquals(
            """
            |expr = term *(("+" / "-") term)
            |""".trimMargin(),
            arithmetic.toBnf(BnfFlavor.ABNF),
        )
    }

    @Test
    fun arithmeticAsClassicBnfDesugarsIntoSyntheticRules() {
        assertEquals(
            """
            |<expr> ::= <term> <expr-rep>
            |<expr-rep> ::= <expr-alt> <term> <expr-rep> | ""
            |<expr-alt> ::= "+" | "-"
            |""".trimMargin(),
            arithmetic.toBnf(BnfFlavor.BNF),
        )
    }

    @Test
    fun optionalIsNativeExceptInBnf() {
        val g = grammar {
            rule("stmt") {
                ref("head")
                optional { ref("tail") }
            }
        }

        assertEquals("stmt = head, [tail];\n", g.toBnf(BnfFlavor.EBNF))
        assertEquals("stmt = head [tail]\n", g.toBnf(BnfFlavor.ABNF))
        assertEquals(
            """
            |<stmt> ::= <head> <stmt-opt>
            |<stmt-opt> ::= <tail> | ""
            |""".trimMargin(),
            g.toBnf(BnfFlavor.BNF),
        )
    }

    @Test
    fun unboundedRepetitionPerFlavor() {
        val g = grammar {
            rule("zero") { zeroOrMore { ref("x") } }
            rule("one") { oneOrMore { ref("x") } }
            rule("two") { repeat(2) { ref("x") } }
        }

        assertEquals(
            """
            |zero = {x};
            |one = x, {x};
            |two = 2 * x, {x};
            |""".trimMargin(),
            g.toBnf(BnfFlavor.EBNF),
        )
        assertEquals(
            """
            |zero = *x
            |one = 1*x
            |two = 2*x
            |""".trimMargin(),
            g.toBnf(BnfFlavor.ABNF),
        )
    }

    @Test
    fun boundedRepetitionPerFlavor() {
        val g = grammar {
            rule("exact") { repeat(3, 3) { ref("x") } }
            rule("upTo") { repeat(0, 2) { ref("x") } }
            rule("between") { repeat(2, 4) { ref("x") } }
        }

        assertEquals(
            """
            |exact = 3 * x;
            |upTo = [x, [x]];
            |between = 2 * x, [x, [x]];
            |""".trimMargin(),
            g.toBnf(BnfFlavor.EBNF),
        )
        assertEquals(
            """
            |exact = 3x
            |upTo = *2x
            |between = 2*4x
            |""".trimMargin(),
            g.toBnf(BnfFlavor.ABNF),
        )
    }

    @Test
    fun boundedRepetitionInBnfBecomesAChoiceOverEachCount() {
        val g = grammar {
            rule("pair") { repeat(1, 2) { ref("x") } }
        }

        assertEquals(
            """
            |<pair> ::= <pair-alt>
            |<pair-alt> ::= <x> | <x> <x>
            |""".trimMargin(),
            g.toBnf(BnfFlavor.BNF),
        )
    }

    @Test
    fun rangeIsNativeInAbnfAndExpandedElsewhere() {
        val g = grammar {
            rule("digit") { range('0', '3') }
        }

        assertEquals("digit = %x30-33\n", g.toBnf(BnfFlavor.ABNF))
        assertEquals("digit = \"0\" | \"1\" | \"2\" | \"3\";\n", g.toBnf(BnfFlavor.EBNF))
        assertEquals("<digit> ::= \"0\" | \"1\" | \"2\" | \"3\"\n", g.toBnf(BnfFlavor.BNF))
    }

    @Test
    fun expandedRangeInsideAConcatenationIsParenthesized() {
        val g = grammar {
            rule("pair") {
                range('0', '1')
                ref("sep")
            }
        }

        assertEquals("pair = (\"0\" | \"1\"), sep;\n", g.toBnf(BnfFlavor.EBNF))
    }

    @Test
    fun rangeWiderThanTheCapThrowsOutsideAbnf() {
        val g = grammar {
            rule("any") { range(0, 0x10FFFF) }
        }

        // ABNF has a native range syntax, so the same model writes fine there.
        assertEquals("any = %x00-10FFFF\n", g.toBnf(BnfFlavor.ABNF))

        assertFailsWith<IllegalArgumentException> { g.toBnf(BnfFlavor.EBNF) }
        assertFailsWith<IllegalArgumentException> { g.toBnf(BnfFlavor.BNF) }
    }

    @Test
    fun terminalsPreferDoubleQuotesAndFallBackToSingle() {
        val g = grammar {
            rule("plain") { +"while" }
            rule("hasDouble") { +"say \"hi\"" }
            rule("hasSingle") { +"it's" }
        }

        assertEquals(
            """
            |plain = "while";
            |hasDouble = 'say "hi"';
            |hasSingle = "it's";
            |""".trimMargin(),
            g.toBnf(BnfFlavor.EBNF),
        )
    }

    @Test
    fun terminalWithBothQuotesSplitsIntoAConcatenation() {
        val g = grammar {
            rule("mixed") { +"it's \"so\"" }
        }

        assertEquals("mixed = \"it's \", '\"', \"so\", '\"';\n", g.toBnf(BnfFlavor.EBNF))
        assertEquals("<mixed> ::= \"it's \" '\"' \"so\" '\"'\n", g.toBnf(BnfFlavor.BNF))
    }

    @Test
    fun abnfMarksLetterTerminalsCaseSensitive() {
        val g = grammar {
            rule("word") { +"abc" }
            rule("punct") { +"+=" }
        }

        assertEquals(
            """
            |word = %s"abc"
            |punct = "+="
            |""".trimMargin(),
            g.toBnf(BnfFlavor.ABNF),
        )
    }

    @Test
    fun abnfEscapesUnquotableCharactersAsHex() {
        val g = grammar {
            rule("nl") { +"a\nb" }
            rule("quote") { +"\"" }
        }

        assertEquals(
            """
            |nl = %s"a" %x0A %s"b"
            |quote = %x22
            |""".trimMargin(),
            g.toBnf(BnfFlavor.ABNF),
        )
    }

    @Test
    fun grammarNameAndCommentsAreDroppedInBnfAndEmittedElsewhere() {
        val g = grammar("Arith") {
            rule("expr", comment = "the entry point") { ref("term") }
        }

        assertEquals(
            """
            |(* Arith *)
            |(* the entry point *)
            |expr = term;
            |""".trimMargin(),
            g.toBnf(BnfFlavor.EBNF),
        )
        assertEquals(
            """
            |; Arith
            |; the entry point
            |expr = term
            |""".trimMargin(),
            g.toBnf(BnfFlavor.ABNF),
        )
        assertEquals("<expr> ::= <term>\n", g.toBnf(BnfFlavor.BNF))
    }

    @Test
    fun syntheticNamesAvoidCollidingWithDeclaredRules() {
        val g = grammar {
            rule("expr") { optional { ref("x") } }
            rule("expr-opt") { ref("y") }
        }

        assertEquals(
            """
            |<expr> ::= <expr-opt-2>
            |<expr-opt-2> ::= <x> | ""
            |<expr-opt> ::= <y>
            |""".trimMargin(),
            g.toBnf(BnfFlavor.BNF),
        )
    }

    @Test
    fun choiceNestedInAConcatenationIsParenthesized() {
        val g = grammar {
            rule("a") {
                ref("head")
                choice {
                    +"x"
                    +"y"
                }
            }
        }

        assertEquals("a = head, (\"x\" | \"y\");\n", g.toBnf(BnfFlavor.EBNF))
        assertEquals("a = head (%s\"x\" / %s\"y\")\n", g.toBnf(BnfFlavor.ABNF))
    }

    @Test
    fun topLevelChoiceNeedsNoParentheses() {
        val g = grammar {
            rule("a") {
                choice {
                    ref("x")
                    ref("y")
                }
            }
        }

        assertEquals("a = x | y;\n", g.toBnf(BnfFlavor.EBNF))
        assertEquals("<a> ::= <x> | <y>\n", g.toBnf(BnfFlavor.BNF))
    }

    @Test
    fun emptyGrammarWritesNothing() {
        BnfFlavor.entries.forEach { flavor ->
            assertEquals("", grammar {}.toBnf(flavor), "flavor $flavor")
        }
    }

    @Test
    fun illegalRuleNamesAreRejectedPerFlavor() {
        assertFailsWith<IllegalArgumentException> {
            grammar { rule("a<b") { ref("x") } }.toBnf(BnfFlavor.BNF)
        }
        assertFailsWith<IllegalArgumentException> {
            grammar { rule("a|b") { ref("x") } }.toBnf(BnfFlavor.EBNF)
        }
        assertFailsWith<IllegalArgumentException> {
            grammar { rule("1st") { ref("x") } }.toBnf(BnfFlavor.ABNF)
        }
        BnfFlavor.entries.forEach { flavor ->
            assertFailsWith<IllegalArgumentException>("flavor $flavor") {
                grammar { rule("") { ref("x") } }.toBnf(flavor)
            }
        }
    }

    @Test
    fun hyphenatedNamesAreEmittedVerbatimInEbnf() {
        val g = grammar {
            rule("if-stmt") { ref("cond") }
        }

        assertEquals("if-stmt = cond;\n", g.toBnf(BnfFlavor.EBNF))
    }
}
