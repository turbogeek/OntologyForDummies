# Checking an ontology for logical errors

This guide is for anyone who ran `--validate` (or wants to) and needs to make sense of the result — especially the phrase "unsatisfiable class." No prior reasoning experience assumed.

## The one-line version

Add `--validate` to your usual command and the tool runs a reasoner over your ontology after writing the report. A reasoner is a program that draws every logical conclusion your statements imply, then checks whether those conclusions hold together. It answers two questions:

- Does the ontology **contradict itself** as a whole? (consistency)
- Are there any **classes that can never have a member**? (unsatisfiable classes)

```bash
java -jar build/libs/ontology-to-english-*-cli.jar examples/se-101.ttl --validate
```

On the bundled Systems Engineering example, that prints:

```
VALIDATION: consistent; no unsatisfiable classes.
```

`--validate` is independent of every other flag — it works alongside `--open`, `--rosetta`, `--color`, and the rest. It does not change the HTML report; it just adds these lines to your terminal. For the full flag list, see [cli-reference.md](cli-reference.md).

## The three outcomes

Every `--validate` run ends in exactly one of three messages. They are quoted here verbatim so you can match what you see on screen.

### Outcome 1 — all clear

```
VALIDATION: consistent; no unsatisfiable classes.
```

The ontology holds together logically and every class can, in principle, have members. This is the result for `examples/se-101.ttl`. Nothing to fix.

### Outcome 2 — consistent, but some classes can never have members

```
VALIDATION: consistent, but N unsatisfiable class(es) — each can never have an instance:
  - <IRI of the first class>
  - <IRI of the second class>
```

The `N` is the count, and each offending class is listed on its own line, prefixed with two spaces, a hyphen, and a space (`  - `), as its full IRI.

This is the classic result on the well-known pizza tutorial ontology. Running `--validate` on it flags two classes:

```bash
java -jar build/libs/ontology-to-english-*-cli.jar src/test/resources/pizza.owl --validate
```

flags `CheeseyVegetableTopping` and `IceCream` as unsatisfiable.

An **unsatisfiable class** (one equivalent to `owl:Nothing`, the empty class) is a class that can never have a single instance — usually because its definition combines requirements that contradict each other. A topping that must be *both* a cheese *and* a vegetable, when cheeses and vegetables are declared to never overlap, is a class with no possible members. The rest of the ontology is still fine; only that one class is logically empty.

### Outcome 3 — the whole ontology contradicts itself

```
VALIDATION: INCONSISTENT — a reasoner found a contradiction in the ontology.
```

This is the serious one. *Inconsistent* means there is no possible interpretation of the ontology at all — the statements taken together describe a world that cannot exist. When an ontology is inconsistent, every class becomes technically unsatisfiable and the usual reasoning falls apart, so the tool reports the single global verdict and stops there.

## Inconsistent vs. unsatisfiable — the distinction that trips people up

These two words sound similar and mean different things:

- An **inconsistent ontology** contradicts itself *as a whole*. There is no valid interpretation of any of it. (Outcome 3.)
- An **unsatisfiable class** is *one* class that can never have an instance, while the rest of the ontology is perfectly fine. (Outcome 2.)

A useful mental picture: an unsatisfiable class is a single broken room; an inconsistent ontology is a collapsed building.

