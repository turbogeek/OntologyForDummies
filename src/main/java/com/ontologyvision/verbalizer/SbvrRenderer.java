// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The <b>SBVR Structured English</b> format: reads each entity's axioms as controlled-English sentences using
 * the four SBVR roles — <b>terms</b> (concepts), <i>verbs</i> (fact types), <b>keywords</b>, and
 * <u>names</u> (individuals). All styling/escaping/glossing comes from the {@link RenderContext}; this class
 * owns only the sentence templates and the class-expression reading (the {@code EXPR} visitor).
 */
class SbvrRenderer implements AxiomRenderer
{
    /** Set at the start of each {@code *Sentences} call so the {@code EXPR} visitor can reach the context. */
    private RenderContext ctx;

    // ---- phrasing hooks: a subclass (OseRenderer) swaps these to vary the controlled-English wording --------
    /** Universal-subclass keyword: "Each" (SBVR) / "Every" (OSE). */
    protected String each ()     { return "Each"; }
    /** Existential-restriction keyword: "at least one" (SBVR) / "some" (OSE). */
    protected String someWord () { return "at least one"; }

    @Override public String formatId ()   { return "sbvr"; }
    @Override public String displayName () { return "SBVR Structured English"; }

    @Override public String classesHeading ()          { return "Vocabulary &mdash; Concepts (Classes)"; }
    @Override public String objectPropertiesHeading () { return "Fact Types &mdash; Relationships (Object Properties)"; }
    @Override public String dataPropertiesHeading ()   { return "Attributes (Data Properties)"; }
    @Override public String individualsHeading ()      { return "Individuals (Instances)"; }

    @Override public String classKind ()          { return "concept"; }
    @Override public String objectPropertyKind () { return "fact type"; }
    @Override public String dataPropertyKind ()   { return "attribute"; }
    @Override public String individualKind ()     { return "instance"; }

    // ---- per-entity sentences -----------------------------------------------------------------------------

    @Override
    public List<String> classSentences (final OWLClass c, final RenderContext ctx)
    {
        this.ctx = ctx;
        final OWLOntology ont = ctx.ontology();
        final List<String> sentences = new ArrayList<>();

        for ( final OWLSubClassOfAxiom ax : ont.getSubClassAxiomsForSubClass( c ) )
        {
            final OWLClassExpression sup = ax.getSuperClass();
            if ( sup.isOWLThing() ) continue;
            if ( !sup.isAnonymous() )
            {
                final OWLClass sc = sup.asOWLClass();
                sentences.add( ctx.kw( each() ) + " " + ctx.term( c ) + " " + ctx.kw( "is " + ctx.aAn( ctx.label( sc ) ) ) + " "
                        + ctx.term( sc ) + "." );
            }
            else
            {
                sentences.add( ctx.kw( each() ) + " " + ctx.term( c ) + " " + render( sup ) + "." );
            }
        }
        for ( final OWLEquivalentClassesAxiom ax : ont.getEquivalentClassesAxioms( c ) )
        {
            for ( final OWLClassExpression other : ax.getClassExpressions() )
            {
                if ( other.equals( c ) ) continue;
                sentences.add( ctx.term( c ) + " " + ctx.kw( "is defined as" ) + " " + render( other ) + "." );
            }
        }
        final Set<OWLClass> disjoints = new TreeSet<>();
        for ( final OWLDisjointClassesAxiom ax : ont.getDisjointClassesAxioms( c ) )
        {
            for ( final OWLClassExpression ce : ax.getClassExpressions() )
            {
                if ( !ce.isAnonymous() && !ce.equals( c ) ) disjoints.add( ce.asOWLClass() );
            }
        }
        if ( !disjoints.isEmpty() )
        {
            final List<String> ts = new ArrayList<>();
            for ( final OWLClass d : disjoints ) ts.add( ctx.term( d ) );
            sentences.add( ctx.kw( "No" ) + " " + ctx.term( c ) + " " + ctx.kw( "is a" ) + " " + ctx.join( ts, " or " ) + "." );
        }
        return sentences;
    }

