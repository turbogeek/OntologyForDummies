// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.List;

/**
 * A pluggable verbalization <b>format</b> (SBVR Structured English today; Manchester Syntax, OWL Simplified
 * English and ACE next). The engine ({@link OntologyVerbalizer}) owns everything format-agnostic — entity
 * iteration, the model + reference glossaries, the HTML/CSS shell, colour modes, and the hover-gloss
 * machinery — and asks an {@code AxiomRenderer} only to turn each entity's axioms into that format's
 * sentences (or lines). Adding a format is a new implementation, not an engine edit.
 *
 * <p>Each {@code *Sentences} method returns the rendered statements for one entity (already role-tagged HTML
 * via the supplied {@link RenderContext}); an empty list means "nothing to say" and the engine skips the
 * entity. Headings and entity-kind labels are format-specific wording the engine drops into the shared shell.
 */
public interface AxiomRenderer
{
    /** Stable id, e.g. {@code "sbvr"}. */
    String formatId ();

    /** Human name, e.g. {@code "SBVR Structured English"}. */
    String displayName ();

    // ---- section headings (format-specific wording) ----
    String classesHeading ();
    String objectPropertiesHeading ();
    String dataPropertiesHeading ();
    String individualsHeading ();

    // ---- entity-kind labels shown next to each entity heading ----
    String classKind ();
    String objectPropertyKind ();
    String dataPropertyKind ();
    String individualKind ();

    // ---- per-entity statements (empty list = nothing to say) ----
    List<String> classSentences (OWLClass c, RenderContext ctx);
    List<String> objectPropertySentences (OWLObjectProperty p, RenderContext ctx);
    List<String> dataPropertySentences (OWLDataProperty p, RenderContext ctx);
    List<String> individualSentences (OWLNamedIndividual i, RenderContext ctx);
}
