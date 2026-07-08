# Markup Poet — Markup DSL

A type-safe Kotlin DSL for building markup documents, written out as AsciiDoc source text. Built with Kotlin Multiplatform.

This is the **DSL poet** of the [Markup Poet](https://github.com/markup-poets) family — a growing set of markup tools ("poets") that includes [asciidoc-kmp](https://github.com/markup-poets/asciidoc-kmp), the AsciiDoc parser.

## Architecture

An abstract, format-agnostic document model composed of **sub-DSLs**, each usable standalone, plus **writer** modules that write the models as concrete markup formats:

| Module | Coordinates | Contents |
|---|---|---|
| `markup-table` | `org.markup-poet:markup-table` | standalone table model + DSL (à la picnic), zero deps |
| `markup-document` | `org.markup-poet:markup-document` | abstract document model + `article` DSL, zero deps |
| `markup-asciidoc-writer` | `org.markup-poet:markup-asciidoc-writer` | writes the models as AsciiDoc source text |

Planned: Markdown, DocBook, and HTML writers as sibling modules; further sub-DSLs.

## Features

- **Type-safe DSL**: Describe documents in Kotlin — sections, paragraphs, code blocks, images, tables, nested lists
- **Standalone sub-DSLs**: Each sub-DSL (e.g. tables) works on its own; build and write a table without a document around it
- **Writer modules per format**: `toAsciidoc(): String`, `writeAsciidocTo(Appendable)`, `writeAsciidocTo(Sink)` (kotlinx-io), `asciidocFlow(): Flow<String>` (streaming)
- **Platform independent**: JVM, Android (API 24+), iOS, Linux, macOS
- **Zero dependencies** in the model/DSL modules; writer modules use kotlinx-io and kotlinx-coroutines

## Quick Start

```kotlin
import org.markup.poet.dsl.document.article
import org.markup.poet.dsl.document.ListType
import org.markup.poet.dsl.write.asciidoc.toAsciidoc

val doc = article("Markup Poet") {
    section("Usage") {
        +"A DSL for markup documents."
        code("kotlin") {
            +"val doc = article {}"
        }
        list(ListType.ORDERED) {
            +"build"
            +"write"
        }
    }
}

println(doc.toAsciidoc())
```

produces AsciiDoc source text:

```asciidoc
= Markup Poet

== Usage

A DSL for markup documents.

[source,kotlin]
----
val doc = article {}
----

. build
. write
```

`article(title) { }` writes the title as the `=` document title and starts sections at `==`, following AsciiDoc convention. The title-less form `article { }` starts sections at `=` instead.

## Standalone sub-DSLs

Sub-DSLs work without a document. The table DSL, for example:

```kotlin
import org.markup.poet.dsl.table.table
import org.markup.poet.dsl.write.asciidoc.toAsciidoc

val t = table {
    header {
        cell("name")
        cell("value")
    }
    row("answer", "42")
}

println(t.toAsciidoc())
```

```asciidoc
|===
|name|value

| answer
| 42
|===
```

The same `table { }` builder is used inside documents via `section { table("title", "id") { ... } }`.

## Writers

Every model type gets four write forms in a writer module:

```kotlin
doc.toAsciidoc()                 // String
doc.writeAsciidocTo(appendable)  // any Appendable (StringBuilder, java.io.Writer, ...)
doc.writeAsciidocTo(sink)        // kotlinx-io Sink, UTF-8; caller flushes/closes
doc.asciidocFlow()               // cold Flow<String> of chunks, in document order;
                                 // concatenating all chunks == toAsciidoc()
```

Note: depending on `markup-asciidoc-writer` brings kotlinx-io and kotlinx-coroutines onto your classpath; the model/DSL modules (`markup-table`, `markup-document`) stay dependency-free.

## Building

```bash
./gradlew build                                    # full build
./gradlew :markup-table:jvmTest :markup-document:jvmTest :markup-asciidoc-writer:jvmTest
./gradlew :markup-asciidoc-writer:linuxX64Test     # native tests (Linux host)
```

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