    @Override
    public List<String> objectPropertySentences (final OWLObjectProperty p, final RenderContext ctx)
    {
        this.ctx = ctx;
        final OWLOntology ont = ctx.ontology();
        final List<String> s = new ArrayList<>();
        final List<String> domains = new ArrayList<>();
        for ( final OWLObjectPropertyDomainAxiom ax : ont.getObjectPropertyDomainAxioms( p ) )
            if ( !ax.getDomain().isOWLThing() ) domains.add( render( ax.getDomain() ) );
        final List<String> ranges = new ArrayList<>();
        for ( final OWLObjectPropertyRangeAxiom ax : ont.getObjectPropertyRangeAxioms( p ) )
            if ( !ax.getRange().isOWLThing() ) ranges.add( render( ax.getRange() ) );
        if ( !domains.isEmpty() && !ranges.isEmpty() )
            s.add( ctx.kw( "A" ) + " " + ctx.join( domains, " or " ) + " " + ctx.verb( p ) + " " + ctx.kw( "a" ) + " "
                    + ctx.join( ranges, " or " ) + "." );
        else if ( !domains.isEmpty() )
            s.add( ctx.kw( "Anything that" ) + " " + ctx.verb( p ) + " something " + ctx.kw( "is a" ) + " "
                    + ctx.join( domains, " or " ) + "." );
        else if ( !ranges.isEmpty() )
            s.add( ctx.kw( "Anything" ) + " " + ctx.verb( p ) + " " + ctx.kw( "is a" ) + " " + ctx.join( ranges, " or " ) + "." );

        for ( final OWLSubObjectPropertyOfAxiom ax : ont.getObjectSubPropertyAxiomsForSubProperty( p ) )
            if ( !ax.getSuperProperty().isAnonymous() )
                s.add( ctx.verb( p ) + " " + ctx.kw( "is a kind of" ) + " " + ctx.verb( ax.getSuperProperty().asOWLObjectProperty() ) + "." );
        for ( final OWLInverseObjectPropertiesAxiom ax : ont.getInverseObjectPropertyAxioms( p ) )
            for ( final OWLObjectPropertyExpression inv : ax.getPropertiesMinus( p ) )
                if ( !inv.isAnonymous() )
                    s.add( ctx.verb( p ) + " " + ctx.kw( "is the inverse of" ) + " " + ctx.verb( inv.asOWLObjectProperty() ) + "." );

        final List<String> chars = new ArrayList<>();
        if ( !ont.getFunctionalObjectPropertyAxioms( p ).isEmpty() )         chars.add( "functional" );
        if ( !ont.getInverseFunctionalObjectPropertyAxioms( p ).isEmpty() )  chars.add( "inverse-functional" );
        if ( !ont.getTransitiveObjectPropertyAxioms( p ).isEmpty() )         chars.add( "transitive" );
        if ( !ont.getSymmetricObjectPropertyAxioms( p ).isEmpty() )          chars.add( "symmetric" );
        if ( !ont.getAsymmetricObjectPropertyAxioms( p ).isEmpty() )         chars.add( "asymmetric" );
        if ( !ont.getReflexiveObjectPropertyAxioms( p ).isEmpty() )          chars.add( "reflexive" );
        if ( !ont.getIrreflexiveObjectPropertyAxioms( p ).isEmpty() )        chars.add( "irreflexive" );
        if ( !chars.isEmpty() )
            s.add( ctx.kw( "The relationship" ) + " " + ctx.verb( p ) + " " + ctx.kw( "is" ) + " " + ctx.kw( ctx.join( chars, " and " ) ) + "." );
        return s;
    }

