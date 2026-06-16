# Troubleshooting & FAQ

This guide is for anyone who hit an error message, got an empty or odd-looking report, or has a quick "how do I...?" question — no prior OWL knowledge needed.

Each entry quotes the message exactly as it appears on screen, names the cause in plain words, and gives you the fix. Messages are reproduced verbatim, so you can match what you see. If you want the full flag list while you debug, keep [cli-reference.md](cli-reference.md) open in another tab.

## Quick index

- [I don't have Java](#i-dont-have-java) — setup
- [`input ontology not found`](#input-ontology-not-found) — wrong path or extension
- [`unknown option`](#unknown-option) — a mistyped flag
- [`--validate needs a reasoner on the classpath`](#validate-needs-a-reasoner) — you ran the slim library jar
- [`(could not open a browser: ...)`](#could-not-open-a-browser) — headless machine, on `--open`
- [The report is missing superclasses](#the-report-is-missing-superclasses) — unresolved imports
- [The IOF example files are missing](#the-iof-example-files-are-missing) — fetch them first
- [No colors, and hovering does nothing](#no-colors-and-hovering-does-nothing) — a non-browser viewer
- [Labels look wrong in Manchester](#labels-look-wrong-in-manchester) — Manchester uses IRI fragments
- [FAQ](#faq)

## Setup

### I don't have Java

The only thing you must install is a **Java 17+ JDK**. The Gradle wrapper (`./gradlew`) fetches everything else — Gradle itself, the OWL API, and the ELK reasoner — the first time you build, so you do not install those by hand.

Check what you have:

```bash
java -version
```

If that prints a version below 17, or "command not found", install a current JDK (for example Temurin/Adoptium or the Oracle JDK) and try again. Once `java -version` reports 17 or higher, build the tool:

```bash
./gradlew cliJar
```

That produces the self-contained CLI jar at `build/libs/ontology-to-english-0.4.0-cli.jar` with the OWL API and the ELK reasoner bundled inside.

## Error messages

### `input ontology not found`

You ran the tool but it printed:

```
input ontology not found: /full/path/you/typed.owl
```

and stopped with exit code **2**.

The first argument must be the path to an ontology file that actually exists. The message echoes the **absolute** path the tool looked at, so read it carefully — the usual causes are a typo in the path, a relative path run from the wrong directory, or a missing/extra file extension. The tool accepts `.owl`, `.ttl`, and `.rdf`, and the **extension selects the parser**, so `mymodel` with no extension will not load even if the bytes are valid Turtle.

Confirm the file is there, then re-run pointing at it:

```bash
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --open
```

### `unknown option`

A flag you passed was not recognized:

```
unknown option: --colour
```

This exits with code **2**. Any argument that starts with `-` and is not a known flag triggers this — almost always a typo (`--colour` for `--color`) or American/British spelling. See [cli-reference.md](cli-reference.md) for the complete, correctly-spelled list. A few easy ones to get wrong:

- `--color full|mono|plain` takes a value (`full` is the default).
- The section toggles are `--no-verbalization`, `--no-model`, `--no-owl`, `--no-rdf` — all are on by default, so you only add them to turn a section off.
- The format flags are `--manchester`, `--ose`, and `--rosetta`. They overwrite each other, so if you pass more than one, the **last one wins**. (`--open` and `--validate` are independent — they stack with anything.)

### `--validate needs a reasoner`

You ran `--validate` and instead of a `VALIDATION:` line you got:

```
--validate needs a reasoner on the classpath. The CLI jar bundles ELK; if you run the slim library jar, add io.github.liveontologies:elk-owlapi.
```

The reasoner (ELK) is what actually checks the ontology, and it is bundled **only in the CLI jar**. This message means you are running the slim library jar, which has no reasoner. Two ways to fix it:

- Run the CLI jar instead — it ships with ELK, so `--validate` works out of the box:

  ```bash
  java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --validate
  ```

  On `examples/se-101.ttl` you should then see:

  ```
  VALIDATION: consistent; no unsatisfiable classes.
  ```

- Or add the reasoner to the classpath yourself: `io.github.liveontologies:elk-owlapi`.

For what the three validation outcomes mean — including the difference between an *inconsistent* ontology and an *unsatisfiable class* — see [validation.md](validation.md).

### `(could not open a browser: ...)`

You added `--open`, the report was written, but you also saw:

```
(could not open a browser: ...)
```

This is **not a failure**. It usually means a headless machine or server with no desktop browser to launch. The tool reports that it could not open a browser, but it has already written the file and the run continues normally — look for the `Wrote` line printed just above:

```
Wrote /full/path/to/se-101-sbvr.html
```

That path is **absolute**, so copy it and open the HTML file yourself in any browser. Everything in the report is there; only the auto-open step was skipped.

## Odd or incomplete reports

### The report is missing superclasses

If the verbalization looks thin — concepts that should have parents show up bare, or a multi-file ontology reads as if half of it is gone — the usual cause is **unresolved imports**. An ontology often pulls in other files via `owl:imports`; if the tool cannot find those files, the imported axioms are not there to verbalize.

The tool resolves imports automatically from the **input file's own folder**: it looks at sibling files and at a **`catalog-v001.xml`** (an OASIS catalog that maps import IRIs to local files) in that folder. Anything it still cannot find is **tolerated silently** — the file you pointed at still verbalizes, just without the missing pieces.

So the fix is to put the imported files where the tool looks:

- Place the imported `.owl`/`.ttl`/`.rdf` files **next to** your input file, or
- Add a `catalog-v001.xml` in that folder mapping each import IRI to its local file.

The bundled IOF example does exactly this — `./gradlew fetchIofExample` writes a `catalog-v001.xml` alongside the fetched files so imports resolve offline. See [examples.md](examples.md) for that layout and [cli-reference.md](cli-reference.md) for the import-resolution details.

### The IOF example files are missing

If a command like `java -jar build/libs/ontology-to-english-*-cli.jar examples/iof/Core.rdf` fails because `examples/iof/` is empty or absent, that is expected on a fresh checkout. The IOF and BFO files are **git-ignored and not committed** to the repo — you fetch them on demand:

```bash
./gradlew fetchIofExample
```

That downloads IOF Core (`Core.rdf`), the IOF AnnotationVocabulary (`AnnotationVocabulary.rdf`), and BFO 2020 (`bfo-core.owl`) into `examples/iof/`, along with a generated `catalog-v001.xml` (for offline import resolution) and a `NOTICE.md`. See [examples.md](examples.md) for what each file is and the attribution you must keep when redistributing BFO.

### No colors, and hovering does nothing

If the report reads fine but everything is one color and hovering a word shows nothing, you are almost certainly viewing it in a context **without JavaScript** — most commonly an embedded Swing viewer (a `JEditorPane`). The hover panel and the in-page toolbar both need a real browser with JavaScript; in a static or embedded view the page is still fully readable, but the hover lookups and toolbar controls are inert. Open the HTML in a web browser to get the interactive behavior.

A few related checks:

- **Colors are off in the browser too.** Color is controlled by `--color full|mono|plain`, and the default is `full`. If you (or someone) generated the report with `--color plain`, color is intentionally dropped — regenerate with `--color full`. Note that `mono` deliberately drops color but keeps the typography, so you still get **term** underlined, **verb** italic, **keyword** bold, and **name** double-underlined.
- **Hovering shows no panel.** The hover panel is on by default but is turned off by `--no-rollover`; if the report was generated with that flag, there are no hover glosses to show. Regenerate without `--no-rollover`.

The color vocabulary is consistent throughout the docs: **term** = green (a concept/class), **verb** = blue (a fact type/relationship), **keyword** = orange (an SBVR keyword like `Each` or `only`), **name** = teal (an individual), and a **literal** = plain near-black (a data value). For the full legend and how hover works, see [reading-the-output.md](reading-the-output.md).

### Labels look wrong in Manchester

If you switched to Manchester (`--manchester` or the Manchester column in `--rosetta`) and the names look cryptic or appear in the wrong language, that is by design. Manchester syntax uses the **IRI fragment** — the short name at the end of a term's identifier — **not** the human-readable `rdfs:label`. So you see frame keywords and fragments like:

```
performsFunction some Function
```

rather than the friendlier SBVR phrasing:

```
Each Component has interface only Interface .
```

This is why a class whose label is in another language still shows as its English-ish IRI fragment in Manchester (the pizza ontology's labels are Portuguese, but Manchester shows the fragments). It is not a bug — Manchester is the formal syntax engineers type in tools like Protégé. For the friendly, labeled reading, use the default SBVR format (or OSE). See [formats.md](formats.md) for a full side-by-side comparison.

## FAQ

### Which format should I use?

- **SBVR Structured English** (the default) — the most readable, best for novices and stakeholders. Reads like `Each System performs function at least one Function .`
- **OSE (OWL Simplified English)** — the same walk of the ontology with two keywords swapped: `Each` becomes `Every` and `at least one` becomes `some` (so `Every System performs function some Function`). Leaner prose; choose it with `--ose`.
- **Manchester** — the formal OWL syntax engineers round-trip with tools like Protégé; choose it with `--manchester`.
- **Rosetta** — all three side by side in one table, sharing a single glossary so you can learn to read across formats; choose it with `--rosetta`. (Rosetta only appears when more than one format is selected.)

Full details and examples are in [formats.md](formats.md).

### What's the difference between "inconsistent" and "unsatisfiable"?

These two sound alike but mean different things:

- An **inconsistent** ontology contradicts itself as a whole — there is no valid way to interpret it at all. Validation prints `VALIDATION: INCONSISTENT — a reasoner found a contradiction in the ontology.`
- An **unsatisfiable class** is a single class that can never have any instance (usually from contradictory restrictions or disjointness), even though the rest of the ontology is perfectly fine. Validation prints `VALIDATION: consistent, but N unsatisfiable class(es) — each can never have an instance:` and then lists each class.

When the ontology is inconsistent, the unsatisfiable list comes back empty — the reasoner stops at the global contradiction rather than hunting for individual bad classes. [validation.md](validation.md) walks through all three outcomes; [concepts.md](concepts.md) defines both terms from scratch.

### Can I verbalize only certain entities instead of the whole ontology?

Yes — from your own code. The library's `verbalizeEntities` entry point takes a chosen `Set` of entities and verbalizes only those, instead of the entire ontology. See [library-guide.md](library-guide.md) for the call. The CLI always verbalizes the whole input file.

### Which input formats are supported?

Three: OWL (`.owl`), Turtle (`.ttl`), and RDF/XML (`.rdf`). The **file extension selects the parser**, so name your file with the matching extension. The bundled example `examples/se-101.ttl` is Turtle; the IOF example `examples/iof/Core.rdf` is RDF/XML.

### Where does the output file go?

If you pass a second (non-flag) argument, that is the output path. If you omit it, the tool writes `<input-basename>-sbvr.html` **next to the input** — it strips the input's extension and appends `-sbvr.html`. For example, `./gradlew run --args="src/test/resources/pizza.owl"` writes `pizza-sbvr.html` in the same folder as `pizza.owl`. Either way, the tool prints the absolute path it wrote:

```
Wrote /full/path/to/pizza-sbvr.html
```

### Is there a non-Java way to run it?

There is a Groovy script, `scripts/sbvr.groovy`, that mirrors the CLI for tools that already have Groovy and the OWL API on hand:

```bash
groovy -cp build/libs/ontology-to-english-*-cli.jar scripts/sbvr.groovy examples/se-101.ttl --open
```

Note the Groovy script supports the section, display, and action options — `--no-verbalization`, `--no-model`, `--no-owl`, `--no-rdf`, `--color`, `--no-rollover`, `--title`, and `--open` — but **not** the format flags (`--manchester`/`--ose`/`--rosetta`) or `--validate`; for those, use the CLI jar (`com.ontologyvision.verbalizer.Main`). See [library-guide.md](library-guide.md) for more.

---

Still stuck? The other guides cover the underlying ideas in depth: [concepts.md](concepts.md) for the vocabulary, [reading-the-output.md](reading-the-output.md) for the report page, [cli-reference.md](cli-reference.md) for every flag, [validation.md](validation.md) for `--validate`, and the project [../README.md](../README.md) and [../ROADMAP.md](../ROADMAP.md) for the big picture.