Because of this, the unsatisfiable list is only computed when the ontology is consistent. If the ontology is inconsistent, the reasoner stops at the global contradiction and the list comes back empty — there is nothing meaningful to enumerate once the whole thing is broken. (In the library, `Validator.Result.unsatisfiable` is populated only when `consistent` is `true`; see [Checking from your own code](#checking-from-your-own-code) below.)

For where these concepts fit in the bigger picture of classes, restrictions, and disjointness, see [concepts.md](concepts.md); for one-line definitions, see [glossary.md](glossary.md).

## How to think about fixing each

The tool tells you *that* something is wrong, not yet *why* — but the diagnosis usually follows from a couple of suspects.

**For an unsatisfiable class**, look at how that class is defined and constrained:

- **Conflicting restrictions.** A universal restriction reading `only` (`owl:allValuesFrom`) combined with an existential one reading `at least one` (`owl:someValuesFrom`) over incompatible types can leave no room for any member. In the report these read like `Each Component has interface only Interface .` and `Each System performs function at least one Function .`
- **Disjointness collisions.** If a class is declared to be two things that are stated never to overlap, it has no possible instances. Disjointness reads like `No Component is a Function or Requirement .` A class forced to be both a `Component` and a `Function` would be empty.

**For an inconsistent ontology**, the contradiction is global, so look for an individual (a named thing) asserted to belong to two disjoint classes, or a restriction that conflicts with a stated fact. Because the verdict is global, you may need to remove statements one at a time to find the culprit.

> **Planned:** an explanation/justification facility — pointing you at the exact offending axioms behind a contradiction or an unsatisfiable class, instead of leaving you to hunt — is on the roadmap, not in the tool today. See [../ROADMAP.md](../ROADMAP.md) (Pillar 3 — REASON & VALIDATE).

## Why validation might not run

The reasoner is loaded on demand, by name, at run time. That keeps the core library free of any hard reasoner dependency, but it means validation only works when a reasoner is actually on the classpath.

The **CLI jar bundles the ELK reasoner**, so `--validate` works out of the box when you run `build/libs/ontology-to-english-*-cli.jar`. If you instead run the slim library jar (which has no reasoner inside), `--validate` prints:

```
--validate needs a reasoner on the classpath. The CLI jar bundles ELK; if you run the slim library jar, add io.github.liveontologies:elk-owlapi.
```

The fix is either to use the CLI jar or to add `io.github.liveontologies:elk-owlapi` to your classpath. Any other failure during validation is reported as `(validation failed: <message>)` and does not stop the report from being written. These messages also appear in [troubleshooting.md](troubleshooting.md).

## Reasoner choice and profiles

There is no single "best" reasoner; they trade speed against logical coverage:

- **ELK** is fast and bundled in the CLI jar. It covers the OWL 2 **EL** profile — a deliberately limited slice of OWL chosen so that reasoning stays quick even on large ontologies. ELK is Apache-2.0 licensed. The CLI bundles **ELK 0.6.0**.
- **HermiT** covers the full OWL 2 **DL** language, so it can validate ontologies that use constructs outside ELK's EL profile, at the cost of speed. HermiT is LGPL licensed and is not bundled; a user supplies it.

The `Validator` is written against OWL API interfaces only, so you can hand it whichever reasoner you like. From the command line you get ELK automatically. From code you pass the factory yourself (next section). A planned, opt-in HermiT path for full OWL 2 DL validation is tracked in [../ROADMAP.md](../ROADMAP.md).

## Checking from your own code

The same validation is available to Java callers through the `Validator` class. Because the library has **no hard reasoner dependency**, you supply the reasoner factory — the library never bundles one (only the CLI jar does).

```java
import com.ontologyvision.verbalizer.Validator;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;

Validator.Result r = new Validator().validate(ont, new ElkReasonerFactory());

if (r.ok()) {
    System.out.println("consistent; no unsatisfiable classes");
} else if (!r.consistent) {
    System.out.println("INCONSISTENT — a contradiction was found");
} else {
    System.out.println(r.unsatisfiable.size() + " unsatisfiable class(es)");
}
```

`validate(OWLOntology, OWLReasonerFactory)` returns a `Validator.Result` with three things:

- `boolean consistent` — whether the ontology holds together as a whole.
- `List<OWLClass> unsatisfiable` — the classes equivalent to `owl:Nothing` (excluding `owl:Nothing` itself), sorted by IRI. Populated only when `consistent` is `true`.
- `boolean ok()` — a shortcut that is `true` only when the ontology is consistent *and* nothing is unsatisfiable.

Pass any OWL API reasoner factory — ELK or HermiT both work. For dependency coordinates, loading ontologies, and the full library surface, see [library-guide.md](library-guide.md).

## Where validation fits in the roadmap

What `--validate` does today — consistency plus unsatisfiable-class detection — is the first, shipped slice of the **REASON & VALIDATE** pillar. The following are **planned**, not current behavior:

- **SHACL** structural checks (required properties present, cardinalities actually met, datatypes) — the closed-world checks a reasoner deliberately will not flag.
- An optional **HermiT** full-DL reasoner path.
- **Naming and anti-pattern** checks (conventions and known ontology pitfalls).
- **Explanations and justifications** that point at the offending axioms behind a contradiction or an unsatisfiable class.

See [../ROADMAP.md](../ROADMAP.md) for the full plan.

## See also

- [concepts.md](concepts.md) — what classes, restrictions, disjointness, consistency, and unsatisfiable classes mean.
- [cli-reference.md](cli-reference.md) — every flag, including how `--validate` combines with the rest.
- [library-guide.md](library-guide.md) — calling `Validator` from your own code and supplying a reasoner.
- [examples.md](examples.md) — the SE-101 and IOF fixtures you can validate.
- [troubleshooting.md](troubleshooting.md) — the validation error messages and their fixes.
- [glossary.md](glossary.md) — quick definitions of reasoner, ELK, HermiT, consistency, and unsatisfiable class.
