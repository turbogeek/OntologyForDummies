# Examples — SE-101 and the real IOF ontologies

This guide is for anyone who wants to run something real — especially systems engineers — and for anyone who needs the licensing and attribution rules for the files the tool can fetch.

There are two examples. **SE-101** ships in the repo and is the fastest way to see the verbalizer in action. **IOF + BFO** are real, third-party industry ontologies you fetch on demand. Start with SE-101.

Before either one, build the self-contained CLI jar once:

```bash
./gradlew cliJar
JAR=build/libs/ontology-to-english-*-cli.jar
```

The wildcard in `JAR` avoids version drift — it always picks up the jar Gradle just built.

## SE-101: Systems Engineering 101 (ships in the repo)

`examples/se-101.ttl` is a small, hand-built systems-engineering ontology, written to read well "for muggles": every class and property has a plain-English label and definition. It models eight kinds of thing:

- **System**, **Subsystem**, **Component**, **Interface**
- **Function**, **Requirement**, **Stakeholder**, **VerificationActivity**

It is deliberately built to exercise every feature the verbalizer shows off:

- **subclassing** — `Each Subsystem is a System .`
- **existential restrictions** ("at least one", `owl:someValuesFrom`) — `Each System performs function at least one Function .`
- **universal restrictions** ("only", `owl:allValuesFrom`) — `Each Component has interface only Interface .`
- **cardinality** ("at least 2") — `Each System has component at least 2 Component .`
- **disjointness** — `No Component is a Function or Requirement .`
- **inverse / symmetric / transitive properties** — `The relationship has part is transitive .` and `Anything that has part something is a System .`
- **individuals (instances)** — specific named things, listed in their own section.

SE-101 is MIT licensed and part of this repo, so there is nothing to download.

### Run SE-101

The default output is SBVR Structured English, written next to the input as `se-101-sbvr.html`:

```bash
java -jar $JAR examples/se-101.ttl --open                 # SBVR Structured English
java -jar $JAR examples/se-101.ttl --rosetta --open       # SBVR | OSE | Manchester, side by side
java -jar $JAR examples/se-101.ttl --validate             # reasoner check
```

`--open` opens the result in your browser; if that fails (for example on a headless box) the tool prints `(could not open a browser: ...)` but still writes the file and prints `Wrote <absolute path>`. The `--rosetta` run puts all three formats in one side-by-side table sharing a single glossary — the best way to learn how the formats line up (see [formats.md](formats.md)). For every flag, see [cli-reference.md](cli-reference.md).

The `--validate` run reports:

```text
VALIDATION: consistent; no unsatisfiable classes.
```

SE-101 is logically clean by design, so this is the expected result — a good baseline before you point `--validate` at an ontology you suspect has problems. For what the other two outcomes look like, see [validation.md](validation.md).

### What the report reads like

The four verbalization sections — `Vocabulary — Concepts (Classes)`, `Fact Types — Relationships (Object Properties)`, `Attributes (Data Properties)`, and `Individuals (Instances)` — turn the formal axioms into English you can read aloud:

```text
Each Subsystem is a System .
Each System performs function at least one Function .
Each System has component at least 2 Component .
Each Component has interface only Interface .
No Component is a Function or Requirement .
The relationship has part is transitive .
Anything that has part something is a System .
```

In a browser these come out colored: **term** (concept/class) in green, **verb** (fact type/relationship) in blue, **keyword** (`Each`, `No`, `only`, `at least one`, `at least 2`) in orange, and **name** (individual) in teal; data values (literals) stay plain. Hover any colored word to see its meaning. For the full legend and how the colors map to roles, see [reading-the-output.md](reading-the-output.md); for the vocabulary itself, see [concepts.md](concepts.md).

## IOF + BFO: the real Industrial Ontology Foundry (fetched on demand)