    @Override
    public List<String> dataPropertySentences (final OWLDataProperty p, final RenderContext ctx)
    {
        this.ctx = ctx;
        final OWLOntology ont = ctx.ontology();
        final List<String> s = new ArrayList<>();
        final List<String> domains = new ArrayList<>();
        for ( final OWLDataPropertyDomainAxiom ax : ont.getDataPropertyDomainAxioms( p ) )
            if ( !ax.getDomain().isOWLThing() ) domains.add( render( ax.getDomain() ) );
        final List<String> ranges = new ArrayList<>();
        for ( final OWLDataPropertyRangeAxiom ax : ont.getDataPropertyRangeAxioms( p ) )
            ranges.add( dataRange( ax.getRange() ) );
        if ( !domains.isEmpty() && !ranges.isEmpty() )
            s.add( ctx.kw( "A" ) + " " + ctx.join( domains, " or " ) + " " + ctx.verb( p ) + " " + ctx.kw( "a value of type" ) + " "
                    + ctx.join( ranges, " or " ) + "." );
        else if ( !domains.isEmpty() )
            s.add( ctx.kw( "Anything that has a" ) + " " + ctx.verb( p ) + " " + ctx.kw( "is a" ) + " " + ctx.join( domains, " or " ) + "." );
        else if ( !ranges.isEmpty() )
            s.add( ctx.kw( "The value of" ) + " " + ctx.verb( p ) + " " + ctx.kw( "is of type" ) + " " + ctx.join( ranges, " or " ) + "." );
        if ( !ont.getFunctionalDataPropertyAxioms( p ).isEmpty() )
            s.add( ctx.kw( "The attribute" ) + " " + ctx.verb( p ) + " " + ctx.kw( "is single-valued (functional)" ) + "." );
        return s;
    }

    @Override
    public List<String> individualSentences (final OWLNamedIndividual i, final RenderContext ctx)
    {
        this.ctx = ctx;
        final OWLOntology ont = ctx.ontology();
        final List<String> s = new ArrayList<>();
        for ( final OWLClassAssertionAxiom ax : ont.getClassAssertionAxioms( i ) )
        {
            final OWLClassExpression ce = ax.getClassExpression();
            if ( ce.isOWLThing() ) continue;
            if ( !ce.isAnonymous() )
                s.add( ctx.name( i ) + " " + ctx.kw( "is " + ctx.aAn( ctx.label( ce.asOWLClass() ) ) ) + " " + ctx.term( ce.asOWLClass() ) + "." );
            else
                s.add( ctx.name( i ) + " " + ctx.kw( "is a" ) + " " + render( ce ) + "." );
        }
        for ( final OWLObjectPropertyAssertionAxiom ax : ont.getObjectPropertyAssertionAxioms( i ) )
            if ( !ax.getProperty().isAnonymous() && !ax.getObject().isAnonymous() )
                s.add( ctx.name( i ) + " " + ctx.verb( ax.getProperty().asOWLObjectProperty() ) + " "
                        + ctx.name( ax.getObject().asOWLNamedIndividual() ) + "." );
        for ( final OWLDataPropertyAssertionAxiom ax : ont.getDataPropertyAssertionAxioms( i ) )
            if ( !ax.getProperty().isAnonymous() )
                s.add( ctx.name( i ) + " " + ctx.verb( ax.getProperty().asOWLDataProperty() ) + " " + ctx.lit( ax.getObject() ) + "." );
        return s;
    }

    // ---- class-expression rendering (SBVR reading of restrictions etc.) -----------------------------------

    private String render (final OWLClassExpression ce) { return ce.accept( EXPR ); }

