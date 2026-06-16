# Command-line reference

This guide is for anyone running the `ontology-to-english` CLI jar (or `./gradlew run`) who wants the complete, authoritative list of flags, arguments, and exit codes — with copy-paste recipes for common goals.

## Quick start

The fastest path is to build the self-contained CLI jar once, then point it at an ontology file:

```bash
./gradlew cliJar
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --open
```

That reads the bundled Systems Engineering 101 ontology, writes `examples/se-101-sbvr.html` next to the input, prints the path it wrote, and opens the page in your browser. The result reads like English — for example:

```
Each System performs function at least one Function .
```

If you would rather not build a jar, the Gradle `run` task does the same thing:

```bash
./gradlew run --args="src/test/resources/pizza.owl"   # writes pizza-sbvr.html next to the input
```

The CLI jar bundles the OWL API and the ELK reasoner, so it runs anywhere with a Java 17+ JDK and nothing else installed. In the runnable examples below the wildcard `build/libs/ontology-to-english-*-cli.jar` is used so you never have to track the version number by hand.

## Synopsis

```bash
java -jar ontology-to-english-0.4.0-cli.jar <input.owl|.ttl|.rdf> [output.html] [flags]
```

Asking for help prints the usage block (to stderr) and stops:

```bash
java -jar build/libs/ontology-to-english-*-cli.jar --help
```

```
usage: sbvr <input.owl|.ttl|.rdf> [output.html]
            [--manchester | --ose | --rosetta]   (default: SBVR Structured English;
                                                 --rosetta = SBVR | OSE | Manchester side-by-side)
            [--no-verbalization] [--no-model] [--no-owl] [--no-rdf]
            [--color full|mono|plain] [--no-rollover] [--title T] [--open]
            [--validate]   (run a reasoner: consistency + unsatisfiable classes)
```

