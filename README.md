# Markup Poet — Markup DSL

A type-safe Kotlin DSL for building markup documents, rendered to AsciiDoc source text. Built with Kotlin Multiplatform.

This is the **DSL poet** of the [Markup Poet](https://github.com/markup-poets) family — a growing set of markup tools ("poets") that includes [asciidoc-kmp](https://github.com/markup-poets/asciidoc-kmp), the AsciiDoc parser.

## Features

- **Type-safe DSL**: Describe documents in Kotlin — sections, paragraphs, code blocks, images, tables, nested lists
- **Renderer-agnostic model**: The document model is independent of any output syntax; renderers plug in as separate modules (AsciiDoc today, Markdown/HTML planned)
- **Platform independent**: JVM, Android (API 24+), iOS, Linux, macOS
- **Zero dependencies**: No external libraries

## Quick Start

```kotlin
import org.markup.poet.markup.dsl.article
import org.markup.poet.markup.model.ListType
import org.markup.poet.markup.asciidoc.renderAsciidoc

val doc = article {
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
    }
}

println(doc.renderAsciidoc())
```

produces AsciiDoc source text:

```asciidoc
[#markup-poet]
= Markup Poet

A DSL for markup documents.

== Usage

[source,kotlin]
----
val doc = article {}
----

. build
. render
```

Note: section nesting starts at `=` for top-level sections. In AsciiDoc convention `=` is the document title, so for standard article layouts start your content sections one level down.

## Modules

| Module | Coordinates | Contents |
|---|---|---|
| `markup-core` | `org.markup-poet:markup-core` | document model + DSL, zero deps |
| `markup-asciidoc-renderer` | `org.markup-poet:markup-asciidoc-renderer` | AsciiDoc source-text renderer |

## Building

```bash
./gradlew build                                                    # full build
./gradlew :markup-core:jvmTest :markup-asciidoc-renderer:jvmTest   # JVM tests
./gradlew :markup-asciidoc-renderer:linuxX64Test                   # native tests (Linux host)
```

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
