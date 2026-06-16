# Ontologies in plain English — a muggle's primer

This guide is for you if you have never met an ontology and need a handful of words before anything else makes sense. No prior OWL, RDF, or SBVR knowledge is assumed — just everyday English and the running [Systems Engineering 101 example](examples.md) that ships with the tool.

By the end you will be able to read a sentence like `Each System performs function at least one Function .` and know exactly what it claims.

## What is an ontology?

An **ontology** is a precise, shareable vocabulary — plus the rules that connect its words — written so a computer can reason about it. Think of it as a dictionary that doesn't just define words but also knows how they fit together, and can catch you when two of your statements contradict each other.

OntologyForDummies takes one of these computer-readable ontologies (an OWL file) and writes it back out as an HTML page that reads like English. The point is to let non-experts ("muggles") read — and eventually create — ontologies, and to let expert ontologists communicate with everyone else.

The rest of this page walks the small set of ideas you meet in that English page, in roughly the order the report presents them. Each idea is paired with a real sentence from the SE-101 report so you can see what it looks like on screen. Every term here also has a one-line entry in the [glossary](glossary.md).

## Concept (class)

A **concept** (its technical name is a *class*) is a *kind* of thing: `System`, `Component`, `Interface`. It names a category, not any particular member of it. SE-101 defines concepts like `System`, `Subsystem`, `Component`, `Function`, and `Requirement`.

In the report, concepts are listed under the section heading **Vocabulary — Concepts (Classes)**, and wherever a concept appears in a sentence it is styled as a **term — green, underlined**.

The simplest statement you can make about a concept is that it is a *kind of* another concept (a subclass). SE-101 says a subsystem is just a special system:

> `Each Subsystem is a System .`

Here `Subsystem` and `System` are both **terms (green)**, and `Each` and `is a` are **keywords (orange)** — fixed connector words that the tool always renders the same way. More on keywords below.

## Individual (instance)

An **individual** (also called an *instance*) is one specific, named thing — not a kind, but an actual member of a kind. If `System` is a concept, then a particular aircraft is an individual of that concept. SE-101 includes a few: `Aircraft`, `Wing`, `Engine`, `Fly`, and the requirement `REQ-001`.

In the report, individuals are listed under **Individuals (Instances)**, and in sentences an individual is styled as a **name — teal, double-underlined** (the double underline is easy to mistake for a plain one, so the color and role word are your reliable cue).

A handy way to remember the difference: `Component` is a concept (a kind of part), while the `Wing` is an individual (one actual part). Data values attached to an individual — like a mass of 250.0 — appear as **literals (plain near-black text)**, which are not styled as a role at all.

## Object property vs. data property

A **property** is a relationship. Ontologies use two flavors, and the report keeps them in separate sections.

An **object property** (also called a *relationship* or *fact type*) links one thing to another thing: a system *has component* a component, a component *has interface* an interface. SE-101 defines `hasComponent`, `hasInterface`, `performsFunction`, `connectsTo`, and more. These are listed under **Fact Types — Relationships (Object Properties)**, and in sentences a property is styled as a **verb — blue, italic**:

> `Each System has component at least 2 Component .`

A **data property** (also called an *attribute*) links a thing to a plain value — a number, a string, a date — rather than to another thing. SE-101 gives a component a `mass in kg` and a requirement a `priority`. Attributes are listed under **Attributes (Data Properties)**. The rule of thumb: if the far end is *another concept*, it is an object property (fact type); if the far end is *a literal value*, it is a data property (attribute).

So the report has four sections, one per kind of entity:

- **Vocabulary — Concepts (Classes)** — the kinds of things.
- **Fact Types — Relationships (Object Properties)** — links between things.
- **Attributes (Data Properties)** — links from things to plain values.
- **Individuals (Instances)** — the specific named things.

(The number after each heading is just a per-section count and changes from ontology to ontology.)

## Axiom

An **axiom** is one statement the ontology asserts — a single fact or rule. Every sentence in the report's verbalization is one axiom rendered into English. `Each Subsystem is a System .` is an axiom. So is `Each System has component at least 2 Component .` You don't have to write the word "axiom" anywhere; just know that "an axiom" and "a sentence in the report" are, for our purposes, the same thing.

## Restrictions: at least one, only, and counting

Most of the interesting rules in an ontology are **restrictions** — conditions a thing must satisfy to count as a member of a concept. Three kinds show up constantly, and all three appear in SE-101.

**Existential ("at least one").** An existential restriction (technical name `owl:someValuesFrom`) says a thing must have *at least one* link of a certain kind. Every system has to perform at least one function:

> `Each System performs function at least one Function .`