The same usage block prints whether you pass `-h`, `--help`, or no arguments at all. The only difference is the exit code (see [Exit codes and errors](#exit-codes-and-errors)).

## Positional arguments

| Position | Required? | Meaning |
| --- | --- | --- |
| 1 | yes | The input ontology file — OWL (`.owl`), Turtle (`.ttl`), or RDF/XML (`.rdf`). |
| 2 | no | The output HTML file. This is simply the first non-flag argument that comes after the input. |

Anything starting with `-` is treated as a flag; the second bare (non-flag) argument becomes the output path. So both of these work, and mean the same destination:

```bash
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl report.html
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --open report.html
```

### Where the output file goes

If you omit the second argument, the tool derives the filename from the input: it strips the input's extension (using the regex `\.[^.]+$`) and appends `-sbvr.html`, then writes it **next to the input file**. So `examples/se-101.ttl` becomes `examples/se-101-sbvr.html`, and `pizza.owl` becomes `pizza-sbvr.html`. The file is always written as UTF-8.

## Input formats

The file extension picks the parser. `.owl`, `.ttl`, and `.rdf` are all accepted; the OWL API loads each accordingly. If you have a model in another OWL tool (Cameo Concept Modeler, Protégé, TopBraid), export it to one of those formats first and run the CLI on the exported file.

## Option reference

Every section and display toggle is **on by default**, and the default color level is **full**. Turning things off is opt-in.

| Flag | Takes a value? | Effect | Default |
| --- | --- | --- | --- |
| `--manchester` | no | Render Manchester syntax only. | off (SBVR) |
| `--ose` | no | Render OWL Simplified English only. | off (SBVR) |
| `--rosetta` | no | Render SBVR, OSE, and Manchester side by side. | off (SBVR) |
| `--no-verbalization` | no | Drop the English verbalization section. | section on |
| `--no-model` | no | Drop the Model Glossary (this ontology's own vocabulary). | section on |
| `--no-owl` | no | Drop the OWL Constructs reference glossary. | section on |
| `--no-rdf` | no | Drop the RDF / RDFS Constructs reference glossary. | section on |
| `--no-rollover` | no | Drop the in-browser hover panel and its data. | rollover on |
| `--color full\|mono\|plain` | yes | Set the color level of the report. | `full` |
| `--title T` | yes | Set the report title. | the input filename |
| `--open` | no | Open the finished report in your browser. | off |
| `--validate` | no | Run the ELK reasoner and print a consistency report. | off |
| `-h`, `--help` | no | Print the usage block and exit. | — |

### Format flags (mutually exclusive — last one wins)

The format flags choose how each axiom is phrased. The default, with no format flag, is **SBVR Structured English** — the verbose, color-coded prose aimed at non-experts:

```
Each System has component at least 2 Component .
```

- `--manchester` — formal OWL 2 Manchester syntax, the kind engineers type in Protégé. It uses IRI fragment names rather than human-readable labels, so the same idea reads `performsFunction some Function`.
- `--ose` — OWL Simplified English, a leaner cousin of SBVR. It walks the exact same axioms but swaps two keywords: `Each` becomes `Every`, and `at least one` becomes `some` (so `Each System performs function at least one Function .` reads `Every System performs function some Function .`).
- `--rosetta` — all three (SBVR | OSE | Manchester) in one side-by-side table that shares a single glossary, so hovering `some` in the Manchester column and `at least one` in the SBVR column shows the **same** underlying meaning. This is the teaching view.

These three are mutually exclusive: each one **replaces** the chosen format list, so if you pass more than one, **the last one on the command line wins**. For the full tour of what each format looks like, see [formats.md](formats.md).

```bash
# --manchester is ignored here; --rosetta is last, so you get the side-by-side table
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --manchester --rosetta
```

### Section toggles

All four sections render by default. Each `--no-...` flag removes one:

- `--no-verbalization` — omit the English sentences entirely (leaving only the glossaries you keep).
- `--no-model` — omit the Model Glossary, which lists this ontology's own concepts and relationships.
- `--no-owl` — omit the OWL Constructs glossary (the static built-in reference of OWL terms).
- `--no-rdf` — omit the RDF / RDFS Constructs glossary (the static built-in reference of RDF/RDFS terms).

The OWL and RDF/RDFS glossaries are the same reference tables in every report, so dropping them with `--no-owl --no-rdf` is the usual way to make a short, audience-specific handout. See [reading-the-output.md](reading-the-output.md) for what each section contains.

### Display flags

- `--color full|mono|plain` consumes the next argument and is **case-insensitive** (`Full`, `MONO`, and `plain` all work). It maps to the report's color level:
  - `full` (default) — full color: <span>term</span> in green (a concept), <span>verb</span> in blue (a fact type), <span>keyword</span> in orange, <span>name</span> in teal (an individual), and literals in plain near-black.
  - `mono` — drops the colors but keeps the typography (underline, italic, bold), which is friendlier for color-blind readers and grayscale printing.
  - `plain` — drops both color and typographic styling.
- `--no-rollover` removes the hover panel and the data behind it. By default (rollover on) hovering a colored term, verb, keyword, or name **in a browser** shows its meaning in a panel at the bottom of the page. Turning it off makes a smaller, simpler file with no hover lookups.
- `--title T` consumes the next argument and sets the report's title. If you omit it, the title defaults to the input filename.

### Action flags

`--open` and `--validate` are independent of each other and of every other flag — you can combine them freely.

- `--open` opens the finished report in your system browser after writing it. If a browser cannot be opened (for example on a headless server), the tool prints `(could not open a browser: ...)` to stderr but **the file is still written and the run continues** — it is always safe to add.
- `--validate` runs reasoner-based validation **after** the report is written. The CLI jar bundles the ELK reasoner, so this works out of the box. It prints one of three results; see [validation.md](validation.md) for how to read them. On the bundled SE-101 ontology you will see:

  ```
  VALIDATION: consistent; no unsatisfiable classes.
  ```

## Import resolution

When your ontology imports others, the CLI tries to resolve those imports automatically. It installs an `AutoIRIMapper` over the **folder that holds the input file**, so it can pull in:

- **sibling files** in the same directory, and
- a `catalog-v001.xml` (an OASIS XML catalog mapping import IRIs to local files) in that directory.

This best-effort resolution is what lets multi-file ontologies — such as the fetched IOF Core + BFO — verbalize **with** their superclasses, even offline. Anything that still cannot be resolved is **tolerated silently** (`MissingImportHandlingStrategy.SILENT`): you get no error, the run continues with whatever loaded. If a report looks thin or is missing superclasses, an unresolved import is the usual cause — put the sibling files or a `catalog-v001.xml` next to the input. See [examples.md](examples.md) for how the IOF fetch lays out its catalog, and [troubleshooting.md](troubleshooting.md) for diagnosing missing content.

## Exit codes and errors

| Exit code | When |
| --- | --- |
| `0` | The report was written successfully, **or** you asked for help with `-h` / `--help`. |
| `2` | No arguments at all, an unknown option, or the input file was not found. |

The user-visible strings, verbatim:

- On success the tool prints to stdout: `Wrote <absolute path>` (the full absolute path of the file it wrote).
- An unknown flag (any argument starting with `-` that is not a recognized option) prints `unknown option: <flag>` to stderr and exits `2`.
- A missing input file prints `input ontology not found: <path>` to stderr and exits `2`.
- A failed `--open` prints `(could not open a browser: ...)` to stderr but does **not** change the exit code — the file is still written.

```bash
java -jar build/libs/ontology-to-english-*-cli.jar nope.ttl
# input ontology not found: /abs/path/to/nope.ttl   (exit 2)

java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --colour mono
# unknown option: --colour   (exit 2 — note the British spelling typo)
```

## Recipes

```bash
# Formal syntax beside plain English, opened in your browser
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --rosetta --open

# Find logical errors (runs the bundled ELK reasoner)
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --validate

# Color-blind / grayscale-print friendly (keep underline/italic/bold, drop color)
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --color mono --open

# A lean stakeholder handout: just this ontology's vocabulary, no OWL/RDF reference tables
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --no-owl --no-rdf --open

# A custom title and a chosen output path
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl se-101.html --title "Systems Engineering 101"
```

## Groovy alternative

If your environment already has Groovy and the OWL API, `scripts/sbvr.groovy` mirrors the CLI without a separate jar. Run it with the CLI jar on the classpath:

```groovy
groovy -cp build/libs/ontology-to-english-*-cli.jar scripts/sbvr.groovy examples/se-101.ttl --open
```

The Groovy script supports the same positional arguments, output-filename rule, and silent missing-import handling. It accepts the section and display flags — `--no-verbalization`, `--no-model`, `--no-owl`, `--no-rdf`, `--no-rollover`, `--color`, `--title`, and `--open` — but it does **not** include the format flags (`--manchester` / `--ose` / `--rosetta`) or `--validate`; for those, use the Java CLI. For embedding the verbalizer in your own code, see [library-guide.md](library-guide.md).

## See also

- [concepts.md](concepts.md) — what concepts, fact types, restrictions, and the rest actually mean.
- [reading-the-output.md](reading-the-output.md) — decode the colors, glossaries, and hover panel.
- [formats.md](formats.md) — SBVR, OSE, Manchester, and the Rosetta side-by-side view in depth.
- [validation.md](validation.md) — how to read each `--validate` result.
- [examples.md](examples.md) — the bundled SE-101 ontology and the on-demand IOF fetch.
- [troubleshooting.md](troubleshooting.md) — every error message mapped to a cause and fix.
- [glossary.md](glossary.md) — quick term lookups.
- [../README.md](../README.md) — the front door. [../ROADMAP.md](../ROADMAP.md) — what is planned next.
