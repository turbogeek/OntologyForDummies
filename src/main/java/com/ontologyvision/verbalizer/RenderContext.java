// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.List;

/**
 * The shared primitives an {@link AxiomRenderer} uses to build role-tagged HTML — supplied by the engine
 * ({@link OntologyVerbalizer}) so a format renderer never has to reach into engine internals. It exposes the
 * ontology, the four styled token roles (which carry colour + hover-gloss), name/escaping helpers, and a
 * couple of generic text helpers. Every format (SBVR, and later Manchester / ACE) emits the same span markup
 * through these, so colour suppression and the hover-gloss panel work identically across formats.
 */
public interface RenderContext
{
    /** The ontology being verbalized (for a renderer to walk an entity's axioms). */
    OWLOntology ontology ();

    // ---- styled, glossed, colour-tagged tokens (the shared styling vocabulary) ----
    /** A concept token (green-underlined "term"), with its hover gloss. */
    String term (OWLEntity e);
    /** A concept token from raw text (no gloss). */
    String term (String s);
    /** A fact-type token (blue-italic "verb"), with its hover gloss. */
    String verb (OWLEntity e);
    /** A fact-type token from raw text (no gloss). */
    String verbRaw (String s);
    /** An individual token (teal double-underlined "name"), with its hover gloss. */
    String name (OWLEntity e);
    /** A keyword token (orange-bold), with the gloss of the OWL/RDF construct it maps to (if any). */
    String kw (String s);
    /** A literal token (plain). */
    String lit (OWLLiteral l);

    // ---- names / escaping ----
    /** Best display name for an entity (English {@code rdfs:label}, else neutral label, else humanized IRI). */
    String label (OWLEntity e);
    /** HTML-escape text. */
    String esc (String s);
    /** The local name / fragment of an IRI. */
    String shortForm (IRI iri);

    // ---- generic text helpers ----
    /** Join parts with a separator. */
    String join (List<String> parts, String sep);
    /** "a" or "an" for the given (plain) word. */
    String aAn (String word);
}