For something heavier and genuinely industrial, point the tool at the [Industrial Ontology Foundry](https://github.com/iofoundry/ontology) Core ontology, which is built on Barry Smith's [Basic Formal Ontology (BFO)](https://github.com/BFO-ontology/BFO-2020). These third-party files are **not** committed here. Fetch them with a Gradle task:

```bash
./gradlew fetchIofExample        # downloads IOF Core + BFO into examples/iof/ (git-ignored)
JAR=build/libs/ontology-to-english-*-cli.jar
```

`fetchIofExample` **downloads three ontology files** and **generates two more** (a catalog and a notice) into `examples/iof/`:

- `Core.rdf` — IOF Core *(downloaded)*
- `AnnotationVocabulary.rdf` — the IOF annotation vocabulary *(downloaded)*
- `bfo-core.owl` — BFO 2020 *(downloaded)*
- `catalog-v001.xml` — a generated OASIS XML catalog (see below) *(generated locally)*
- `NOTICE.md` — the required attribution for the fetched files *(generated locally)*

This directory is **git-ignored** and never committed, so you must run `fetchIofExample` before the IOF commands below will work. If you see missing files or unresolved imports, that is almost always the cause — see [troubleshooting.md](troubleshooting.md).

### Run IOF

```bash
java -jar $JAR examples/iof/Core.rdf --rosetta --open
java -jar $JAR examples/iof/Core.rdf --validate
```

Verbalizing `examples/iof/Core.rdf` resolves **107 classes**, including BFO foundations like `continuant`, `occurrent`, and `material entity`. This is a real upper ontology — a good stress test for the verbalizer and a way to read industry-standard vocabulary in plain English.

### Offline imports: how the catalog works

IOF Core `owl:imports` BFO and the IOF annotation vocabulary. Rather than hit the network every time, `fetchIofExample` writes `examples/iof/catalog-v001.xml` — an OASIS XML catalog that maps each imported ontology IRI to its local file. When you verbalize `Core.rdf`, the tool's importer reads sibling files and that catalog from the input's own folder and resolves the imports **offline**.

The catch: the `examples/iof/` layout must be preserved exactly. The catalog maps IRIs to *these* filenames in *this* folder, so keep `Core.rdf`, `AnnotationVocabulary.rdf`, `bfo-core.owl`, and `catalog-v001.xml` together. Move or rename one and the imports stop resolving (the tool tolerates missing imports silently, so the report just comes out thinner rather than erroring).

## Licensing and attribution

Each example carries its own license. The fetched IOF and BFO files are redistributable, but BFO's CC BY 4.0 **requires** you to keep its attribution — which is exactly why those files are fetched on demand rather than vendored into this MIT repo. The generated `examples/iof/NOTICE.md` carries that attribution for you.

| Ontology | License | Attribution |
|----------|---------|-------------|
| `se-101.ttl` (this repo) | MIT | — |
| IOF Core (`Core.rdf`, `AnnotationVocabulary.rdf`) | MIT | © Industrial Ontology Foundry |
| BFO 2020 (`bfo-core.owl`) | CC BY 4.0 | © B. Smith et al. — keep the attribution when redistributing |

If you redistribute output or files derived from the IOF/BFO fetch, carry the `NOTICE.md` attribution along with it.

## Where to go next

- New to the vocabulary in these sentences? Start with [concepts.md](concepts.md).
- Want to decode the colored report? See [reading-the-output.md](reading-the-output.md).
- Curious about the SBVR / OSE / Manchester / Rosetta views? See [formats.md](formats.md).
- Need the full flag list? See [cli-reference.md](cli-reference.md).
- Interpreting a `--validate` result? See [validation.md](validation.md).
- Hit an error or missing file? See [troubleshooting.md](troubleshooting.md).
- Embedding the tool in your own code? See [library-guide.md](library-guide.md).
- For where the project is headed (the READ / CREATE / REASON pillars), see [../ROADMAP.md](../ROADMAP.md).
