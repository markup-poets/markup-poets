package org.markup.poet.dsl.grammar

import kotlin.test.Test
import kotlin.test.assertEquals

class GrammarDslTest {

    @Test
    fun singleElementRuleCollapsesToThatElement() {
        val g = grammar {
            rule("a") { ref("b") }
        }

        assertEquals(Grammar(name = null, rules = listOf(Rule("a", Ref("b")))), g)
    }

    @Test
    fun emptyRuleBodyYieldsEmpty() {
        val g = grammar { rule("a") {} }

        assertEquals(Empty, g.rules.single().expr)
    }

    @Test
    fun multipleElementsBecomeSeqInOrder() {
        val g = grammar {
            rule("a") {
                +"let"
                ref("name")
                +"="
            }
        }

        assertEquals(Seq(listOf(Terminal("let"), Ref("name"), Terminal("="))), g.rules.single().expr)
    }

    @Test
    fun singleAlternativeChoiceCollapses() {
        val g = grammar {
            rule("a") { choice { +"x" } }
        }

        assertEquals(Terminal("x"), g.rules.single().expr)
    }

    @Test
    fun choiceKeepsAlternativesInOrder() {
        val g = grammar {
            rule("sign") {
                choice {
                    +"+"
                    +"-"
                    ref("none")
                }
            }
        }

        assertEquals(
            Choice(listOf(Terminal("+"), Terminal("-"), Ref("none"))),
            g.rules.single().expr,
        )
    }

    @Test
    fun altBuildsAConcatenatedAlternative() {
        val g = grammar {
            rule("a") {
                choice {
                    alt {
                        +"do"
                        ref("body")
                    }
                    +"skip"
                }
            }
        }

        assertEquals(
            Choice(listOf(Seq(listOf(Terminal("do"), Ref("body"))), Terminal("skip"))),
            g.rules.single().expr,
        )
    }

    @Test
    fun repetitionHelpersProduceTheRightBounds() {
        val g = grammar {
            rule("zero") { zeroOrMore { ref("x") } }
            rule("one") { oneOrMore { ref("x") } }
            rule("bounded") { repeat(2, 4) { ref("x") } }
            rule("unbounded") { repeat(3) { ref("x") } }
        }

        assertEquals(Repeat(Ref("x"), min = 0, max = null), g.rules[0].expr)
        assertEquals(Repeat(Ref("x"), min = 1, max = null), g.rules[1].expr)
        assertEquals(Repeat(Ref("x"), min = 2, max = 4), g.rules[2].expr)
        assertEquals(Repeat(Ref("x"), min = 3, max = null), g.rules[3].expr)
    }

    @Test
    fun optionalWrapsItsBody() {
        val g = grammar {
            rule("a") {
                optional {
                    +"else"
                    ref("block")
                }
            }
        }

        assertEquals(Optional(Seq(listOf(Terminal("else"), Ref("block")))), g.rules.single().expr)
    }

    @Test
    fun rangesAcceptCharsAndCodePoints() {
        val g = grammar {
            rule("alpha") { range('a', 'z') }
            rule("upper") { range(0x41, 0x5A) }
        }

        assertEquals(Range(0x61, 0x7A), g.rules[0].expr)
        assertEquals(Range(0x41, 0x5A), g.rules[1].expr)
    }

    @Test
    fun grammarKeepsNameRuleOrderAndComments() {
        val g = grammar("Arith") {
            rule("first", comment = "the entry point") { ref("second") }
            rule("second") { empty() }
        }

        assertEquals("Arith", g.name)
        assertEquals(listOf("first", "second"), g.rules.map { it.name })
        assertEquals("the entry point", g.rules[0].comment)
        assertEquals(null, g.rules[1].comment)
        assertEquals(Empty, g.rules[1].expr)
    }

    @Test
    fun nestingComposes() {
        val g = grammar {
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

        assertEquals(
            Seq(
                listOf(
                    Ref("term"),
                    Repeat(
                        Seq(listOf(Choice(listOf(Terminal("+"), Terminal("-"))), Ref("term"))),
                        min = 0,
                        max = null,
                    ),
                ),
            ),
            g.rules.single().expr,
        )
    }
}
