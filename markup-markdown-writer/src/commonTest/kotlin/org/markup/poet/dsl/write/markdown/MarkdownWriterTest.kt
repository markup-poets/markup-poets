package org.markup.poet.dsl.write.markdown

import org.markup.poet.dsl.document.ListType
import org.markup.poet.dsl.document.article
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownWriterTest {

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
            |# Hello
            |
            |para one
            |
            |para two
            |""".trimMargin(),
            markup.toMarkdown(),
        )
    }

    @Test
    fun articleTitleWritesAsDocumentTitle() {
        val markup = article("Doc Title") {
            section("First") {
                +"body"
                section("Nested") {}
            }
        }

        assertEquals(
            """
            |# Doc Title
            |
            |## First
            |
            |body
            |
            |### Nested
            |""".trimMargin(),
            markup.toMarkdown(),
        )
    }

    @Test
    fun sectionWithIdGetsAnchor() {
        val markup = article {
            section("Intro", id = "intro") {
                +"text"
            }
        }

        assertEquals(
            """
            |<a id="intro"></a>
            |# Intro
            |
            |text
            |""".trimMargin(),
            markup.toMarkdown(),
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
            |# S
            |
            |```kotlin
            |val x = 1
            |```
            |""".trimMargin(),
            markup.toMarkdown(),
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
            |# S
            |
            |```
            |plain
            |```
            |""".trimMargin(),
            markup.toMarkdown(),
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
            |# S
            |
            |![](cat.jpg)
            |""".trimMargin(),
            markup.toMarkdown(),
        )
    }

    @Test
    fun fullImageBecomesLinkedImageWithAnchor() {
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

        // width/height have no native Markdown syntax and are dropped
        assertEquals(
            """
            |# S
            |
            |<a id="img-sunset"></a>
            |[![Sunset](sunset.jpg "A mountain sunset")](https://example.com/sunset)
            |""".trimMargin(),
            markup.toMarkdown(),
        )
    }

    @Test
    fun headerlessTableGetsEmptyHeaderCells() {
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
            |# S
            |
            ||  |  |
            || --- | --- |
            || row 0, cell 0 | row 0, cell 1 |
            || row 1, cell 0 | row 1, cell 1 |
            |""".trimMargin(),
            markup.toMarkdown(),
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
            |# S
            |
            |<a id="table_id"></a>
            |**Greetings**
            |
            || col 1 | col 2 |
            || --- | --- |
            || Hello | World |
            || Hola | Mundo |
            |""".trimMargin(),
            markup.toMarkdown(),
        )
    }

    @Test
    fun nestedOrderedListUsesIndentationAndNumbers() {
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
            |# S
            |
            |1. item 1
            |    1. item 1.1
            |    2. item 1.2
            |2. item 2
            |""".trimMargin(),
            markup.toMarkdown(),
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
            |# S
            |
            |- item 1
            |    - item 1.1
            |- item 2
            |""".trimMargin(),
            markup.toMarkdown(),
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
                        +"write"
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
            |<a id="markup-poet"></a>
            |# Markup Poet
            |
            |A DSL for markup documents.
            |
            |## Usage
            |
            |```kotlin
            |val doc = article {}
            |```
            |
            |1. build
            |2. write
            |
            |## Data
            |
            |**Results**
            |
            || name | value |
            || --- | --- |
            || answer | 42 |
            |
            |![Logo](logo.png)
            |""".trimMargin(),
            markup.toMarkdown(),
        )
    }
}
