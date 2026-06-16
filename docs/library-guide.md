# Using OntologyForDummies from your own code

This guide is for Java developers who want to embed the verbalizer in another tool (for example, a MagicDraw/MSOSA plugin) or drive it from a Groovy script, instead of running the command line.

If you only want to run the tool, start with the [command-line reference](cli-reference.md). If you want to understand the colored report your code will produce, read [reading the report](reading-the-output.md). This guide assumes you are comfortable with Java, Maven/Gradle, and the [OWL API](glossary.md).

The whole library is two calls: load an ontology, then ask `OntologyVerbalizer` to turn it into a self-contained HTML string. Here is the smallest useful program.

```java
import com.ontologyvision.verbalizer.OntologyVerbalizer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import java.io.File;
import java.nio.file.Files;

OWLOntology ont = OWLManager.createOWLOntologyManager()
        .loadOntologyFromOntologyDocument(new File("se-101.ttl"));

String html = new OntologyVerbalizer().verbalizeOntology(ont, "Systems Engineering 101");

Files.writeString(new File("se-101-sbvr.html").toPath(), html);
```

That `html` string is a complete, standalone HTML document — the same colored, hyperlinked page the CLI writes, full of sentences like `Each System performs function at least one Function .` and `No Component is a Function or Requirement .`. You can write it to a file, push it into a browser, or drop it into a Swing viewer.

## Add the dependency

The library is published as `com.ontologyvision:ontology-to-english:0.4.0` (group : artifact : version). Its only runtime dependency is the OWL API, `net.sourceforge.owlapi:owlapi-distribution:5.5.1`, which the build system pulls in transitively.

Gradle:

```gradle
dependencies {
    implementation 'com.ontologyvision:ontology-to-english:0.4.0'
}
```

Maven:

```xml
<dependency>
  <groupId>com.ontologyvision</groupId>
  <artifactId>ontology-to-english</artifactId>
  <version>0.4.0</version>
</dependency>
```

If you do not have a shared registry, publish the library to your local Maven cache and depend on it offline:

```bash
./gradlew publishToMavenLocal
```

That installs the library (plus a sources jar) into `~/.m2/repository` with no credentials and no network, so any project on the same machine can resolve `com.ontologyvision:ontology-to-english:0.4.0`.

## Library jar vs CLI jar

There are two artifacts, and the difference matters for [validation](validation.md):

- The **published library** carries the OWL API as a normal dependency in its POM, and nothing else. It does **not** bundle a reasoner. This is what you get from `publishToMavenLocal` or a registry.
- The **CLI jar** (`build/libs/ontology-to-english-*-cli.jar`, built with `./gradlew cliJar`) is self-contained: it bundles the OWL API *and* the ELK reasoner. This is what you run with `java -jar`, and what the Groovy script wants on its classpath.

