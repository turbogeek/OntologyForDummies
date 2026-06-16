# Glossary of ontology and tool terms

This page is for any reader who hits an unfamiliar word in the docs or in a generated report and just wants a one-line, plain-English definition with a pointer to more.

It is a flat, alphabetical lookup. For the same terms explained in depth, with a running example, see [concepts.md](concepts.md). For how the report shows these words on screen, see [reading-the-output.md](reading-the-output.md).

A handy fact: the report you generate already carries its own built-in lookups. The **OWL Constructs** glossary explains **37** OWL terms and the **RDF / RDFS Constructs** glossary explains **22** RDF/RDFS terms, both as static reference tables baked into the jar. So if a big word appears in a report, you can usually hover it or scroll down to its glossary entry without leaving the page.

Throughout this glossary we use the same color vocabulary as the rest of the docs: **term** = green (a concept), **verb** = blue (a fact type), **keyword** = orange, **name** = teal (an individual), **literal** = plain. Each color is always paired with its role word so grayscale and color-blind readers are served too.

## Core ontology terms

**Attribute (data property).** A property whose value is a plain data value — a number, a date, a piece of text — rather than a link to another thing. Attributes appear in the report's `Attributes (Data Properties)` section. Contrast with an object property (a fact type), which links two things.

**Axiom.** One statement the ontology asserts — a single fact or rule. Every sentence in the report, such as `Each Subsystem is a System .`, comes from an axiom.

**Cardinality.** A rule about *how many* of something must be present. The report reads it as `at least N`, `at most N`, or `exactly N`. For example, `Each System has component at least 2 Component .` is a cardinality restriction. (In OWL these map to `owl:minCardinality`, `owl:maxCardinality`, and `owl:cardinality`.)

**Class (concept).** A *kind* of thing — System, Component, Interface. Classes appear in the report's `Vocabulary — Concepts (Classes)` section and are styled as a **term** (green, underlined). "Concept" and "class" mean the same thing here; this tool prefers the friendlier word "concept."

**Consistency.** Whether the ontology, taken as a whole, can be true at all. A *consistent* ontology has at least one valid interpretation; an *inconsistent* one contradicts itself so badly that nothing can satisfy it. See [validation.md](validation.md). Note that consistency is about the *whole* ontology, while an unsatisfiable class is a *local* problem — see below.

**Data property.** See **Attribute** above.

**Disjointness.** A rule that two concepts can never overlap — nothing can be both at once. It reads like `No Component is a Function or Requirement .` (In OWL this is `owl:disjointWith`.)

**Fact type (object property).** See **Object property** below — "fact type" is the SBVR name for the same idea, and it is the **verb** (blue, italic) role you see in the report.

**Individual (instance).** A specific, named thing — one particular System, not the kind "System." Individuals appear in the report's `Individuals (Instances)` section and are styled as a **name** (teal, double-underlined).

**Object property (fact type / relationship).** A property that links one thing to another — *has component*, *performs function*, *has interface*. Object properties appear in the report's `Fact Types — Relationships (Object Properties)` section and read as **verbs**: `Each Component has interface only Interface .` Contrast with an attribute (data property), whose value is a plain data value.

**Ontology.** A precise, shareable vocabulary plus the rules that connect its words — think of it as a dictionary a computer can actually reason about. This whole tool exists to turn an ontology back into readable English.

**Restriction.** A rule that limits how a property may be used on a concept. Three flavors show up most:
- *existential* — "at least one": `Each System performs function at least one Function .` (OWL: `owl:someValuesFrom`).
- *universal* — "only": `Each Component has interface only Interface .` (OWL: `owl:allValuesFrom`).
- *cardinality* — "at least N / exactly N": `Each System has component at least 2 Component .`

**Unsatisfiable class.** A single concept that can never have any instance — it is equivalent to `owl:Nothing` — usually because of contradictory restrictions or disjointness, even though the rest of the ontology is perfectly fine. This is *not* the same as an inconsistent ontology: an unsatisfiable class is one broken concept, whereas inconsistency means the whole ontology contradicts itself. When an ontology is inconsistent the unsatisfiable list is left empty, because the reasoner stops at the global contradiction. See [validation.md](validation.md).

## SBVR reading roles

SBVR Structured English, the default output, marks every word with one of four roles. The report prints this legend so you can match colors to roles:

```
SBVR Structured English — term (concept), verb (fact type), keyword, name (individual)
```

**term.** A **concept / class**. Green (`#157f3b`), underlined. Example: *System*, *Component*.

**verb.** A **fact type / relationship** (an object property). Blue (`#1b6ca8`), italic. Example: *has component*, *performs function*.

**keyword.** An SBVR **keyword** — the structural glue words. Orange (`#c8741a`), bold. Examples: `Each`, `No`, `only`, `at least one`, `at least N`.

**name.** An **individual**. Teal (`#0a7d7d`), double-underlined. Example: a specific named System.

**literal.** A plain **data value** (a number, date, or string). Near-black (`#1a1a1a`), plain. Literals are not named in the legend because they are just plain text.

### Keyword phrases

These are the orange-bold keywords you will read most, and how OSE rephrases two of them:

| SBVR phrase | OSE phrase | Means |
|---|---|---|
| `Each` | `Every` | subclassing — every member of A is also a B (`rdfs:subClassOf`) |
| `No` | `No` | disjointness — A and B never overlap (`owl:disjointWith`) |
| `only` | `only` | universal restriction (`owl:allValuesFrom`) |
| `at least one` | `some` | existential restriction (`owl:someValuesFrom`) |
| `at least N` | `at least N` | minimum cardinality (`owl:minCardinality`) |

