# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-07-17

Adds the BNF family — grammars in, BNF/EBNF/ABNF out.

### Added

- **`markup-grammar`**: a standalone grammar model (`Grammar` / `Rule` /
  `Expr`) and `grammar { }` DSL, zero dependencies. Like the other model
  modules it is notation-agnostic — there is deliberately no grouping node,
  since grouping is a printing concern rather than a semantic one.
- **`markup-bnf-writer`**: writes grammars as **BNF, EBNF (ISO 14977) or
  ABNF (RFC 5234)** source text. One model, three notations: the flavor is a
  defaulted `BnfFlavor` parameter on all four output forms rather than three
  separate writers, because BNF is the format and the three are notations of
  it that differ in punctuation.

  ```kotlin
  val g = grammar {
      rule("expr") {
          ref("term")
          zeroOrMore { choice { +"+"; +"-" }; ref("term") }
      }
  }

  g.toBnf()                 // expr = term, {("+" | "-"), term};
  g.toBnf(BnfFlavor.ABNF)   // expr = term *(("+" / "-") term)
  ```

  `EBNF` is the default: it is the only flavor that maps 1:1 to the model.
  Classic BNF has no optional, repetition or grouping syntax, so those
  desugar into synthetic rules — it is the one flavor whose emitted rule
  count differs from the model's.

- **Documentation**: `reference/bnf-mapping.adoc` covers the notation
  punctuation, the desugaring table, terminal quoting, and every deviation.

### Notes

- ABNF quoted strings are case-insensitive per RFC 5234, which the model's
  terminals are not, so a terminal containing ASCII letters is emitted as a
  case-sensitive `%s"..."` string (RFC 7405). This is the one place the
  writer reaches past RFC 5234, and it is deliberate: plain `"abc"` would
  mean something the model does not.
- `Range` is native only in ABNF. BNF and EBNF expand it into a choice of
  single-character terminals, capped at 256 code points — a wider range
  throws rather than silently emitting an unusable grammar. This is the only
  case where a valid model succeeds in one flavor and fails in another.
- Writer options are defaulted parameters on the four output forms, not new
  functions. `markup-bnf-writer` is the first writer to have one, and sets
  that precedent.

## [0.1.0] - 2026-07-08

First release: a type-safe Kotlin Multiplatform DSL that builds markup models
and writes them as source text. Rendering to presentation formats (HTML, PDF)
is out of scope — that is the job of downstream tools consuming the output.

### Added

- **`markup-table`**: standalone table model + DSL, zero dependencies.
- **`markup-document`**: abstract document model + `article` DSL, zero
  dependencies.
- **`markup-graph`**: standalone graph model + DSL (nodes/edges) with
  `isAcyclic()` and `topologicalSortOrNull()` (Kahn, deterministic), zero
  dependencies.
- **`markup-asciidoc-writer`**: writes documents and tables as AsciiDoc.
- **`markup-markdown-writer`**: writes documents and tables as Markdown (GFM).
- **`markup-dot-writer`**: writes graphs as Graphviz DOT.
- Four output forms per model in every writer — `toX()`,
  `writeXTo(Appendable)`, `writeXTo(Sink)` (kotlinx-io), and
  `xFlow(): Flow<String>` — where concatenating the flow's chunks yields
  exactly `toX()`.
- Targets: JVM, Android (API 24+), iOS, Linux, macOS.

[0.2.0]: https://github.com/markup-poets/markup-poets/releases/tag/v0.2.0
[0.1.0]: https://github.com/markup-poets/markup-poets/releases/tag/v0.1.0