The keyword `at least one` (orange, bold) is the tell. In [OWL Simplified English](formats.md) the same restriction reads `some` instead of `at least one`.

**Universal ("only").** A universal restriction (technical name `owl:allValuesFrom`) says that *if* a thing has links of a certain kind, *all* of them must point at a given concept. A component connects out only through interfaces:

> `Each Component has interface only Interface .`

Note the difference from existential: "only" does not say a component *has* an interface — it says that any interface links it does have must point at `Interface`. The keyword to watch for is `only`.

**Cardinality ("at least 2", "exactly N").** A cardinality restriction puts a number on the count. SE-101 requires every system to have at least two components:

> `Each System has component at least 2 Component .`

The keyword `at least 2` carries the count. Existential is really just "at least 1"; cardinality lets the ontology say at least N, exactly N, or at most N.

## Disjointness

**Disjointness** says two concepts can never overlap — nothing can be a member of both at once. SE-101 declares that parts, functions, and requirements are kinds apart: a component is never a function, and never a requirement. The report states it as a single "No …" sentence:

> `No Component is a Function or Requirement .`

The keyword `No` (orange, bold) signals a disjointness statement. This is one of the most common ways an ontology rules things out, and — as you'll see in [validation.md](validation.md) — over-eager disjointness is a frequent cause of classes that can never have members.

## Special property traits: inverse, symmetric, transitive

Beyond linking two things, a property can carry **traits** that tell the reasoner how the link behaves. SE-101 uses three you'll meet often.

**Inverse** — two properties that are the same link read backwards. SE-101 pairs `realizes` with `realizedBy`: if a component realizes a function, that function is realized by the component. (One caveat for later: when an inverse link appears unnamed inside another rule, the report shows the placeholder text `(inverse property)` rather than a styled role — see [reading-the-output.md](reading-the-output.md).)

**Symmetric** — a link that automatically holds both ways. SE-101 marks `connectsTo` symmetric: if interface A connects to B, then B connects to A.

**Transitive** — a link that chains: a part of a part is itself a part. SE-101 marks `hasPart` transitive, and the report says so directly:

> `The relationship has part is transitive .`

Transitivity is also why you'll see a property's domain spelled out as an "Anything that …" sentence. Because anything that has a part is a system, the report writes:

> `Anything that has part something is a System .`

Here `has part` is the **verb (blue, italic)** and `System` is the **term (green)**.

## Consistency vs. an unsatisfiable class

These two phrases sound alike and are constantly confused, but they mean different things — and you'll see both when you run [`--validate`](validation.md).

**Consistency** is about the *whole ontology*. An ontology is **inconsistent** when its statements contradict each other so badly that there is no possible valid interpretation — the whole thing collapses. There's nothing salvageable to talk about until you fix it.

An **unsatisfiable class** is about *one concept*. It is a concept that can never have any member — formally, a class equivalent to `owl:Nothing` (the empty class) — usually because contradictory restrictions or disjointness rules box it in. The crucial part: the rest of the ontology can be perfectly fine. One bad concept can be unsatisfiable while everything around it is healthy.

A short way to keep them straight:

- **Inconsistent** = the *whole ontology* contradicts itself.
- **Unsatisfiable class** = *one concept* can never have members, but the rest is fine.

(When an ontology is inconsistent, the reasoner stops at the global contradiction, so it does not even compute a list of unsatisfiable classes — that list comes back empty. SE-101 itself is clean: `--validate` reports `VALIDATION: consistent; no unsatisfiable classes.` The full walkthrough of every outcome lives in [validation.md](validation.md).)

You may also wonder about two special concepts the OWL world always has: `owl:Thing` (the top concept that everything belongs to) and `owl:Nothing` (the empty concept nothing belongs to). The verbalizer skips these in rendering, so you never read a pointless tautology like "Each owl:Thing is a …" — the report stays focused on *your* vocabulary.

## Where to go next

- **See it on a real page** — [reading-the-output.md](reading-the-output.md) decodes the colors, the four sections, the hover panel, and the built-in glossaries.
- **Run the example yourself** — the 60-second quick start in the [README](../README.md) builds the tool and verbalizes SE-101 in one go; [examples.md](examples.md) goes deeper on SE-101 and the real-world IOF ontologies.
- **Pick a reading style** — [formats.md](formats.md) shows the same sentence in SBVR, OWL Simplified English, Manchester syntax, and the side-by-side Rosetta view.
- **Look a word up** — [glossary.md](glossary.md) has a one-line definition for every term on this page.
- **Where the project is headed** — the READ / CREATE / REASON & VALIDATE roadmap lives in [ROADMAP.md](../ROADMAP.md).
