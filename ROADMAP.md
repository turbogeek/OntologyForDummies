# OntologyForDummies — Roadmap

The mission (from the README) is to let novices **read *and* create** ontologies — and to give expert
ontologists a way to communicate with the muggles. That splits into two pillars, plus a shared reasoning &
validation layer:

- **Pillar 1 — READ**: turn an existing OWL ontology into readable forms (verbalization). *This is what we
  have today.*
- **Pillar 2 — CREATE**: go the other way — turn controlled English (and, eventually, source documents) into
  OWL, so a muggle can *author* an ontology.
- **Pillar 3 — REASON & VALIDATE**: use a reasoner and quality checks to find contradictions, unsatisfiable
  classes, anti-patterns, and naming/pattern violations. Shared by both pillars (and by VOM).

> Direction matters. The controlled languages split by which way they run:
> *Verbalize* (OWL → text) is the READ pillar. *Author* (text → OWL) is the CREATE pillar. ACE, OWL Simplified
> English, and TEDEI were all designed primarily for authoring; SBVR and Manchester are reading forms.

---

## Where we are — v0.1.0

**Tool #1: "Ontology to English"** — a pure-OWL-API + JDK, MIT library/CLI that reads an OWL ontology and writes
a colored, hyperlinked HTML report in **SBVR Structured English**, with a model glossary plus plain-English
glossaries of the OWL and RDF/RDFS constructs used, and hover-the-word meanings. Ships as a library, a
self-contained CLI jar (`SbvrMain`), and a Groovy script. Consumed by the VOM MagicDraw plugin as a published
artifact (`com.ontologyvision:ontology-to-english`).

---

## Pillar 1 — READ (verbalize OWL → readable forms)

Evolve Tool #1 from "SBVR only" into a multi-format reader. Key design finding from the format exploration:
there are only **two kinds of output** — *formal syntax* (Manchester) and *controlled English* (SBVR / OSE /
ACE-style), where the controlled-English variants are **one traversal with different phrasing profiles**, not
separate engines.

| Format | What it is | Status / cost |
|---|---|---|
| **SBVR Structured English** | Business-rules reading (term/verb/keyword/name roles) | ✅ done |
| **Manchester Syntax** | Formal OWL 2 syntax (`hasTopping some MozzarellaTopping`) | OWL-API renderer — **~½ day** |
| **OWL Simplified English** | Lean controlled English (Power, 2012) | phrasing profile of the controlled-English renderer — small |
| **ACE-style** | Most English-like (full sentences) | hand-built; adds article/number agreement; *not* round-trippable |

### 1.1 — `AxiomRenderer` refactor (foundation)
Extract a format-agnostic `OntologyVerbalizer` engine (axiom-walking + the shared HTML/CSS/glossary/rollover
shell) and put per-format sentence assembly behind an `AxiomRenderer` interface. Implementations:
`ManchesterRenderer` and a single `ControlledEnglishRenderer` parameterized by a phrasing profile
(`SBVR | OSE | ACE`). Move today's SBVR logic *verbatim* into the SBVR profile, gated by a golden-file test on
`pizza.owl` so the existing report stays byte-identical. Scoped rename while we're at v0.1.0 (cheap): engine
→ `OntologyVerbalizer`, `SbvrOptions` → `VerbalizerOptions`, package `…sbvr` → `…verbalizer`; keep the SBVR
renderer and its `.term/.verb` roles named `Sbvr*`.

### 1.2 — Manchester Syntax
`ManchesterOWLSyntaxOWLObjectRendererImpl` + `SimpleShortFormProvider` (IRI fragments — *not* a label provider,
or pizza's Portuguese `rdfs:label`s leak in). Renders per-axiom, drops straight into the existing report.

### 1.3 — The "Rosetta" side-by-side view (signature feature)
The same axiom in every column — **Plain English (SBVR/ACE) | Formal syntax (Manchester)** — sharing one
glossary, so hovering `some` (Manchester) and `at least one` (SBVR) both light the same `owl:someValuesFrom`
entry. *The column adjacency is the teaching.* It also renders statically (no JS), so it works in VOM's in-app
Swing viewer where a JS format-toggle would not.

### 1.4 — Controlled-English phrasing profiles
OSE and ACE-style become phrasing profiles on the controlled-English renderer (keyword set, articles/number
agreement, whether SBVR font-roles + modality are on). OSE is nearly free once parameterized; ACE-style adds
English grammar and ships labelled "ACE-*style*, not round-trippable."

### 1.5 — (Optional) genuine round-trippable ACE
Only if users need APE-valid ACE: an **arms-length** external adapter (OWL→OWL-2-XML → external SWI-Prolog
`owl-verbalizer`, or a user-supplied HTTP endpoint). Kept out of the MIT core (the Attempto stack is LGPL and
dormant since 2013), opt-in behind a flag.

---

## Pillar 2 — CREATE (controlled English / documents → OWL)

The "create" half of the mission. Reuses the OWL-API core but **not** the rendering engine — this is parsing,
not rendering.

### 2.1 — OWL → TEDEI verbalizer  *(priority)*
A controlled-English reading aligned to **TEDEI**'s grammar (Mathews & Kumar, 2017 — a text→OWL CNL that
reportedly covers far more of the pizza ontology than ACE). Doing the *read* direction first means the language
a user reads is the same language they'll later write — a deliberate stepping stone to round-trip. Implemented
as another phrasing profile / renderer in Pillar 1's framework.

### 2.2 — TEDEI → OWL authoring  *(the other direction)*
A controlled-English **parser** (ANTLR grammar, as in the TEDEI paper) that turns edited TEDEI sentences into
OWL axioms — "write your ontology in plain controlled English." A predictive/assisted editor (à la the OWL
Simplified English editor) is the eventual UX. This is a distinct, larger track; the OWL→TEDEI verbalizer
(2.1) defines the target language and de-risks it.

