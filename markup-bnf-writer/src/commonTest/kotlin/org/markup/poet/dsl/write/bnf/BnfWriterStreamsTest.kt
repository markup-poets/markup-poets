package org.markup.poet.dsl.write.bnf

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readString
import org.markup.poet.dsl.grammar.grammar
import kotlin.test.Test
import kotlin.test.assertEquals

/** The four-forms contract holds per flavor, so every assertion runs across all three. */
class BnfWriterStreamsTest {

    private val sample = grammar("Arith") {
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
        rule("term") { +"1" }
    }

    @Test
    fun writesGrammarToSink() {
        BnfFlavor.entries.forEach { flavor ->
            val buffer = Buffer()
            sample.writeBnfTo(buffer, flavor)
            assertEquals(sample.toBnf(flavor), buffer.readString(), "flavor $flavor")
        }
    }

    @Test
    fun writesGrammarToAppendable() {
        BnfFlavor.entries.forEach { flavor ->
            val sb = StringBuilder()
            sample.writeBnfTo(sb, flavor)
            assertEquals(sample.toBnf(flavor), sb.toString(), "flavor $flavor")
        }
    }

    @Test
    fun emptyGrammarWritesNothing() {
        BnfFlavor.entries.forEach { flavor ->
            val buffer = Buffer()
            grammar {}.writeBnfTo(buffer, flavor)
            assertEquals("", buffer.readString(), "flavor $flavor")
            assertEquals("", grammar {}.toBnf(flavor), "flavor $flavor")
        }
    }

    @Test
    fun flowChunksConcatenateToStringOutput() = runTest {
        BnfFlavor.entries.forEach { flavor ->
            assertEquals(
                sample.toBnf(flavor),
                sample.bnfFlow(flavor).toList().joinToString(""),
                "flavor $flavor",
            )
        }
    }

    @Test
    fun defaultFlavorMatchesEbnfAcrossAllFourForms() = runTest {
        val buffer = Buffer()
        sample.writeBnfTo(buffer)
        val sb = StringBuilder()
        sample.writeBnfTo(sb)

        val expected = sample.toBnf(BnfFlavor.EBNF)
        assertEquals(expected, sample.toBnf())
        assertEquals(expected, buffer.readString())
        assertEquals(expected, sb.toString())
        assertEquals(expected, sample.bnfFlow().toList().joinToString(""))
    }

    @Test
    fun flowEmitsTheNameCommentThenOneChunkPerRule() = runTest {
        val chunks = sample.bnfFlow(BnfFlavor.EBNF).toList()

        assertEquals(3, chunks.size)
        assertEquals("(* Arith *)\n", chunks[0])
        assertEquals("expr = term, {(\"+\" | \"-\"), term};\n", chunks[1])
        assertEquals("term = \"1\";\n", chunks[2])
    }

    @Test
    fun bnfEmitsAnExtraChunkPerSyntheticRule() = runTest {
        // The same model desugars in BNF only: two rules in, three chunks out.
        val g = grammar {
            rule("stmt") {
                ref("head")
                optional { ref("tail") }
            }
            rule("head") { +"h" }
        }

        assertEquals(2, g.bnfFlow(BnfFlavor.EBNF).toList().size)
        assertEquals(2, g.bnfFlow(BnfFlavor.ABNF).toList().size)

        val bnf = g.bnfFlow(BnfFlavor.BNF).toList()
        assertEquals(3, bnf.size)
        assertEquals("<stmt> ::= <head> <stmt-opt>\n", bnf[0])
        assertEquals("<stmt-opt> ::= <tail> | \"\"\n", bnf[1])
        assertEquals("<head> ::= \"h\"\n", bnf[2])
    }

    @Test
    fun unnamedGrammarEmitsNoLeadingComment() = runTest {
        val g = grammar {
            rule("a") { ref("b") }
        }

        BnfFlavor.entries.forEach { flavor ->
            assertEquals(1, g.bnfFlow(flavor).toList().size, "flavor $flavor")
        }
    }
}
