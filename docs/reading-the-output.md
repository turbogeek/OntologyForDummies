# Reading the report — colors, roles, glossaries, and hover

This guide is for anyone who has generated an HTML report and is now staring at the colored page wondering what it all means. By the end you will be able to read every line, look up any word, and tweak what shows up.

If you have not generated a report yet, run the bundled example first:

```bash
./gradlew cliJar
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --open
```

That writes `se-101-sbvr.html` next to the input and opens it in your browser. (See [cli-reference.md](cli-reference.md) for every flag, and [examples.md](examples.md) for what SE-101 models.)

## The page at a glance

From top to bottom, a default report has:

1. **Title** — the ontology's name (or whatever you passed with `--title`).
2. **Legend** — a one-line key telling you what each color means. (Covered below.)
3. **Toolbar** — a small row of controls to change colors and hide sections in a browser.
4. **Four verbalization sections** — the heart of the page: your ontology, written out as English sentences.
5. **Three optional glossaries** — the Model Glossary plus two static reference tables for OWL and RDF terms.
6. **A hover panel** pinned to the bottom of the window that shows the meaning of whatever colored word your mouse is over.

The toolbar and the hover panel are interactive helpers that need a web browser with JavaScript. The sentences and glossaries are plain HTML and read fine anywhere — including an embedded Swing viewer or a printout — they just lose the clicking and hovering.

## The four sections: what each one lists

Each kind of thing in an ontology gets its own section, with these exact headings (the small number after each heading is a count and varies by ontology):

| Heading | What it lists |
| --- | --- |
| `Vocabulary — Concepts (Classes)` | The **kinds of things** (concepts/classes) — like `System` or `Component`. |
| `Fact Types — Relationships (Object Properties)` | The **relationships** (object properties) that link one thing to another — like *has component*. |
| `Attributes (Data Properties)` | The **attributes** (data properties) that attach a plain value — like a number or a name. |
| `Individuals (Instances)` | The **specific named things** (individuals/instances) — one particular `System`, by name. |

So the Concepts section is where you read sentences like:

> `Each Subsystem is a System .`
>
> `Each System performs function at least one Function .`
>
> `Each System has component at least 2 Component .`

For the vocabulary behind these words — what a *concept*, *relationship*, *attribute*, or *individual* actually is — see [concepts.md](concepts.md).

## The color and type legend

Every sentence is color-coded by the **role** each word plays. The colors are doubled up with typography (underline, italic, bold) on purpose, so the page still reads correctly in grayscale or for color-blind readers — always read the *role word*, not just the hue.

| Role | Color | Type style | Means |
| --- | --- | --- | --- |
| **term** | green `#157f3b` | underlined | a concept (class) |
| **verb** | blue `#1b6ca8` | italic | a fact type (relationship) |
| **keyword** | orange `#c8741a` | bold | an SBVR keyword (`Each`, `No`, `only`, `at least one`, `at least N`) |
| **name** | teal `#0a7d7d` | double-underlined | an individual |
| **literal** | plain near-black `#1a1a1a` | plain text | a data value |

Reading the legend against a real sentence makes it click:

> `Each` `System` `performs function` `at least one` `Function` `.`

Here `Each` and `at least one` are orange keywords, `System` and `Function` are green underlined terms (concepts), and `performs function` is the blue italic verb (the fact type). Literals — plain data values — are just near-black text and, unlike the four roles above, are **not** named in the legend.

### The exact legend text

So you can match it on screen, the legend line on a default SBVR report reads verbatim:

```
SBVR Structured English — term (concept), verb (fact type), keyword, name (individual)
```

Those four colored sample words (term, verb, keyword, name) appear in the legend already styled, so the legend doubles as a swatch you can compare against the sentences below it.

## Looking a word up: hover for its meaning

You do not have to memorize anything. With **rollover** on (the default), hovering your mouse over any colored term, verb, keyword, or name in a browser shows that word's meaning in a panel pinned to the bottom of the window.

Until you hover, the panel — a dark bar (`#1f2933` background, light `#f5f7fa` text) across the bottom — shows its prompt:

```
Hover a term, verb, keyword or name to see its meaning.
```

When you hover a styled word, the panel swaps to that word's meaning:

