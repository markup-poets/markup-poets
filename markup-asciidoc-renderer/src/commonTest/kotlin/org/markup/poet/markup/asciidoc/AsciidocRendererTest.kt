package org.markup.poet.markup.asciidoc

import org.markup.poet.markup.dsl.article
import org.markup.poet.markup.model.ListType
import kotlin.test.Test
import kotlin.test.assertEquals

class AsciidocRendererTest {

    @Test
    fun sectionWithParagraphs() {
        val markup = article {
            section("Hello") {
                +"para one"
                +"para two"
            }
        }

        assertEquals(
            """
            |= Hello
            |
            |para one
            |
            |para two
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun nestedSectionHeadingsUseSingleSpace() {
        val markup = article {
            section("A") {
                section("B") {
                    section("C") {}
                }
            }
        }

        assertEquals(
            """
            |= A
            |
            |== B
            |
            |=== C
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun sectionWithIdGetsAnchorLine() {
        val markup = article {
            section("Intro", id = "intro") {
                +"text"
            }
        }

        assertEquals(
            """
            |[#intro]
            |= Intro
            |
            |text
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun codeBlockWithLanguage() {
        val markup = article {
            section("S") {
                code("kotlin") {
                    +"val x = 1"
                }
            }
        }

        assertEquals(
            """
            |= S
            |
            |[source,kotlin]
            |----
            |val x = 1
            |----
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun codeBlockWithoutLanguage() {
        val markup = article {
            section("S") {
                code {
                    +"plain"
                }
            }
        }

        assertEquals(
            """
            |= S
            |
            |[source]
            |----
            |plain
            |----
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun minimalImage() {
        val markup = article {
            section("S") {
                image("cat.jpg")
            }
        }

        assertEquals(
            """
            |= S
            |
            |image::cat.jpg[]
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun fullImage() {
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

        assertEquals(
            """
            |= S
            |
            |[#img-sunset]
            |.A mountain sunset
            |[link=https://example.com/sunset]
            |image::sunset.jpg[Sunset,200,100]
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun imageWithDimensionsButNoAlt() {
        val markup = article {
            section("S") {
                image("cat.jpg", width = 120, height = 300)
            }
        }

        assertEquals(
            """
            |= S
            |
            |image::cat.jpg[,120,300]
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun tableWithoutHeaderGetsColsAttribute() {
        val markup = article {
            section("S") {
                table {
                    row("row 0, cell 0", "row 0, cell 1")
                    row("row 1, cell 0", "row 1, cell 1")
                }
            }
        }

        assertEquals(
            """
            |= S
            |
            |[cols="1,1"]
            ||===
            || row 0, cell 0
            || row 0, cell 1
            |
            || row 1, cell 0
            || row 1, cell 1
            ||===
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun tableWithHeaderTitleAndId() {
        val markup = article {
            section("S") {
                table("Greetings", id = "table_id") {
                    header {
                        cell("col 1")
                        cell("col 2")
                    }
                    row("Hello", "World")
                    row("Hola", "Mundo")
                }
            }
        }

        assertEquals(
            """
            |= S
            |
            |[#table_id]
            |.Greetings
            ||===
            ||col 1|col 2
            |
            || Hello
            || World
            |
            || Hola
            || Mundo
            ||===
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun nestedOrderedList() {
        val markup = article {
            section("S") {
                list(ListType.ORDERED) {
                    +"item 1"
                    list(ListType.ORDERED) {
                        +"item 1.1"
                        +"item 1.2"
                    }
                    +"item 2"
                }
            }
        }

        assertEquals(
            """
            |= S
            |
            |. item 1
            |.. item 1.1
            |.. item 1.2
            |. item 2
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun nestedUnorderedList() {
        val markup = article {
            section("S") {
                list {
                    +"item 1"
                    list {
                        +"item 1.1"
                    }
                    +"item 2"
                }
            }
        }

        assertEquals(
            """
            |= S
            |
            |* item 1
            |** item 1.1
            |* item 2
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }

    @Test
    fun fullDocument() {
        val markup = article {
            section("Markup Poet", id = "markup-poet") {
                +"A DSL for markup documents."
                section("Usage") {
                    code("kotlin") {
                        +"val doc = article {}"
                    }
                    list(ListType.ORDERED) {
                        +"build"
                        +"render"
                    }
                }
                section("Data") {
                    table("Results") {
                        header {
                            cell("name")
                            cell("value")
                        }
                        row("answer", "42")
                    }
                    image("logo.png", alt = "Logo")
                }
            }
        }

        assertEquals(
            """
            |[#markup-poet]
            |= Markup Poet
            |
            |A DSL for markup documents.
            |
            |== Usage
            |
            |[source,kotlin]
            |----
            |val doc = article {}
            |----
            |
            |. build
            |. render
            |
            |== Data
            |
            |.Results
            ||===
            ||name|value
            |
            || answer
            || 42
            ||===
            |
            |image::logo.png[Logo]
            |""".trimMargin(),
            markup.renderAsciidoc(),
        )
    }
}
