# Example ontologies

Two ways to see the verbalizer turn real OWL into readable English.

## 1. `se-101.ttl` — Systems Engineering 101 (ships in this repo)

A small, hand-built systems-engineering ontology — `System`, `Subsystem`, `Component`, `Interface`,
`Function`, `Requirement`, `Stakeholder`, `VerificationActivity` — written to read well "for muggles":
every class and property has a plain-English label and definition. It exercises the things the verbalizer
shows off: subclassing, existential ("at least one" / `some`) and universal ("only") restrictions,
cardinality ("at least 2"), disjointness, inverse / symmetric / transitive properties, and individuals.
MIT licensed, part of this repo.

```bash
./gradlew cliJar
JAR=build/libs/ontology-to-english-*-cli.jar

java -jar $JAR examples/se-101.ttl --open                 # SBVR Structured English
java -jar $JAR examples/se-101.ttl --rosetta --open       # SBVR | OWL Simplified English | Manchester, side by side
java -jar $JAR examples/se-101.ttl --validate             # reasoner: consistent; no unsatisfiable classes
```

## 2. The real Industrial Ontology Foundry (IOF) — fetched on demand

The [Industrial Ontology Foundry](https://github.com/iofoundry/ontology) publishes a real
systems-engineering upper ontology (IOF Core), built on Barry Smith's
[Basic Formal Ontology (BFO)](https://github.com/BFO-ontology/BFO-2020). These third-party files are **not**
committed here — fetch them on demand:

```bash
./gradlew fetchIofExample        # downloads IOF Core + BFO into examples/iof/ (git-ignored)
JAR=build/libs/ontology-to-english-*-cli.jar

java -jar $JAR examples/iof/Core.rdf --rosetta --open
java -jar $JAR examples/iof/Core.rdf --validate
```

The fetch also writes `examples/iof/catalog-v001.xml` (so the CLI resolves IOF Core's imports of BFO and the
IOF annotation vocabulary **offline**) and `examples/iof/NOTICE.md` with the required attribution.

### Licenses

| Ontology | License | Attribution |
|----------|---------|-------------|
| `se-101.ttl` (this repo) | MIT | — |
| IOF Core (`Core.rdf`, `AnnotationVocabulary.rdf`) | MIT | © Industrial Ontology Foundry |
| BFO 2020 (`bfo-core.owl`) | CC BY 4.0 | © B. Smith et al. — keep the attribution when redistributing |

Both fetched ontologies are redistributable, but BFO's CC BY 4.0 **requires** you to keep its attribution
(handled by the generated `NOTICE.md`). That's why they're fetched rather than vendored into this MIT repo.