- For a **concept**, **relationship**, **attribute**, or **individual**, it shows that entity's own `rdfs:comment` if the ontology author wrote one. If there is no comment, the tool generates a short gloss from the entity's own axioms, so you still get a meaning.
- For a **keyword**, it explains the SBVR phrasing. Keywords like `Each`, `No`, `only`, and `at least one` are orange-bold *and* carry their own hover glosses, so hovering `at least one` tells you it means "there is at least one such thing" (the existential restriction). Move on to [concepts.md](concepts.md) for the ideas behind these keywords, or [formats.md](formats.md) to see how the same idea reads in other formats.

This hover behavior needs JavaScript. In a static or embedded (Swing) view the page is still completely readable — you just lose the live lookups, so the glossaries below become your reference instead.

## The three reference glossaries

Below the sentences a report can carry up to three glossaries. They answer two different questions, so it helps to know which is which:

- **Model Glossary** — *this* ontology's own vocabulary: every concept, relationship, attribute, and individual you just read, listed with its meaning. The count varies per ontology because it is built from your file.
- **OWL Constructs** — a static table of **37** built-in plain-English explanations of OWL terms. This is the same for every report; it is baked into the jar, not derived from your ontology.
- **RDF / RDFS Constructs** — a static table of **22** built-in plain-English explanations of RDF/RDFS terms, also baked in.

The two static tables are there so that when a sentence (or a hover gloss) mentions a big standards word like `owl:equivalentClass` or `rdfs:domain`, you have a plain definition on the same page. A few real entries from those built-in tables:

- `rdf:type` — *The property stating an individual is an instance of a class; "x rdf:type C" asserts x belongs to C.*
- `rdfs:label` — *A plain, human-readable name for a resource — no logical/inferential weight, purely presentational; often language-tagged.*
- `rdfs:domain` — *States any individual that is the subject of the property must belong to the named class*, which reads in SBVR as "Anything that has a topping is a Pizza."
- `owl:equivalentClass` — *Asserts two class expressions have exactly the same instances in every interpretation — mutual subclassing.*

For a flat, alphabetical lookup of all of these terms outside the report, see [glossary.md](glossary.md).

## The toolbar: change colors and hide sections

In a browser the toolbar near the top lets you adjust the page without regenerating it:

- **SBVR colors** — a selector with **Full color**, **Monochrome (keep type)**, and **Plain**. Full is the default and shows everything above. Monochrome drops the colors but keeps the underline/italic/bold so roles are still distinguishable; Plain drops both. (These match the `--color full|mono|plain` flag and the library's `ColorLevel` — see [cli-reference.md](cli-reference.md).)
- **Hide model glossary / Hide OWL glossary / Hide RDF glossary** — checkboxes that collapse each glossary. A checkbox only appears if that glossary is present in the report.

These controls are convenience tools for a browser. In an embedded Swing `JEditorPane` they are inert — the page still renders, but the selector and checkboxes do nothing, because that viewer ignores the page's JavaScript.

## Caveats worth knowing

- **Double-underline vs. plain underline.** A teal **name** (individual) is *double*-underlined while a green **term** (concept) is single-underlined. At small sizes the two underlines can look like one, so lean on the color (teal vs. green) and the role word to tell an individual from a concept.
- **`(inverse property)` is a placeholder, not a role.** When a relationship is expressed as the inverse of another and has no name of its own, the report writes the literal text `(inverse property)` instead of a colored verb. It is a stand-in, not a clickable role with a hover gloss.
- **Hover and the toolbar need a browser.** Both rely on JavaScript. Open the saved `.html` file in any browser to get the live lookups and controls; in a static or Swing view the sentences and glossaries are still fully readable on their own.
- **A glossary may simply be absent.** The three glossaries are controlled by options (`includeModelGlossary`, `includeOwlGlossary`, `includeRdfGlossary`, or the matching `--no-model` / `--no-owl` / `--no-rdf` flags). If a report was generated with one turned off, that section — and its toolbar checkbox — will not appear.

## Where to go next

- [concepts.md](concepts.md) — the plain-English vocabulary behind every colored word.
- [formats.md](formats.md) — how the same sentence reads in OSE, Manchester, and the side-by-side Rosetta view.
- [cli-reference.md](cli-reference.md) — every flag that changes what appears in the report.
- [glossary.md](glossary.md) — quick one-line definitions of any term you hit here.
