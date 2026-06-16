# Four ways to read an ontology — SBVR, OSE, Manchester, Rosetta

This guide is for anyone deciding which output to generate, and for learners who want to use the side-by-side view to build intuition about what an OWL ontology actually says.

The verbalizer can render the *same* ontology in three different "languages," plus a fourth teaching view that lays them next to each other. Under the hood there are really only two kinds of reading:

- **Controlled English** — full sentences a non-expert can read: **SBVR Structured English** (the default) and **OWL Simplified English (OSE)**.
- **Formal syntax** — the terse, keyword-based notation engineers type in tools like Protégé: **Manchester Syntax**.

The fourth mode, **Rosetta**, is not a separate language. It is a side-by-side table that shows every statement in each selected language at once, all sharing one glossary, so you can learn to read the formal syntax by comparing it to the English.

Every example below is the *same idea* expressed four ways, drawn from the bundled SE-101 (Systems Engineering 101) example. See [examples.md](examples.md) for how to run it yourself.

## SBVR Structured English (the default)

SBVR Structured English is the verbose, beginner-friendly default. It reads each statement (axiom) as a full sentence using four color-coded roles — green underlined **term** (a concept/class), blue italic **verb** (a fact type/relationship), orange bold **keyword**, and teal double-underlined **name** (an individual). [reading-the-output.md](reading-the-output.md) explains the colors in detail; [concepts.md](concepts.md) defines the underlying ideas.

Two keywords are worth memorizing because they distinguish SBVR from OSE:

- `Each` introduces a "is a kind of" statement (a SubClassOf axiom).
- `at least one` marks an existential restriction (`owl:someValuesFrom`) — "there has to be one of these."

Real SE-101 sentences:

```
Each Subsystem is a System .
Each System performs function at least one Function .
Each System has component at least 2 Component .
Each Component has interface only Interface .
No Component is a Function or Requirement .
```

Notice the keyword vocabulary doing the work: `Each`, `at least one`, `at least 2` (cardinality), `only` (a universal restriction, `owl:allValuesFrom`), and `No ... is a ... or ...` (disjointness). All of these are orange and bold, and each carries a hover meaning in a browser.

**Use SBVR when** your reader is new to ontologies. It spells everything out.

## OWL Simplified English (OSE)

OSE is a *leaner* controlled-English reading. It is so close to SBVR that the tool walks the exact same axiom tree and only swaps two keywords:

| SBVR keyword | OSE keyword |
| --- | --- |
| `Each` | `Every` |
| `at least one` | `some` |

Everything else — the term/verb/name roles, the colors, the hover glosses, the sentence structure — is identical. So the SBVR sentence

```
Each System performs function at least one Function .
```

reads in OSE as

```
Every System performs function some Function .
```

**Use OSE when** you want slightly tighter prose and your reader is comfortable with "some" meaning "at least one."

## Manchester Syntax

Manchester Syntax is the formal OWL 2 notation that engineers type in Protégé and other ontology editors. It is **not prose** — it is terse, monospaced, and always uncolored (the output is HTML-escaped plain text in a monospace span). In the Rosetta view it acts as the *bridge column*, teaching a reader the real OWL syntax next to its plain-English reading.

A few things that surprise newcomers:

- **It uses IRI fragment names, not labels.** Manchester is rendered through the OWL API's `ManchesterOWLSyntaxOWLObjectRendererImpl` with a `SimpleShortFormProvider`, so names come from the IRI fragment (e.g. `Component`, `hasComponent`), deliberately **not** from `rdfs:label`. This is on purpose: labels can be in another language, and Manchester convention uses identifier-style names. (See the gotcha below.)
- **It is organized into frames with keywords.** Frame keywords you will see include `SubClassOf:`, `EquivalentTo:`, `DisjointWith:`, `SubPropertyOf:`, `InverseOf:`, `Domain:`, and `Range:`, plus the expression keywords `some`, `only`, `min`, `max`, `exactly`, `value`, `and`, `or`, `not`, and `self`.

The "every System performs at least one Function" idea, in Manchester, lives under the `System` class as:

```
SubClassOf: performsFunction some Function
```

The keyword `some` here means exactly what `at least one` means in SBVR — both are `owl:someValuesFrom`. That is not a coincidence you have to take on faith: the Manchester keywords are wrapped as glossable tokens, so in a browser, **hovering `some` in the Manchester column lights up the same `owl:someValuesFrom` meaning as hovering `at least one` in the SBVR column.** That shared gloss is the whole point of the Rosetta view.

**Use Manchester when** you are an engineer, you are round-tripping to an OWL tool, or you want to see the literal axiom structure.

### Gotcha: Manchester shows fragments, so foreign-language labels still appear as fragments

Because Manchester uses IRI fragments rather than `rdfs:label`, a class whose label is in another language still shows up as its (often English-ish) identifier. The classic case is the pizza tutorial ontology, whose labels are Portuguese: the SBVR and OSE columns show the Portuguese terms, but the Manchester column shows the IRI fragments. If a Manchester name looks "wrong" or untranslated, this is why — see [troubleshooting.md](troubleshooting.md).

## The Rosetta view

Rosetta is the teaching mode. It renders the **same statement in every selected format, side by side in a table**, with all columns sharing one glossary. Select it with `--rosetta`, which turns on SBVR, OSE, and Manchester together (SBVR | OSE | Manchester).

Two important behaviors:

- **It only appears when more than one format is selected.** Pick a single format and each entity's statements render as a plain bullet list. Pick more than one and they render as a side-by-side table — one column per format, each headed by the format's name.
- **It renders statically.** The table needs no JavaScript, so it works even in an embedded Swing viewer (where the hover panel and toolbar are inert). The hover glosses still need a browser, but the side-by-side comparison itself is always there.

This is where the cross-format learning happens: read the friendly SBVR sentence, glance one column over at the Manchester syntax, and the shared glossary confirms they mean the same thing.

## When to use which

| Mode | Best for | How it reads |
| --- | --- | --- |
| **SBVR** | Novices / "muggles" | Verbose, fully spelled-out English (`Each`, `at least one`) |
| **OSE** | Leaner English prose | Same as SBVR with `Every` and `some` |
| **Manchester** | Engineers, round-tripping to OWL tools | Formal OWL 2 syntax, IRI fragments, uncolored |
| **Rosetta** | Teaching and learning | All of the above, side by side, one shared glossary |

A note on what is *not* yet a feature: the built-in SBVR, OSE, and Manchester renderers are not a public, documented extension point — there is no public hook to inject a custom renderer of your own yet. That is a roadmap item, not current behavior; see [ROADMAP.md](../ROADMAP.md).

## Selecting a format from the command line

All three format flags are mutually exclusive — **if you pass more than one, the last one on the command line wins** (each flag replaces the format list). The default, with no format flag, is SBVR Structured English.

```bash
# Build the self-contained CLI jar once
./gradlew cliJar

# SBVR (the default) — no format flag needed
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --open

# OSE only
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --ose --open

# Manchester only
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --manchester --open

# Rosetta: SBVR | OSE | Manchester, side by side
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --rosetta --open
```

- `--manchester` selects Manchester only.
- `--ose` selects OSE only.
- `--rosetta` selects SBVR + OSE + Manchester side by side.

For the complete flag list — including the section toggles, `--color`, `--title`, and `--validate` — see [cli-reference.md](cli-reference.md). To decode the colors and hover panel once a report is open, see [reading-the-output.md](reading-the-output.md). To work with formats from your own Java or Groovy code (the `Format` enum is `SBVR`, `OSE`, `MANCHESTER`), see [library-guide.md](library-guide.md).
