// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * The <b>OWL 2 Manchester Syntax</b> format — the terse, keyword-based formal syntax ontology engineers type
 * in Protégé (<code>hasTopping some MozzarellaTopping</code>). Unlike the controlled-English formats this is
 * not prose; in the side-by-side "Rosetta" view it is the <i>bridge column</i> that teaches a reader the real
 * OWL syntax next to its plain-English reading.
 *
 * <p>Rendering is the OWL API's own {@link ManchesterOWLSyntaxOWLObjectRendererImpl} with a
 * {@link SimpleShortFormProvider} (IRI fragments, e.g. {@code American}) — deliberately NOT a label provider,
 * because labels can be non-English (the pizza fixture's are Portuguese) and Manchester convention uses
 * identifier-style names. Output is plain text, so it is HTML-escaped and wrapped in a monospace span.
 */
final class ManchesterRenderer implements AxiomRenderer
{
    private final ManchesterOWLSyntaxOWLObjectRendererImpl r = new ManchesterOWLSyntaxOWLObjectRendererImpl();

    ManchesterRenderer ()
    {
        r.setShortFormProvider( new SimpleShortFormProvider() );
    }

    @Override public String formatId ()   { return "manchester"; }
    @Override public String displayName () { return "Manchester Syntax"; }

    @Override public String classesHeading ()          { return "Classes"; }
    @Override public String objectPropertiesHeading () { return "Object Properties"; }
    @Override public String dataPropertiesHeading ()   { return "Data Properties"; }
    @Override public String individualsHeading ()      { return "Individuals"; }

    @Override public String classKind ()          { return "Class"; }
    @Override public String objectPropertyKind () { return "ObjectProperty"; }
    @Override public String dataPropertyKind ()   { return "DataProperty"; }
    @Override public String individualKind ()     { return "NamedIndividual"; }

    /** {@code render()} is documented thread-unsafe-by-instance but synchronized internally; one instance,
     *  reused. */
    private synchronized String render (final OWLObject o) { return r.render( o ); }

    /** A Manchester frame line: {@code <keyword> <rendered-expression>}, HTML-escaped + monospaced. */
    private String line (final String keyword, final OWLObject expr, final RenderContext ctx)
    {
        return "<code class=\"manchester\">" + ctx.esc( keyword + " " + render( expr ) ) + "</code>";
    }

    private String plain (final String text, final RenderContext ctx)
    {
        return "<code class=\"manchester\">" + ctx.esc( text ) + "</code>";
    }

    @Override
    public List<String> classSentences (final OWLClass c, final RenderContext ctx)
    {
        final OWLOntology ont = ctx.ontology();
        final List<String> out = new ArrayList<>();
        for ( final OWLSubClassOfAxiom ax : ont.getSubClassAxiomsForSubClass( c ) )
            if ( !ax.getSuperClass().isOWLThing() ) out.add( line( "SubClassOf:", ax.getSuperClass(), ctx ) );
        for ( final OWLEquivalentClassesAxiom ax : ont.getEquivalentClassesAxioms( c ) )
            for ( final OWLClassExpression other : ax.getClassExpressions() )
                if ( !other.equals( c ) ) out.add( line( "EquivalentTo:", other, ctx ) );
        for ( final OWLDisjointClassesAxiom ax : ont.getDisjointClassesAxioms( c ) )
            for ( final OWLClassExpression d : ax.getClassExpressions() )
                if ( !d.isAnonymous() && !d.equals( c ) ) out.add( line( "DisjointWith:", d, ctx ) );
        out.sort( null );                                            // deterministic order
        return out;
    }

    @Override
    public List<String> objectPropertySentences (final OWLObjectProperty p, final RenderContext ctx)
    {
        final OWLOntology ont = ctx.ontology();
        final List<String> out = new ArrayList<>();
        for ( final OWLObjectPropertyDomainAxiom ax : ont.getObjectPropertyDomainAxioms( p ) )
            if ( !ax.getDomain().isOWLThing() ) out.add( line( "Domain:", ax.getDomain(), ctx ) );
        for ( final OWLObjectPropertyRangeAxiom ax : ont.getObjectPropertyRangeAxioms( p ) )
            if ( !ax.getRange().isOWLThing() ) out.add( line( "Range:", ax.getRange(), ctx ) );
        for ( final OWLSubObjectPropertyOfAxiom ax : ont.getObjectSubPropertyAxiomsForSubProperty( p ) )
            if ( !ax.getSuperProperty().isAnonymous() ) out.add( line( "SubPropertyOf:", ax.getSuperProperty(), ctx ) );
        for ( final OWLInverseObjectPropertiesAxiom ax : ont.getInverseObjectPropertyAxioms( p ) )
            for ( final OWLObjectPropertyExpression inv : ax.getPropertiesMinus( p ) )
                if ( !inv.isAnonymous() ) out.add( line( "InverseOf:", inv, ctx ) );

        final List<String> chars = new ArrayList<>();
        if ( !ont.getFunctionalObjectPropertyAxioms( p ).isEmpty() )         chars.add( "Functional" );
        if ( !ont.getInverseFunctionalObjectPropertyAxioms( p ).isEmpty() )  chars.add( "InverseFunctional" );
        if ( !ont.getTransitiveObjectPropertyAxioms( p ).isEmpty() )         chars.add( "Transitive" );
        if ( !ont.getSymmetricObjectPropertyAxioms( p ).isEmpty() )          chars.add( "Symmetric" );
        if ( !ont.getAsymmetricObjectPropertyAxioms( p ).isEmpty() )         chars.add( "Asymmetric" );
        if ( !ont.getReflexiveObjectPropertyAxioms( p ).isEmpty() )          chars.add( "Reflexive" );
        if ( !ont.getIrreflexiveObjectPropertyAxioms( p ).isEmpty() )        chars.add( "Irreflexive" );
        if ( !chars.isEmpty() ) out.add( plain( "Characteristics: " + String.join( ", ", chars ), ctx ) );
        out.sort( null );
        return out;
    }

    @Override
    public List<String> dataPropertySentences (final OWLDataProperty p, final RenderContext ctx)
    {
        final OWLOntology ont = ctx.ontology();
        final List<String> out = new ArrayList<>();
        for ( final OWLDataPropertyDomainAxiom ax : ont.getDataPropertyDomainAxioms( p ) )
            if ( !ax.getDomain().isOWLThing() ) out.add( line( "Domain:", ax.getDomain(), ctx ) );
        for ( final OWLDataPropertyRangeAxiom ax : ont.getDataPropertyRangeAxioms( p ) )
            out.add( line( "Range:", ax.getRange(), ctx ) );
        for ( final OWLSubDataPropertyOfAxiom ax : ont.getDataSubPropertyAxiomsForSubProperty( p ) )
            if ( !ax.getSuperProperty().isAnonymous() ) out.add( line( "SubPropertyOf:", ax.getSuperProperty(), ctx ) );
        if ( !ont.getFunctionalDataPropertyAxioms( p ).isEmpty() ) out.add( plain( "Characteristics: Functional", ctx ) );
        out.sort( null );
        return out;
    }

    @Override
    public List<String> individualSentences (final OWLNamedIndividual i, final RenderContext ctx)
    {
        final OWLOntology ont = ctx.ontology();
        final List<String> out = new ArrayList<>();
        for ( final OWLClassAssertionAxiom ax : ont.getClassAssertionAxioms( i ) )
            if ( !ax.getClassExpression().isOWLThing() ) out.add( line( "Types:", ax.getClassExpression(), ctx ) );
        for ( final OWLObjectPropertyAssertionAxiom ax : ont.getObjectPropertyAssertionAxioms( i ) )
            if ( !ax.getProperty().isAnonymous() && !ax.getObject().isAnonymous() )
                out.add( plain( "Facts: " + render( ax.getProperty() ) + " " + render( ax.getObject() ), ctx ) );
        for ( final OWLDataPropertyAssertionAxiom ax : ont.getDataPropertyAssertionAxioms( i ) )
            if ( !ax.getProperty().isAnonymous() )
                out.add( plain( "Facts: " + render( ax.getProperty() ) + " " + render( ax.getObject() ), ctx ) );
        out.sort( null );
        return out;
    }
}