### 2.3 — Document → ontology scanner
Augment or draft an ontology from source documents (e.g. scan the **SEBoK / INCOSE Systems Engineering
Handbook** to seed a systems-engineering ontology). Realistic, human-in-the-loop pipeline:

```
source text ──(LLM/NLP extraction)──▶ candidate controlled-English (TEDEI) sentences
                                          │  ← human curates/edits (auditable, reviewable middle layer)
                                          ▼
                              TEDEI → OWL (deterministic parse, §2.2)
                                          │
                                          ▼
                          reasoner: consistency / unsatisfiable / unintended entailments  (Pillar 3)
                                          │  ← issues fed back to the curator
                                          ▼
                                   reviewed ontology fragment
```

The controlled-English middle layer is the key idea: it keeps a **human-auditable, deterministically-parseable
bridge** between messy text and formal OWL, so the LLM *proposes* but never silently commits — and the reasoner
catches contradictions before they land. This is a **drafting assistant, not a "scan → ontology" button**: LLM
extraction is roughly novice-modeler quality and hallucinates, so the reviewable middle layer is what makes it
safe to use at all.

*SEBoK licensing:* its text is **CC BY-NC-SA 3.0** (NonCommercial + ShareAlike; some third-party figures/tables
are the stricter CC BY-NC-ND) — respect those obligations. And **build on existing SE-ontology work** (the
INCOSE MBSE Ontology, LML, "Ontology for Systems Engineering") rather than starting from scratch — see *Example
ontologies* below.

---

## Pillar 3 — REASON & VALIDATE

Find contradictions and quality problems by **multiple methods** — these are *orthogonal* layers (each catches
a class of error the others structurally cannot), all kept **optional and pluggable** so the MIT core stays
dependency-light:

1. **Reasoner — logical soundness** (open-world). Consistency, unsatisfiable classes, and
   contradiction/entailment detection — the "find contradictions" goal. Behind the OWL API
   `OWLReasonerFactory` so it's swappable; **default ELK** (Apache-2.0, pure-Java, OWL 2 EL, fast) with
   **HermiT** (LGPL, full OWL 2 DL) as an opt-in extra. **Avoid Openllet** (AGPL-3.0 — incompatible with MIT).
   Pair it with an explanation/justification facility so a contradiction points at the *offending axioms*, not
   just "inconsistent."
2. **SHACL — data/structural conformance** (closed-world). Required properties present, cardinalities actually
   met, datatypes, value ranges — things the reasoner deliberately will *not* flag (absence ≠ invalid under
   OWA). Optional module via **TopQuadrant SHACL** or **Apache Jena SHACL** (both Apache-2.0). Bonus feature:
   **generate SHACL shapes from the OWL** (cardinality/domain/range/datatype axioms map mechanically to
   `sh:minCount`/`maxCount`/`class`/`datatype`) — a closed-world check almost "for free."
3. **QA patterns / anti-patterns** — naming conventions (UpperCamelCase classes, lowerCamel `has…`/`is…`
   properties), design patterns (positive templates) and anti-patterns / pitfalls (**OOPS!** 41-pitfall
   catalogue, Apache-2.0, ~22 structurally auto-detectable; **OntoClean**; **Roussey** logical/structural
   anti-patterns), runnable as OWL-API visitors, SPARQL graph-patterns, or SHACL.

Output is **one unified validation report** merging all layers, each finding tagged with its source + severity.

> **Why there's no OCL layer here:** OCL validates a *UML model*, and OntologyForDummies has no UML model — it
> works on OWL directly. OCL is **VOM's** layer: VOM edits a UML-profile model *from which the OWL is built*
> (the OWL is the derived artifact, not the source), so OCL guards the authoring model in-tool before export.
> See the VOM `PHASE18-VALIDATION-QA-DESIGN` doc for that side.

---

## Example ontologies (content & learning targets)

Bundled / referenced ontologies serve two purposes — **demo fixtures** to verbalize "for muggles," and a
**learning target** (systems engineering):

- **Pizza** — the classic teaching ontology (already the test fixture).
- **Industrial Ontology Foundry (IOF)** — github.com/iofoundry — systems-engineering basics; a strong candidate
  to add as example content (verify the license before bundling or declaring a dependency).
- **Barry Smith / BFO-based systems-engineering ontologies** — the basis for an SE ontology, which is itself a
  goal here (and a vehicle for learning VOM). Mine the video series + papers; the resulting SE ontology lives in
  this project.

These are demo/learning content, not engine code.

## Suggested sequencing

1. **Pillar 1 foundation**: `AxiomRenderer` refactor (SBVR byte-identical) + scoped rename.
2. **Manchester + Rosetta** — highest-impact, lowest-risk, most on-mission.
3. **OSE + TEDEI verbalizer (2.1)** — cheap once the controlled-English renderer is parameterized; 2.1 is the
   priority that sets up round-trip.
4. **Reasoner integration (Pillar 3)** — needed by the scanner and by validation.
5. **ACE-style**, then **TEDEI → OWL authoring (2.2)**, then the **document scanner (2.3)**.

---

## Ethos / constraints

MIT, dependency-light, "runs anywhere with a JRE." Heavy or non-MIT pieces (genuine ACE via Prolog; large
reasoners; LLM calls in the scanner) stay **optional and arms-length** so the core library never inherits their
weight or licenses. VOM consumes the published artifact and keeps its MagicDraw-specific reasoning/query/OCL
work in its own repo (see the VOM roadmap).
