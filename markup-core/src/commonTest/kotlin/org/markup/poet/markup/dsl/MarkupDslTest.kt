package org.markup.poet.markup.dsl

import org.markup.poet.markup.model.BlockImage
import org.markup.poet.markup.model.CodeBlock
import org.markup.poet.markup.model.ListBlock
import org.markup.poet.markup.model.ListType
import org.markup.poet.markup.model.Paragraph
import org.markup.poet.markup.model.Section
import org.markup.poet.markup.model.TableBlock
import org.markup.poet.markup.model.TableRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarkupDslTest {

    @Test
    fun sectionWithParagraphs() {
        val markup = article {
            section("Title", id = "intro") {
                +"first"
                text("second")
                text("p2", "third")
            }
        }

        assertEquals(1, markup.blocks.size)
        val section = markup.blocks[0] as Section
        assertEquals("Title", section.title)
        assertEquals("intro", section.id)
        assertEquals(1, section.level)
        assertEquals(
            listOf(
                Paragraph("", "first"),
                Paragraph("", "second"),
                Paragraph("p2", "third"),
            ),
            section.blocks,
        )
    }

    @Test
    fun nestedSectionsGetIncreasingLevels() {
        val markup = article {
            section("L1") {
                section("L2") {
                    section("L3") {}
                }
            }
        }

        val l1 = markup.blocks[0] as Section
        val l2 = l1.blocks[0] as Section
        val l3 = l2.blocks[0] as Section
        assertEquals(listOf(1, 2, 3), listOf(l1.level, l2.level, l3.level))
    }

    @Test
    fun codeBlockAccumulatesText() {
        val markup = article {
            section("S") {
                code("kotlin") {
                    +"val x = 1"
                }
                code {}
            }
        }

        val section = markup.blocks[0] as Section
        assertEquals(CodeBlock("", "kotlin", "val x = 1"), section.blocks[0])
        assertEquals(CodeBlock("", "", ""), section.blocks[1])
    }

    @Test
    fun tableWithoutHeader() {
        val markup = article {
            section("S") {
                table("My table", id = "t1") {
                    row("a", "b")
                    row {
                        cell("c")
                        cell("d")
                    }
                }
            }
        }

        val table = (markup.blocks[0] as Section).blocks[0] as TableBlock
        assertNull(table.header)
        assertEquals("My table", table.title)
        assertEquals("t1", table.id)
        assertEquals(listOf(TableRow(listOf("a", "b")), TableRow(listOf("c", "d"))), table.rows)
        assertEquals(2, table.columnCount)
    }

    @Test
    fun tableWithHeader() {
        val markup = article {
            section("S") {
                table {
                    header {
                        cell("col 1")
                        cell("col 2")
                    }
                    row("x", "y")
                }
            }
        }

        val table = (markup.blocks[0] as Section).blocks[0] as TableBlock
        assertEquals(TableRow(listOf("col 1", "col 2")), table.header)
        assertEquals(1, table.rows.size)
    }

    @Test
    fun nestedLists() {
        val markup = article {
            section("S") {
                list(ListType.ORDERED) {
                    +"one"
                    list(ListType.UNORDERED) {
                        +"one point one"
                    }
                    item("two")
                }
            }
        }

        val list = (markup.blocks[0] as Section).blocks[0] as ListBlock
        assertEquals(ListType.ORDERED, list.type)
        assertEquals(3, list.items.size)
        assertEquals(Paragraph("", "one"), list.items[0])
        val nested = list.items[1] as ListBlock
        assertEquals(ListType.UNORDERED, nested.type)
        assertEquals(listOf<Any>(Paragraph("", "one point one")), nested.items)
        assertEquals(Paragraph("", "two"), list.items[2])
    }

    @Test
    fun imageSeparatesPathFromTitle() {
        val markup = article {
            section("S") {
                image(
                    path = "sunset.jpg",
                    id = "img-sunset",
                    title = "A mountain sunset",
                    link = "https://example.com/sunset",
                    width = 200,
                    height = 100,
                    alt = "Sunset",
                )
            }
        }

        val image = (markup.blocks[0] as Section).blocks[0] as BlockImage
        assertEquals("sunset.jpg", image.path)
        assertEquals("A mountain sunset", image.title)
        assertEquals("https://example.com/sunset", image.link)
        assertEquals(200, image.attributes.width)
        assertEquals(100, image.attributes.height)
        assertEquals("Sunset", image.attributes.alt)
    }
}
