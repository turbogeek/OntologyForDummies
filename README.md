# OntologyForDummies

**Turn an OWL ontology into a colored, hyperlinked web page that reads like plain English — so muggles can read it.**

Ontology Vision is a set of ontology visualization tools aimed squarely at the novice (or slightly novice) ontologist. The basic hope is to let the novice read and create ontologies, and to give the expert ontologist a way to communicate with the muggles.

Unlike Harry (the) Potter, we think ontologies are important to the muggles. (The owner of the project is a muggle who was creating an ontology on magic, so he needed a way to learn the magic of ontology in order to create a magic ontology.)

The first tool converts ontologies into reasonably easy-to-read ontologies.

But there will be more! Why? AI first, because it is now easy for a gray-haired engineer/programmer to build cool tools. But also AI, because one of the multipliers of AI intelligence is good, standardized, navigable data. Ontologies meet this need.

The problem is creating them. One of the biggest issues is that professional ontologists are some of the rarest brains on the planet. A quick Google search said a large ontology conference is 1,500 people. My first JavaOne conference was over 20,000. Google IO is 5,000, with thousands more downloading the videos. Simply put, to boost the creation of ontologies, we need more help. Yes, real ontologists need to be involved, but the precision, language, skills, and rules of ontology need a little translation for the muggles who are the experts in everything else — except ontology.

## Tool #1: Ontology to English

Our first tool uses SBVR (you don't care what SBVR is yet, so I won't bore you) and the OWL API to create English-like translations of an ontology. It reads an OWL file and writes a nice readable web page with colors and hyperlinks, with even more translations of the big words of ontology that sneak in. This engine has been refined over the years, but the big professional ontology tools never scratched the muggle brain-cell itch. For many people, this simple translation will go quite far. If we find better, we will let you know.

## 60-second quick start

You need a **Java 17+ JDK**. Everything else is fetched by the Gradle wrapper.

```bash
./gradlew cliJar                                                          # build the portable CLI jar
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --open
```

That builds a single self-contained jar at `build/libs/ontology-to-english-0.4.0-cli.jar` (the OWL API is bundled inside it), runs it on the bundled **Systems Engineering 101** ontology, and pops the result open in your browser. The CLI prints `Wrote <absolute path>` so you can always find the file.

Prefer Gradle to do the running?

```bash
./gradlew run --args="src/test/resources/pizza.owl"   # writes pizza-sbvr.html next to the input
```

Feed it any OWL file — it accepts OWL (`.owl`), Turtle (`.ttl`), and RDF/XML (`.rdf`), and the file extension picks the parser. The default output is **SBVR Structured English**; add `--manchester`, `--ose`, or `--rosetta` to switch reading modes. See [`docs/cli-reference.md`](docs/cli-reference.md) for every flag.

## What you'll see

The page reads like English, with each word color-coded by the role it plays (the color is always paired with a typographic cue, so it works in grayscale too):

- **term** — green, underlined — a concept (class), e.g. *System*
- **verb** — blue, italic — a fact type (relationship), e.g. *performs function*
- **keyword** — orange, bold — an SBVR keyword, e.g. *Each*, *only*, *at least one*
- **name** — teal, double-underlined — an individual

So a real sentence from SE-101 reads: `Each System performs function at least one Function .` Hover any colored word in a browser and a panel at the bottom tells you what it means. Full walkthrough in [`docs/reading-the-output.md`](docs/reading-the-output.md).

## Documentation map

New to ontologies? **Start with the primer**, then read your first report.

- [`docs/concepts.md`](docs/concepts.md) — *Ontologies in plain English.* What a class, individual, property, axiom, and restriction actually are. **Start here.**
- [`docs/reading-the-output.md`](docs/reading-the-output.md) — Decode the report: colors, roles, hover glosses, and the three reference glossaries.
- [`docs/formats.md`](docs/formats.md) — Four ways to read an ontology: SBVR, OSE, Manchester, and the side-by-side Rosetta view.
- [`docs/cli-reference.md`](docs/cli-reference.md) — Every command-line flag, with recipes.
- [`docs/library-guide.md`](docs/library-guide.md) — Embed the verbalizer in your own Java code (or the Groovy script).
- [`docs/validation.md`](docs/validation.md) — Run `--validate` to find logical errors, and read the result.
- [`docs/examples.md`](docs/examples.md) — The bundled SE-101 ontology and the real IOF / BFO ontologies.
- [`docs/troubleshooting.md`](docs/troubleshooting.md) — Error messages, odd reports, and FAQs.
- [`docs/glossary.md`](docs/glossary.md) — One-line definitions for every term you'll meet.

## Use it from your own code

It's a small library too, with a single runtime dependency (the OWL API). The Maven/Gradle coordinates are `com.ontologyvision:ontology-to-english:0.4.0`.

```java
OWLOntology ont = OWLManager.createOWLOntologyManager()
        .loadOntologyFromOntologyDocument(new File("pizza.owl"));
String html = new OntologyVerbalizer().verbalizeOntology(ont, "Pizza");        // full report
String some = new OntologyVerbalizer().verbalizeEntities(ont, entities, "Selection"); // a subset
```

Each call returns a complete, self-contained HTML document. There's also a Groovy version of the exact same thing in [`scripts/sbvr.groovy`](scripts/sbvr.groovy) for tools that already have Groovy + the OWL API on the classpath. Full details — dependency setup, `VerbalizerOptions`, and validation from code — are in [`docs/library-guide.md`](docs/library-guide.md).

## Roadmap

Tool #1 is one piece of a bigger plan with three pillars — **READ** (multi-format verbalization, largely done), **CREATE** (round-trip authoring and drafting an ontology from text), and **REASON & VALIDATE** (today's `--validate`, plus more checks to come). See [`ROADMAP.md`](ROADMAP.md).

## Help the Ontology Muggles

Do you want to help? Please do, by writing issues on the project for new features and corrections. The aim is to integrate where we see gaps in specific tools and technologies too — Systems Engineering, Enterprise Architecture, AI, and more.

## License

MIT, © 2026 Daniel Brookshier. See [`LICENSE`](LICENSE).