    private final OWLClassExpressionVisitorEx<String> EXPR = new OWLClassExpressionVisitorEx<String>()
    {
        @Override public String visit (final OWLClass ce) { return ce.isOWLThing() ? ctx.kw( "thing" ) : ctx.term( ce ); }
        @Override public String visit (final OWLObjectSomeValuesFrom ce )
        { return verbObj( ce.getProperty() ) + " " + ctx.kw( someWord() ) + " " + render( ce.getFiller() ); }
        @Override public String visit (final OWLObjectAllValuesFrom ce )
        { return verbObj( ce.getProperty() ) + " " + ctx.kw( "only" ) + " " + render( ce.getFiller() ); }
        @Override public String visit (final OWLObjectExactCardinality ce )
        { return verbObj( ce.getProperty() ) + " " + ctx.kw( "exactly " + ce.getCardinality() ) + " " + render( ce.getFiller() ); }
        @Override public String visit (final OWLObjectMinCardinality ce )
        { return verbObj( ce.getProperty() ) + " " + ctx.kw( "at least " + ce.getCardinality() ) + " " + render( ce.getFiller() ); }
        @Override public String visit (final OWLObjectMaxCardinality ce )
        { return verbObj( ce.getProperty() ) + " " + ctx.kw( "at most " + ce.getCardinality() ) + " " + render( ce.getFiller() ); }
        @Override public String visit (final OWLObjectHasValue ce )
        { return verbObj( ce.getProperty() ) + " " + ( ce.getFiller().isNamed()
                ? ctx.name( ce.getFiller().asOWLNamedIndividual() ) : ctx.kw( "a particular individual" ) ); }
        @Override public String visit (final OWLObjectIntersectionOf ce ) { return joinExpr( ce.getOperands(), " and " ); }
        @Override public String visit (final OWLObjectUnionOf ce ) { return joinExpr( ce.getOperands(), " or " ); }
        @Override public String visit (final OWLObjectComplementOf ce ) { return ctx.kw( "not" ) + " " + render( ce.getOperand() ); }
        @Override public String visit (final OWLObjectOneOf ce )
        {
            final List<String> ns = new ArrayList<>();
            for ( final OWLIndividual ind : ce.getIndividuals() )
                ns.add( ind.isNamed() ? ctx.name( ind.asOWLNamedIndividual() ) : "an individual" );
            return ctx.kw( "one of" ) + " " + ctx.join( ns, ", " );
        }
        @Override public String visit (final OWLObjectHasSelf ce ) { return verbObj( ce.getProperty() ) + " " + ctx.kw( "itself" ); }
        @Override public String visit (final OWLDataSomeValuesFrom ce )
        { return verbData( ce.getProperty() ) + " " + ctx.kw( "some" ) + " " + dataRange( ce.getFiller() ); }
        @Override public String visit (final OWLDataAllValuesFrom ce )
        { return verbData( ce.getProperty() ) + " " + ctx.kw( "only" ) + " " + dataRange( ce.getFiller() ); }
        @Override public String visit (final OWLDataExactCardinality ce )
        { return verbData( ce.getProperty() ) + " " + ctx.kw( "exactly " + ce.getCardinality() ) + " value(s)"; }
        @Override public String visit (final OWLDataMinCardinality ce )
        { return verbData( ce.getProperty() ) + " " + ctx.kw( "at least " + ce.getCardinality() ) + " value(s)"; }
        @Override public String visit (final OWLDataMaxCardinality ce )
        { return verbData( ce.getProperty() ) + " " + ctx.kw( "at most " + ce.getCardinality() ) + " value(s)"; }
        @Override public String visit (final OWLDataHasValue ce ) { return verbData( ce.getProperty() ) + " " + ctx.lit( ce.getFiller() ); }
    };

    private String joinExpr (final Set<OWLClassExpression> ops, final String sep)
    {
        final List<String> parts = new ArrayList<>();
        for ( final OWLClassExpression op : ops ) parts.add( render( op ) );
        parts.sort( null );                                           // deterministic operand order
        return ctx.join( parts, sep );
    }

    private String verbObj (final OWLObjectPropertyExpression pe)
    { return pe.isAnonymous() ? ctx.verbRaw( "(inverse property)" ) : ctx.verb( pe.asOWLObjectProperty() ); }
    private String verbData (final OWLDataPropertyExpression pe)
    { return pe.isAnonymous() ? ctx.verbRaw( "(data property)" ) : ctx.verb( (OWLDataProperty) pe ); }
    private String dataRange (final OWLDataRange dr)
    { return dr.isOWLDatatype() ? ctx.term( ctx.shortForm( dr.asOWLDatatype().getIRI() ) ) : ctx.esc( dr.toString() ); }
}