OSE (OWL Simplified English) walks the exact same axioms as SBVR and differs only in these keyword swaps — `Each` becomes `Every`, `at least one` becomes `some`. See [formats.md](formats.md).

Behind the scenes the tool keeps a `KEYWORD_TO_CONSTRUCT` mapping so that hovering a keyword can show the OWL/RDF construct it stands for — for example `each` and `every` both map to `rdfs:subClassOf`, `at least one` and `some` both map to `owl:someValuesFrom`, and `only` maps to `owl:allValuesFrom`. That is why hovering `some` in Manchester and `at least one` in SBVR shows the *same* meaning.

## Selected OWL constructs

These are the formal OWL terms you are most likely to meet, glossed in plain words. The report's built-in **OWL Constructs** glossary covers 37 of them; these are the headline few.

**rdfs:subClassOf.** Asserts every instance of the subclass is necessarily also an instance of the superclass — a one-directional "is-a-kind-of" inclusion; it does not imply the reverse. Reads as `Each Subsystem is a System .`

**owl:equivalentClass.** Asserts two class expressions have exactly the same instances in every interpretation — mutual subclassing, where membership in one is necessary and sufficient for the other. Used for definitions ("A Vegetarian Pizza is by definition a Pizza that has only vegetarian toppings.").

**owl:someValuesFrom.** The class of individuals having *at least one* value of a property in a specified class — it requires that *some* related thing of the right kind exists (not that all of them are). This is the existential restriction, read as `at least one` in SBVR / `some` in OSE.

**owl:allValuesFrom.** The class of individuals *all of whose* values for a property fall in a specified class — it constrains every related value but does not require that any exist. This is the universal restriction, read as `only`.

**rdfs:domain.** States that any individual that is the subject of the property must belong to the named class — it constrains what can have the relationship and lets a reasoner infer the subject's type.

**rdfs:range.** States that any value that is the object of the property must belong to the named class or datatype — it constrains what the relationship can point to and lets a reasoner infer the value's type.

## Selected RDF / RDFS constructs

These RDF/RDFS terms are the foundations OWL is built on. The report's built-in **RDF / RDFS Constructs** glossary covers 22 of them.

**rdf:type.** The property stating an individual is an instance of a class; `x rdf:type C` asserts that x belongs to C. It is abbreviated `a` in Turtle, so `Alice a Person` reads "Alice is a Person."

**rdfs:label.** A plain, human-readable name for a resource — no logical or inferential weight, purely presentational; often language-tagged. The verbalizer prefers labels for its readable sentences (which is why a class labeled `Margherita` reads as "Margherita").

**rdfs:comment.** A human-readable description, note, or definition of a resource — non-inferential documentation, frequently language-tagged. When present, a class's comment becomes its glossary entry text and its hover gloss.

## Tool-specific terms

**ELK.** The reasoner bundled in the CLI jar. It is fast and covers the OWL 2 EL profile, and is licensed Apache-2.0. `--validate` uses it out of the box. See [validation.md](validation.md).

**HermiT.** A heavier reasoner that covers full OWL 2 DL, licensed LGPL. It is *not* bundled, but you can supply it yourself through an `OWLReasonerFactory` when calling the library. See [library-guide.md](library-guide.md).

**Model Glossary.** A report section listing *this* ontology's own vocabulary — the concepts, fact types, attributes, and individuals it defines. Its term count varies per ontology because it is derived from your file (unlike the OWL and RDF glossaries, which are fixed).

**OWL Constructs glossary.** A static, built-in reference table of **37** OWL terms (such as `owl:equivalentClass` and `rdfs:domain`) explained in plain English. It is the same in every report — it does not depend on your ontology.

**RDF / RDFS Constructs glossary.** A static, built-in reference table of **22** RDF/RDFS terms (such as `rdf:type` and `rdfs:label`) explained in plain English. Also fixed across all reports.

**Reasoner.** A program that checks an ontology's logic — confirming it is consistent and finding any unsatisfiable classes. Reasoners are pluggable: ELK is fast for OWL 2 EL, HermiT covers full OWL 2 DL. See [validation.md](validation.md).

**Rollover / hover panel.** With rollover on (the default), hovering a colored term, verb, keyword, or name *in a browser* shows its meaning in a fixed panel at the bottom of the page. The hover shows the entity's `rdfs:comment` if it has one, otherwise a gloss generated from its axioms. The panel needs JavaScript, so in a static or embedded (Swing) view the page is still fully readable but the panel sits inert. See [reading-the-output.md](reading-the-output.md).

**Rosetta view.** A side-by-side table that shows the *same* axiom in SBVR, OSE, and Manchester at once, all columns sharing one glossary — a teaching view for learning to read across formats. It appears only when more than one format is selected; a single format renders as a bullet list instead. It renders statically, so it works without JavaScript. See [formats.md](formats.md).

## Going deeper

For full, worked definitions with a running example, start at [concepts.md](concepts.md). To decode a report on screen, see [reading-the-output.md](reading-the-output.md). For the planned reasoning and validation work, see [../ROADMAP.md](../ROADMAP.md).

If you want the underlying specifications:
- W3C **OWL 2 Primer** — the gentle official introduction to OWL.
- An **SBVR** primer — for the controlled-English vocabulary the SBVR and OSE outputs are based on.