The verbalizer itself never needs a reasoner — only validation does, and validation lets you supply your own (see [Optional validation](#optional-validation) below). So the library stays lightweight, and you opt into a reasoner when you actually want one.

## The two entry points

`OntologyVerbalizer` has exactly two public methods, each with a `VerbalizerOptions` overload. All four return a complete, self-contained HTML `String`.

```java
OntologyVerbalizer v = new OntologyVerbalizer();

// 1. Verbalize the whole ontology.
String full = v.verbalizeOntology(ont, "Systems Engineering 101");

// 2. Verbalize only a chosen set of entities (e.g. a user's selection in a modeling tool).
Set<OWLEntity> picked = Set.of(systemClass, hasComponentProperty);
String subset = v.verbalizeEntities(ont, picked, "Selection");
```

`verbalizeEntities` is what you call from a context menu: hand it the classes, properties, and individuals the user selected and it reports just those (plus the axioms that mention them). If the selection has nothing to say, the report shows a friendly note rather than an empty page.

The `title` argument is the heading at the top of the page; it also appears in the document `<title>`. There is no separate "format" argument here — formats and everything else are configured through `VerbalizerOptions`.

## Configuring the report with VerbalizerOptions

`VerbalizerOptions` is an immutable value object built through a builder. `VerbalizerOptions.DEFAULT` is everything on: all sections, full color, and rollover (hover) enabled — exactly what the no-options calls above use.

```java
import com.ontologyvision.verbalizer.VerbalizerOptions;
import com.ontologyvision.verbalizer.VerbalizerOptions.ColorLevel;
import com.ontologyvision.verbalizer.VerbalizerOptions.Format;

VerbalizerOptions opts = VerbalizerOptions.builder()
        .formats(Format.SBVR, Format.MANCHESTER)   // more than one -> the Rosetta side-by-side view
        .colorLevel(ColorLevel.MONO)               // drop color, keep underline/italic/bold
        .includeOwlGlossary(false)                 // omit the built-in OWL reference table
        .includeRdfGlossary(false)                 // omit the built-in RDF/RDFS reference table
        .rollover(true)                            // emit hover glosses (default)
        .build();

String html = new OntologyVerbalizer().verbalizeOntology(ont, "Pizza", opts);
```

The builder methods, each returning the builder so you can chain them:

| Method | Effect | Default |
| --- | --- | --- |
| `includeVerbalization(boolean)` | Emit the SBVR/OSE/Manchester sentences (the main content). | `true` |
| `includeModelGlossary(boolean)` | Emit the **Model Glossary** — this ontology's own vocabulary. | `true` |
| `includeOwlGlossary(boolean)` | Emit the **OWL Constructs** reference table (37 built-in terms). | `true` |
| `includeRdfGlossary(boolean)` | Emit the **RDF / RDFS Constructs** reference table (22 built-in terms). | `true` |
| `rollover(boolean)` | Emit hover glosses (see [the trade-off below](#the-rollover-trade-off)). | `true` |
| `colorLevel(ColorLevel)` | Pick `FULL`, `MONO`, or `PLAIN`. | `FULL` |
| `formats(Format...)` | Pick one or more output formats. | `SBVR` |

A section turned off here is simply absent from the HTML. (The report also has an in-page toolbar that can *hide* present sections at view time in a browser — but that only affects what is visible, not what your code wrote. See [reading the report](reading-the-output.md).)

### Choosing formats in code

`Format` is `SBVR`, `OSE`, or `MANCHESTER` (all caps). Pass one for a single-format report; pass more than one and you get the **Rosetta** side-by-side table, where each column shows the same fact in a different language and they share one glossary. Passing an empty or `null` array falls back to `SBVR`.

```java
.formats(Format.SBVR)                                  // SBVR Structured English (the default)
.formats(Format.OSE)                                   // OWL Simplified English
.formats(Format.MANCHESTER)                            // Manchester Syntax
.formats(Format.SBVR, Format.OSE, Format.MANCHESTER)   // Rosetta — all three, side by side
```

The same idea reads differently in each. SBVR says `Each System performs function at least one Function .`; OSE swaps two keywords to read "Every System performs function some Function"; Manchester uses IRI fragment names and frame keywords like `performsFunction some Function`. See [the four formats](formats.md) for the full comparison.

### Color levels

`ColorLevel` controls how much styling the report carries:

- `FULL` — the four role colors: **term** green (a concept/class), **verb** blue (a fact type/relationship), **keyword** orange (an SBVR keyword like `Each` or `at least one`), and **name** teal (an individual). Literals are plain near-black.
- `MONO` — drops the color but keeps the typographic cues (underline, italic, bold), so roles are still distinguishable in grayscale or print.
- `PLAIN` — drops both color and type styling; everything reads as flat text.

In a browser the reader can also flip between these three at view time with the toolbar, but `colorLevel` sets the starting point and is the only choice an embedded Swing viewer will show.

### The rollover trade-off

With `rollover(true)` (the default), styled terms, verbs, keywords, and names are emitted with their plain-English meaning attached, plus a bottom hover panel. Hover any colored word **in a browser** and its meaning appears in the panel — for example, hovering the orange `at least one` keyword shows the `owl:someValuesFrom` gloss. (The small inline script that reads those meanings is always present regardless — it also drives the in-page toolbar.)

With `rollover(false)` the per-word meanings and the bottom hover panel are omitted. The file is smaller and renders the same static text, but there is no hover lookup. Turn it off when you are generating many reports, embedding in a non-interactive surface, or simply do not need the glossary-on-hover behavior. (The inline toolbar script is still emitted.)

## What to do with the HTML

The returned string is a single self-contained document — CSS and JavaScript are inlined, so there are no external files to ship. You have two main destinations:

- **A browser.** Write it to a `.html` file and open it (or serve it). Here the JavaScript works: the color/section toggles in the toolbar and the hover panel are all live. This is the richest view.
- **A Swing `JEditorPane`.** You can set the HTML directly into a pane for an in-app preview. Swing ignores the `<script>`, so the toolbar and hover panel are inert — but the page is still fully readable as static text, with the color level you chose baked in. (The Rosetta side-by-side table also renders statically, so it works in Swing too.)

```java
JEditorPane pane = new JEditorPane("text/html", html);
pane.setEditable(false);
```

## Loading ontologies (and resolving imports)

The examples above load a single file with a plain `OWLManager`. That is enough when the ontology has no imports, or when you do not mind missing superclasses from unresolved imports.

If your ontology imports others — like the IOF Core example, which imports BFO — mirror what the command line does so the imports resolve and the report shows the full picture. The CLI does two things: it tolerates anything it still cannot find, and it points an `AutoIRIMapper` at the input's folder so sibling files and a `catalog-v001.xml` resolve automatically.

```java
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.io.FileDocumentSource;

File input = new File("examples/iof/Core.rdf");
OWLOntologyManager m = OWLManager.createOWLOntologyManager();

// Resolve imports from sibling files / a catalog-v001.xml in the input's folder.
m.getIRIMappers().add(new AutoIRIMapper(input.getAbsoluteFile().getParentFile(), false));

// Tolerate anything still missing, so a partial ontology still verbalizes.
OWLOntologyLoaderConfiguration cfg = new OWLOntologyLoaderConfiguration()
        .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

OWLOntology ont = m.loadOntologyFromOntologyDocument(new FileDocumentSource(input), cfg);
```

See [examples](examples.md) for how the IOF fetch lays out those sibling files and the catalog, and [troubleshooting](troubleshooting.md) if superclasses go missing.

## Optional validation

To check an ontology for logical errors from code, use `Validator`. It is deliberately reasoner-agnostic: it speaks only OWL API interfaces, so *you* supply the reasoner factory. That is why the core library has no hard reasoner dependency — you bring ELK, HermiT, or any other OWL API reasoner.

```java
import com.ontologyvision.verbalizer.Validator;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;   // ELK ships only in the CLI jar

OWLReasonerFactory factory = new ElkReasonerFactory();
Validator.Result r = new Validator().validate(ont, factory);

if (r.ok()) {
    // consistent and nothing unsatisfiable
} else if (!r.consistent) {
    // the whole ontology contradicts itself
} else {
    for (org.semanticweb.owlapi.model.OWLClass c : r.unsatisfiable) {
        // each class that can never have an instance
    }
}
```

`Validator.Result` has two public fields — `boolean consistent` and `List<OWLClass> unsatisfiable` — plus an `ok()` method that is true only when the ontology is consistent and nothing is unsatisfiable. Note that `unsatisfiable` is only populated when the ontology is consistent; if the ontology is inconsistent the reasoner stops at the global contradiction and that list is empty.

Remember the [jar distinction](#library-jar-vs-cli-jar): `ElkReasonerFactory` is bundled in the CLI jar, but **not** pulled in by the published library. If you depend on the slim library and want ELK, add it yourself:

```gradle
runtimeOnly 'io.github.liveontologies:elk-owlapi:0.6.0'
```

ELK is fast and covers the OWL 2 EL profile (Apache-2.0). For full OWL 2 DL you can supply HermiT (LGPL) as the factory instead. For how to read the three outcomes and what "unsatisfiable" means, see [validation](validation.md).

## Driving it from Groovy

If your environment already has Groovy and the OWL API, `scripts/sbvr.groovy` mirrors the command line without a build step. Put the CLI jar on the classpath:

```bash
./gradlew cliJar
groovy -cp build/libs/ontology-to-english-*-cli.jar scripts/sbvr.groovy pizza.owl --open
```

No Groovy on your `PATH`? Run it through `java` instead:

```bash
java -cp "groovy-all.jar:build/libs/ontology-to-english-*-cli.jar" \
     groovy.ui.GroovyMain scripts/sbvr.groovy pizza.owl
```

The script writes `<input-basename>-sbvr.html` next to the input (or to a second positional argument you provide) and prints `Wrote <absolute path>`, just like the CLI. It accepts this subset of flags:

`--no-verbalization`, `--no-model`, `--no-owl`, `--no-rdf`, `--no-rollover`, `--color full|mono|plain`, `--title T`, and `--open`.

Two differences from the full [CLI](cli-reference.md) are worth knowing: the Groovy script always renders **SBVR** (it does not expose `--manchester`, `--ose`, or `--rosetta`), and it has no `--validate`. For format switching or validation, use the CLI jar or call the library directly as shown above.

## Known limitation: no custom-renderer hook yet

Internally the engine is format-agnostic: an `AxiomRenderer` interface (with a `RenderContext`) lets the SBVR, OSE, and Manchester renderers plug in the same way. You might reasonably expect to inject your own renderer for a new output language.

You cannot, yet. `AxiomRenderer` and `RenderContext` exist, but the built-in renderers are package-private implementation details and there is **no public, documented hook** to register a custom one. Treat the three built-in formats as the supported set for now. A public renderer extension point is a **planned** item — see [ROADMAP.md](../ROADMAP.md) for the broader READ / CREATE / REASON & VALIDATE direction.

---

Next steps: the [command-line reference](cli-reference.md) for the full flag list, [validation](validation.md) for reading reasoner output, [the four formats](formats.md) to choose between SBVR, OSE, Manchester, and Rosetta, and the [glossary](glossary.md) for any term above. New to ontologies entirely? Start with the [concepts primer](concepts.md).
