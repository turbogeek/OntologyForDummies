// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verbalizes an OWL ontology (or a chosen subset of its entities) into <b>SBVR Structured English</b> — a
 * controlled natural-language reading of the ontology's axioms — and renders it as a styled, self-contained
 * HTML report.
 *
 * <p>Depends only on the OWL API (no MagicDraw types), so it runs <b>headlessly</b>: feed it any
 * {@link OWLOntology} and it returns one self-contained HTML {@code String}. The report can include the SBVR
 * verbalization, a <b>model glossary</b> (this ontology's own terms + meanings), and the static <b>OWL</b> and
 * <b>RDF</b> reference glossaries; what it includes, its initial SBVR color level, and whether interactive
 * rollover data is emitted are controlled by {@link VerbalizerOptions}. The HTML also embeds (for a real browser; a
 * Swing JEditorPane ignores the script) view-time toggles for colors and sections and a bottom hover panel
 * that shows each token's glossary meaning. The {@link Main} class is a headless CLI.
 *
 * <p>The engine is format-agnostic: it owns entity iteration, the glossaries, the HTML/CSS shell and the
 * shared token styling, and delegates each entity's sentences to a pluggable {@link AxiomRenderer} (today
 * {@link SbvrRenderer}; Manchester / OSE / ACE plug in the same way). SBVR styling: <b>terms</b> (concepts)
 * green-underlined; <i>verbs</i> (fact types) blue-italic; <b>keywords</b> orange-bold; <u>names</u>
 * (individuals) teal double-underlined; literals plain.
 */
public final class OntologyVerbalizer
{
    private VerbalizerOptions opts = VerbalizerOptions.DEFAULT;
    private OWLOntology ont;                              // the primary ontology being verbalized
    private Set<OWLOntology> lastOnts = new LinkedHashSet<>();

    /** The verbalization FORMAT. SBVR today; Manchester / OSE / ACE plug in here without engine changes. */
    private final AxiomRenderer renderer = new SbvrRenderer();
    /** Shared primitives (styled tokens, names, escaping) the engine hands to {@link #renderer}. */
    private final RenderContext ctx = new Ctx();

    // ---- public API -------------------------------------------------------------------------------------

    /** Verbalizes the whole ontology with {@link VerbalizerOptions#DEFAULT}. */
    public String verbalizeOntology (final OWLOntology ont, final String title)
    {
        return verbalizeOntology( ont, title, VerbalizerOptions.DEFAULT );
    }

    /**
     * Verbalizes the whole ontology into a styled HTML report.
     * @param ont   the ontology to read
     * @param title a human title for the report header
     * @param opts  which sections to emit, color level, rollover
     * @return a complete, self-contained HTML document
     */
    public String verbalizeOntology (final OWLOntology ont, final String title, final VerbalizerOptions opts)
    {
        this.opts = ( opts == null ? VerbalizerOptions.DEFAULT : opts );
        this.ont = ont;
        this.lastOnts = ont.getImportsClosure();

        final List<OWLClass> classes = sorted( ont.getClassesInSignature() );
        final List<OWLObjectProperty> objProps = sorted( ont.getObjectPropertiesInSignature() );
        final List<OWLDataProperty> dataProps = sorted( ont.getDataPropertiesInSignature() );
        final List<OWLNamedIndividual> individuals = sorted( ont.getIndividualsInSignature() );

        final String body = assembleBody( classes, objProps, dataProps, individuals, false );

        final int op = objProps.size(), dp = dataProps.size();
        final String subtitle = classes.size() + " classes &middot; " + ( op + dp ) + " properties &middot; "
                + individuals.size() + " individuals &middot; " + ont.getAxiomCount() + " axioms";
        return htmlDoc( title, ontologyIri( ont ), subtitle, body, classes, objProps, dataProps, individuals );
    }

    /** Verbalizes only the given entities with {@link VerbalizerOptions#DEFAULT}. */
    public String verbalizeEntities (final OWLOntology ont, final Set<OWLEntity> entities, final String title)
    {
        return verbalizeEntities( ont, entities, title, VerbalizerOptions.DEFAULT );
    }

    /**
     * Verbalizes only the given entities (e.g. a context-menu selection) plus the axioms that mention them.
     * @param ont      the ontology to read
     * @param entities the focus entities (classes, properties, individuals)
     * @param title    a human title for the report header
     * @param opts     which sections to emit, color level, rollover
     * @return a complete, self-contained HTML document
     */
    public String verbalizeEntities (final OWLOntology ont, final Set<OWLEntity> entities, final String title,
                                     final VerbalizerOptions opts)
    {
        this.opts = ( opts == null ? VerbalizerOptions.DEFAULT : opts );
        this.ont = ont;
        this.lastOnts = ont.getImportsClosure();

        final List<OWLClass> classes = new ArrayList<>();
        final List<OWLObjectProperty> objProps = new ArrayList<>();
        final List<OWLDataProperty> dataProps = new ArrayList<>();
        final List<OWLNamedIndividual> individuals = new ArrayList<>();
        for ( final OWLEntity e : entities )
        {
            if ( e.isOWLClass() )                  classes.add( e.asOWLClass() );
            else if ( e.isOWLObjectProperty() )    objProps.add( e.asOWLObjectProperty() );
            else if ( e.isOWLDataProperty() )      dataProps.add( e.asOWLDataProperty() );
            else if ( e.isOWLNamedIndividual() )   individuals.add( e.asOWLNamedIndividual() );
        }
        classes.sort( byLabel() ); objProps.sort( byLabel() ); dataProps.sort( byLabel() ); individuals.sort( byLabel() );

        String body = assembleBody( classes, objProps, dataProps, individuals, true );
        if ( body.isEmpty() )
        {
            body = "<p class=\"note\">No verbalizable axioms were found for the selected element(s).</p>";
        }
        final String subtitle = entities.size() + " selected element(s)";
        return htmlDoc( title, ontologyIri( ont ), subtitle, body, classes, objProps, dataProps, individuals );
    }

    /** Builds the section body per {@link #opts}: verbalization, model glossary, then OWL/RDF reference glossaries. */
    private String assembleBody (final List<OWLClass> classes, final List<OWLObjectProperty> objProps,
                                 final List<OWLDataProperty> dataProps, final List<OWLNamedIndividual> individuals,
                                 final boolean selection)
    {
        final StringBuilder body = new StringBuilder();

        if ( opts.includeVerbalization )
        {
            final StringBuilder v = new StringBuilder();
            verbalizeClassesSection( ont, classes, v );
            verbalizeObjectPropsSection( ont, objProps, v );
            verbalizeDataPropsSection( ont, dataProps, v );
            verbalizeIndividualsSection( ont, individuals, v );
            wrap( body, "sec-verbalization", v.toString() );
        }
        if ( opts.includeModelGlossary )
        {
            wrap( body, "sec-model", modelGlossarySection( classes, objProps, dataProps, individuals, selection ) );
        }
        if ( opts.includeOwlGlossary )
        {
            wrap( body, "sec-owl", referenceGlossarySection( "OWL Constructs", GlossaryData.owl() ) );
        }
        if ( opts.includeRdfGlossary )
        {
            wrap( body, "sec-rdf", referenceGlossarySection( "RDF / RDFS Constructs", GlossaryData.rdf() ) );
        }
        return body.toString();
    }

    private static void wrap (final StringBuilder out, final String cls, final String html)
    {
        if ( html != null && !html.isEmpty() ) out.append( "<div class=\"" ).append( cls ).append( "\">" ).append( html ).append( "</div>" );
    }

    // ---- verbalization sections: the engine owns iteration, skip rules and section assembly; the renderer
    //      supplies each entity's sentences + the format's headings/kinds. --------------------------------

    private void verbalizeClassesSection (final OWLOntology ont, final List<OWLClass> classes,
                                          final StringBuilder out)
    {
        final List<String> blocks = new ArrayList<>();
        for ( final OWLClass c : classes )
        {
            if ( c.isOWLThing() || c.isOWLNothing() ) continue;
            final List<String> sentences = renderer.classSentences( c, ctx );
            final String comment = comment( ont, c );
            if ( sentences.isEmpty() && comment == null ) continue;   // nothing to say about a bare class
            blocks.add( entityBlock( renderer.classKind(), term( c ), sentences, comment ) );
        }
        if ( !blocks.isEmpty() ) section( renderer.classesHeading(), blocks, out );
    }

    private void verbalizeObjectPropsSection (final OWLOntology ont, final List<OWLObjectProperty> props,
                                              final StringBuilder out)
    {
        final List<String> blocks = new ArrayList<>();
        for ( final OWLObjectProperty p : props )
        {
            if ( p.isOWLTopObjectProperty() || p.isOWLBottomObjectProperty() ) continue;
            final List<String> s = renderer.objectPropertySentences( p, ctx );
            final String comment = comment( ont, p );
            if ( s.isEmpty() && comment == null ) continue;
            blocks.add( entityBlock( renderer.objectPropertyKind(), verb( p ), s, comment ) );
        }
        if ( !blocks.isEmpty() ) section( renderer.objectPropertiesHeading(), blocks, out );
    }

    private void verbalizeDataPropsSection (final OWLOntology ont, final List<OWLDataProperty> props,
                                            final StringBuilder out)
    {
        final List<String> blocks = new ArrayList<>();
        for ( final OWLDataProperty p : props )
        {
            if ( p.isOWLTopDataProperty() || p.isOWLBottomDataProperty() ) continue;
            final List<String> s = renderer.dataPropertySentences( p, ctx );
            final String comment = comment( ont, p );
            if ( s.isEmpty() && comment == null ) continue;
            blocks.add( entityBlock( renderer.dataPropertyKind(), verb( p ), s, comment ) );
        }
        if ( !blocks.isEmpty() ) section( renderer.dataPropertiesHeading(), blocks, out );
    }

    private void verbalizeIndividualsSection (final OWLOntology ont, final List<OWLNamedIndividual> inds,
                                              final StringBuilder out)
    {
        final List<String> blocks = new ArrayList<>();
        for ( final OWLNamedIndividual i : inds )
        {
            final List<String> s = renderer.individualSentences( i, ctx );
            final String comment = comment( ont, i );
            if ( s.isEmpty() && comment == null ) continue;
            blocks.add( entityBlock( renderer.individualKind(), name( i ), s, comment ) );
        }
        if ( !blocks.isEmpty() ) section( renderer.individualsHeading(), blocks, out );
    }

    // ---- model glossary + reference glossaries ----------------------------------------------------------

    private String modelGlossarySection (final List<OWLClass> classes, final List<OWLObjectProperty> objProps,
                                         final List<OWLDataProperty> dataProps, final List<OWLNamedIndividual> inds,
                                         final boolean selection)
    {
        final StringBuilder rows = new StringBuilder();
        int n = 0;
        for ( final OWLClass c : classes )           { if ( c.isOWLThing() || c.isOWLNothing() ) continue; rows.append( glossaryRow( c, "concept", term( c ) ) );   n++; }
        for ( final OWLObjectProperty p : objProps ) { if ( p.isOWLTopObjectProperty() || p.isOWLBottomObjectProperty() ) continue; rows.append( glossaryRow( p, "fact type", verb( p ) ) ); n++; }
        for ( final OWLDataProperty p : dataProps )  { if ( p.isOWLTopDataProperty() || p.isOWLBottomDataProperty() ) continue; rows.append( glossaryRow( p, "attribute", verb( p ) ) ); n++; }
        for ( final OWLNamedIndividual i : inds )    { rows.append( glossaryRow( i, "individual", name( i ) ) ); n++; }
        if ( n == 0 ) return "";
        final String heading = selection ? "Model Glossary &mdash; selected terms" : "Model Glossary";
        return "<h2>" + heading + " <span class=\"count\">" + n + "</span></h2>"
                + "<p class=\"note\">The vocabulary of this ontology &mdash; its concepts, fact types, and "
                + "individuals &mdash; with the meaning of each (its <code>rdfs:comment</code>, or a meaning "
                + "derived from its axioms).</p>"
                + "<table class=\"gloss\"><tr><th>Designation</th><th>Kind</th><th>Meaning</th></tr>"
                + rows + "</table>";
    }

    private String glossaryRow (final OWLEntity e, final String kind, final String designationHtml)
    {
        return "<tr id=\"gl-" + escAttr( shortForm( e.getIRI() ) ) + "\"><td>" + designationHtml + "</td><td>"
                + kind + "</td><td>" + esc( glossPlain( e ) ) + "</td></tr>";
    }

    private String referenceGlossarySection (final String heading, final List<GlossaryData.Entry> entries)
    {
        if ( entries == null || entries.isEmpty() ) return "";
        final StringBuilder rows = new StringBuilder();
        for ( final GlossaryData.Entry en : entries )
        {
            rows.append( "<tr><td class=\"gloss-term\">" ).append( esc( en.term ) ).append( "</td><td>" )
                .append( esc( en.meaning ) ).append( "</td><td class=\"gloss-sbvr\">" )
                .append( esc( en.sbvrReading ) ).append( "</td></tr>" );
        }
        return "<h2>" + heading + " <span class=\"count\">" + entries.size() + "</span></h2>"
                + "<table class=\"gloss\"><tr><th>Term</th><th>Meaning</th><th>SBVR reading</th></tr>"
                + rows + "</table>";
    }

    // ---- styled spans (with data-gloss rollover when enabled) — the shared token vocabulary every format
    //      emits through the RenderContext (see SbvrRenderer for the SBVR class-expression reading) --------

    private String role (final String cls, final String glossPlain, final String displayHtml)
    {
        if ( opts.rollover && glossPlain != null && !glossPlain.isEmpty() )
            // An <a href="gloss:..."> so the meaning is readable BOTH by the browser's mouseover JS and by the
            // Swing JEditorPane's HyperlinkListener (which fires only for anchors, never plain spans). The
            // "gloss:" URL scheme is inert — clicks are suppressed in the browser and yield a null URL in Swing.
            return "<a class=\"" + cls + "\" href=\"gloss:" + urlEnc( glossPlain ) + "\">" + displayHtml + "</a>";
        return "<span class=\"" + cls + "\">" + displayHtml + "</span>";
    }

    /** Percent-encodes a gloss so it can ride inside an {@code href="gloss:..."} anchor. Encodes space as
     *  {@code %20} (not {@code +}) so the browser's {@code decodeURIComponent} and Java's {@code URLDecoder}
     *  both recover the original text. */
    private static String urlEnc (final String s)
    {
        try { return java.net.URLEncoder.encode( s, "UTF-8" ).replace( "+", "%20" ); }
        catch ( final Exception e ) { return s.replace( " ", "%20" ).replace( "\"", "%22" ); }
    }

    private String term (final OWLEntity e)  { return role( "term", glossPlain( e ), esc( label( e ) ) ); }
    private String term (final String s)     { return "<span class=\"term\">" + esc( s ) + "</span>"; }
    private String verb (final OWLEntity e)  { return role( "verb", glossPlain( e ), esc( label( e ) ) ); }
    private String verbRaw (final String s)  { return "<span class=\"verb\">" + esc( s ) + "</span>"; }
    private String name (final OWLEntity e)  { return role( "name", glossPlain( e ), esc( label( e ) ) ); }
    private String kw (final String s)       { return role( "kw", keywordGloss( s ), esc( s ) ); }
    private String lit (final OWLLiteral l)  { return "<span class=\"lit\">" + esc( '“' + l.getLiteral() + '”' ) + "</span>"; }

    // ---- meanings (glossary text for the model glossary + rollover) -------------------------------------

    /** Plain-text meaning of an entity: its {@code rdfs:comment} if present, else a meaning derived from its axioms. */
    private String glossPlain (final OWLEntity e)
    {
        final String c = rawComment( e );
        if ( c != null && !c.isEmpty() ) return c;
        return generatedGloss( e );
    }

    private String generatedGloss (final OWLEntity e)
    {
        if ( ont == null ) return "";
        if ( e.isOWLClass() )
        {
            final List<String> supers = new ArrayList<>();
            for ( final OWLSubClassOfAxiom ax : ont.getSubClassAxiomsForSubClass( e.asOWLClass() ) )
                if ( !ax.getSuperClass().isAnonymous() && !ax.getSuperClass().isOWLThing() )
                    supers.add( label( ax.getSuperClass().asOWLClass() ) );
            supers.sort( null );
            return supers.isEmpty() ? "A concept (class)." : "A kind of " + naturalList( supers ) + ".";
        }
        if ( e.isOWLObjectProperty() )
        {
            final String d = firstNamedDomainObj( e.asOWLObjectProperty() );
            final String r = firstNamedRangeObj( e.asOWLObjectProperty() );
            if ( d != null && r != null ) return "A relationship from a " + d + " to a " + r + ".";
            if ( r != null ) return "A relationship to a " + r + ".";
            return "A relationship (object property).";
        }
        if ( e.isOWLDataProperty() )
        {
            for ( final OWLDataPropertyRangeAxiom ax : ont.getDataPropertyRangeAxioms( e.asOWLDataProperty() ) )
                if ( ax.getRange().isOWLDatatype() ) return "An attribute whose value is a " + label( ax.getRange().asOWLDatatype() ) + ".";
            return "An attribute (data property).";
        }
        if ( e.isOWLNamedIndividual() )
        {
            final List<String> types = new ArrayList<>();
            for ( final OWLClassAssertionAxiom ax : ont.getClassAssertionAxioms( e.asOWLNamedIndividual() ) )
                if ( !ax.getClassExpression().isAnonymous() && !ax.getClassExpression().isOWLThing() )
                    types.add( label( ax.getClassExpression().asOWLClass() ) );
            types.sort( null );
            return types.isEmpty() ? "An individual (instance)." : "An individual that is a " + naturalList( types ) + ".";
        }
        return "";
    }

    private String firstNamedDomainObj (final OWLObjectProperty p)
    {
        for ( final OWLObjectPropertyDomainAxiom ax : ont.getObjectPropertyDomainAxioms( p ) )
            if ( !ax.getDomain().isAnonymous() && !ax.getDomain().isOWLThing() ) return label( ax.getDomain().asOWLClass() );
        return null;
    }

    private String firstNamedRangeObj (final OWLObjectProperty p)
    {
        for ( final OWLObjectPropertyRangeAxiom ax : ont.getObjectPropertyRangeAxioms( p ) )
            if ( !ax.getRange().isAnonymous() && !ax.getRange().isOWLThing() ) return label( ax.getRange().asOWLClass() );
        return null;
    }

    /** The OWL/RDF construct meaning for an SBVR keyword phrase (for keyword rollovers), or {@code null}. */
    private String keywordGloss (final String keyword)
    {
        if ( keyword == null ) return null;
        // normalize: lowercase, strip a trailing count ("exactly 3" -> "exactly") and "value(s)"
        String k = keyword.toLowerCase().trim().replaceAll( "\\s+\\d+$", "" ).replace( " value(s)", "" ).trim();
        final String term = KEYWORD_TO_CONSTRUCT.get( k );
        if ( term == null ) return null;
        final String meaning = constructMeaning( term );
        return meaning == null ? null : term + " — " + meaning;
    }

    private static String constructMeaning (final String term)
    {
        for ( final GlossaryData.Entry e : GlossaryData.owl() ) if ( e.term.equals( term ) ) return e.meaning;
        for ( final GlossaryData.Entry e : GlossaryData.rdf() ) if ( e.term.equals( term ) ) return e.meaning;
        return null;
    }

    private static final Map<String, String> KEYWORD_TO_CONSTRUCT = new HashMap<>();
    static
    {
        KEYWORD_TO_CONSTRUCT.put( "each", "rdfs:subClassOf" );
        KEYWORD_TO_CONSTRUCT.put( "is a", "rdfs:subClassOf" );
        KEYWORD_TO_CONSTRUCT.put( "is an", "rdfs:subClassOf" );
        KEYWORD_TO_CONSTRUCT.put( "at least one", "owl:someValuesFrom" );
        KEYWORD_TO_CONSTRUCT.put( "only", "owl:allValuesFrom" );
        KEYWORD_TO_CONSTRUCT.put( "some", "owl:someValuesFrom" );
        KEYWORD_TO_CONSTRUCT.put( "exactly", "owl:cardinality" );
        KEYWORD_TO_CONSTRUCT.put( "at least", "owl:minCardinality" );
        KEYWORD_TO_CONSTRUCT.put( "at most", "owl:maxCardinality" );
        KEYWORD_TO_CONSTRUCT.put( "no", "owl:disjointWith" );
        KEYWORD_TO_CONSTRUCT.put( "is defined as", "owl:equivalentClass" );
        KEYWORD_TO_CONSTRUCT.put( "is a kind of", "rdfs:subPropertyOf" );
        KEYWORD_TO_CONSTRUCT.put( "is the inverse of", "owl:inverseOf" );
        KEYWORD_TO_CONSTRUCT.put( "and", "owl:intersectionOf" );
        KEYWORD_TO_CONSTRUCT.put( "or", "owl:unionOf" );
        KEYWORD_TO_CONSTRUCT.put( "not", "owl:complementOf" );
        KEYWORD_TO_CONSTRUCT.put( "one of", "owl:oneOf" );
        KEYWORD_TO_CONSTRUCT.put( "itself", "owl:hasSelf" );
    }

    private String naturalList (final List<String> items)
    {
        if ( items.size() == 1 ) return items.get( 0 );
        if ( items.size() == 2 ) return items.get( 0 ) + " and " + items.get( 1 );
        return join( items.subList( 0, items.size() - 1 ), ", " ) + ", and " + items.get( items.size() - 1 );
    }

    // ---- labels / comments ------------------------------------------------------------------------------

    /** Best English name: English {@code rdfs:label} if present, else a language-neutral label, else the humanized IRI fragment. */
    private String label (final OWLEntity e)
    {
        String enLabel = null, noLangLabel = null;
        for ( final OWLOntology o : lastOnts )
        {
            for ( final OWLAnnotationAssertionAxiom ax : o.getAnnotationAssertionAxioms( e.getIRI() ) )
            {
                if ( !ax.getProperty().isLabel() || !( ax.getValue() instanceof OWLLiteral ) ) continue;
                final OWLLiteral lit = (OWLLiteral) ax.getValue();
                final String lang = lit.getLang();
                if ( lang != null && lang.toLowerCase().startsWith( "en" ) )       enLabel = lit.getLiteral();
                else if ( lang == null || lang.isEmpty() )                          noLangLabel = lit.getLiteral();
            }
        }
        if ( enLabel != null )     return enLabel;
        if ( noLangLabel != null ) return noLangLabel;
        return humanize( shortForm( e.getIRI() ) );
    }

    /** Raw (un-escaped) {@code rdfs:comment}, or {@code null}. */
    private String rawComment (final OWLEntity e)
    {
        if ( ont == null ) return null;
        for ( final OWLAnnotationAssertionAxiom ax : ont.getAnnotationAssertionAxioms( e.getIRI() ) )
            if ( ax.getProperty().isComment() && ax.getValue() instanceof OWLLiteral )
                return ( (OWLLiteral) ax.getValue() ).getLiteral();
        return null;
    }

    /** Escaped {@code rdfs:comment} for display, or {@code null}. */
    private String comment (final OWLOntology ont, final OWLEntity e)
    {
        final String c = rawComment( e );
        return c == null ? null : esc( c );
    }

    // ---- HTML scaffolding -------------------------------------------------------------------------------

    private String entityBlock (final String kind, final String heading, final List<String> sentences,
                                final String comment)
    {
        sentences.sort( null );                                       // deterministic statement order
        final StringBuilder b = new StringBuilder();
        b.append( "<div class=\"entity\"><div class=\"ehead\">" ).append( heading )
                .append( " <span class=\"kind\">(" ).append( kind ).append( ")</span></div>" );
        if ( comment != null ) b.append( "<div class=\"cmt\">" ).append( comment ).append( "</div>" );
        if ( !sentences.isEmpty() )
        {
            b.append( "<ul>" );
            for ( final String s : sentences ) b.append( "<li>" ).append( s ).append( "</li>" );
            b.append( "</ul>" );
        }
        b.append( "</div>" );
        return b.toString();
    }

    private void section (final String heading, final List<String> blocks, final StringBuilder out)
    {
        out.append( "<h2>" ).append( heading ).append( " <span class=\"count\">" ).append( blocks.size() )
                .append( "</span></h2>" );
        for ( final String blk : blocks ) out.append( blk );
    }

    private String htmlDoc (final String title, final String ontIri, final String subtitle, final String body,
                            final List<OWLClass> classes, final List<OWLObjectProperty> objProps,
                            final List<OWLDataProperty> dataProps, final List<OWLNamedIndividual> inds)
    {
        final String bodyClass = ( opts.colorLevel == VerbalizerOptions.ColorLevel.MONO ) ? "mono"
                : ( opts.colorLevel == VerbalizerOptions.ColorLevel.PLAIN ) ? "no-color" : "";
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + esc( title ) + " &mdash; SBVR</title>"
                + "<style>" + css() + "</style></head>"
                + "<body class=\"" + bodyClass + "\"><div class=\"wrap\">"
                + "<h1>" + esc( title ) + "</h1>"
                + ( ontIri == null ? "" : "<p class=\"sub\">" + esc( ontIri ) + "</p>" )
                + "<div class=\"stats\">" + subtitle + "</div>"
                + "<div class=\"legend\">SBVR Structured English &mdash; "
                + "<span class=\"term\">term</span> (concept), "
                + "<span class=\"verb\">verb</span> (fact type), "
                + "<span class=\"kw\">keyword</span>, "
                + "<span class=\"name\">name</span> (individual)</div>"
                + toolbar()
                + body
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:18px 0 8px;\">"
                + "<p class=\"sub\">Generated by VOM &mdash; Visual Ontology Modeler. "
                + "Interactive controls (color &amp; section toggles, hover meanings) need a web browser.</p>"
                + "</div>"
                + glossPanel()
                + script()
                + "</body></html>";
    }

    /** View-time toolbar (works in a browser; inert in a Swing JEditorPane). Only offers toggles for present sections. */
    private String toolbar ()
    {
        final StringBuilder t = new StringBuilder( "<div class=\"toolbar\">" );
        final String sel = opts.colorLevel.name().toLowerCase();   // full|mono|plain
        t.append( "<label>SBVR colors: <select id=\"t-colormode\">" )
         .append( "<option value=\"full\"" ).append( sel.equals( "full" ) ? " selected" : "" ).append( ">Full color</option>" )
         .append( "<option value=\"mono\"" ).append( sel.equals( "mono" ) ? " selected" : "" ).append( ">Monochrome (keep type)</option>" )
         .append( "<option value=\"plain\"" ).append( sel.equals( "plain" ) ? " selected" : "" ).append( ">Plain</option>" )
         .append( "</select></label>" );
        if ( opts.includeModelGlossary ) t.append( "<label><input type=\"checkbox\" id=\"t-model\"> Hide model glossary</label>" );
        if ( opts.includeOwlGlossary )   t.append( "<label><input type=\"checkbox\" id=\"t-owl\"> Hide OWL glossary</label>" );
        if ( opts.includeRdfGlossary )   t.append( "<label><input type=\"checkbox\" id=\"t-rdf\"> Hide RDF glossary</label>" );
        return t.append( "</div>" ).toString();
    }

    private String glossPanel ()
    {
        if ( !opts.rollover ) return "";
        return "<div id=\"sbvr-gloss\" class=\"gloss-panel\"><span class=\"gloss-hint\">"
                + "Hover a term, verb, keyword or name to see its meaning.</span></div>";
    }

    private String script ()
    {
        // One delegated mouseover/mouseout listener drives the bottom panel; toolbar handlers flip body classes.
        // Inline so it works in a saved offline file. A Swing JEditorPane ignores <script> (static view still works).
        return "<script>(function(){"
                + "function g(el){while(el&&el.nodeType===1){if(el.tagName==='A'){var h=el.getAttribute('href')||'';if(h.indexOf('gloss:')===0)return h.substring(6);}el=el.parentNode;}return null;}"
                + "var p=document.getElementById('sbvr-gloss');var hint=p?p.innerHTML:'';"
                + "if(p){document.addEventListener('mouseover',function(e){var h=g(e.target);if(h!==null)p.textContent=decodeURIComponent(h);});"
                + "document.addEventListener('mouseout',function(e){if(g(e.target)!==null)p.innerHTML=hint;});"
                + "document.addEventListener('click',function(e){if(g(e.target)!==null)e.preventDefault();});}"
                + "function chk(id,cls){var c=document.getElementById(id);if(c)c.addEventListener('change',function(){document.body.classList.toggle(cls,c.checked);});}"
                + "chk('t-model','hide-model');chk('t-owl','hide-owl');chk('t-rdf','hide-rdf');"
                + "var m=document.getElementById('t-colormode');if(m)m.addEventListener('change',function(){var b=document.body.classList;b.remove('mono','no-color');if(m.value==='mono')b.add('mono');else if(m.value==='plain')b.add('no-color');});"
                + "})();</script>";
    }

    private String css ()
    {
        return "body{font-family:-apple-system,Segoe UI,Helvetica,Arial,sans-serif;margin:0;color:#1a1a1a;}"
                + ".wrap{max-width:880px;margin:0 auto;padding:18px 26px 5em;}"
                + "h1{font-size:20px;margin:0 0 2px;} .sub{color:#666;font-size:12px;margin:0 0 4px;}"
                + ".stats{color:#666;font-size:12px;margin:0 0 12px;}"
                + "h2{font-size:15px;margin:22px 0 8px;border-bottom:2px solid #eee;padding-bottom:3px;}"
                + ".count{color:#999;font-weight:normal;font-size:12px;}"
                + ".entity{margin:0 0 10px;padding:6px 0 0;} .ehead{font-size:14px;margin-bottom:2px;}"
                + ".kind{color:#999;font-size:11px;font-weight:normal;}"
                + ".cmt{color:#555;font-style:italic;font-size:12px;margin:1px 0 3px;}"
                + "ul{margin:2px 0 6px;padding-left:20px;} li{margin:1px 0;line-height:1.5;}"
                + ".term{color:#157f3b;text-decoration:underline;}"
                + ".verb{color:#1b6ca8;font-style:italic;}"
                + ".kw{color:#c8741a;font-weight:bold;}"
                + ".name{color:#0a7d7d;text-decoration:underline double;}"
                + ".lit{color:#1a1a1a;}"
                // glossable terms render as <a href="gloss:..."> for hover; keep their SBVR colours and stop
                // them looking/behaving like ordinary links (the body.mono / body.no-color rules below still
                // out-specify these to suppress colour when requested).
                + "a.term,a.verb,a.kw,a.name{cursor:default;}"
                + "a.term{color:#157f3b;text-decoration:underline;}"
                + "a.verb{color:#1b6ca8;text-decoration:none;font-style:italic;}"
                + "a.kw{color:#c8741a;text-decoration:none;font-weight:bold;}"
                + "a.name{color:#0a7d7d;text-decoration:underline;}"
                + ".legend{background:#fafafa;border:1px solid #eee;border-radius:6px;padding:6px 10px;font-size:11px;margin:0 0 8px;color:#555;}"
                + ".toolbar{font-size:11px;color:#555;margin:0 0 14px;display:flex;gap:16px;flex-wrap:wrap;align-items:center;}"
                + ".toolbar label{cursor:pointer;} .toolbar select{font-size:11px;}"
                + ".note{color:#888;font-style:italic;font-size:12px;}"
                + "table.gloss{border-collapse:collapse;width:100%;font-size:12px;margin:4px 0 8px;}"
                + "table.gloss th,table.gloss td{border:1px solid #eee;padding:4px 8px;text-align:left;vertical-align:top;}"
                + "table.gloss th{background:#fafafa;font-weight:600;}"
                + ".gloss-term{font-family:ui-monospace,Menlo,Consolas,monospace;white-space:nowrap;color:#157f3b;}"
                + ".gloss-sbvr{color:#555;}"
                + ".gloss-panel{position:fixed;left:0;right:0;bottom:0;background:#1f2933;color:#f5f7fa;"
                + "border-top:3px solid #157f3b;padding:13px 22px;font-size:14px;line-height:1.45;min-height:2.2em;"
                + "z-index:1000;box-shadow:0 -2px 10px rgba(0,0,0,.25);}"
                + ".gloss-hint{color:#9aa5b1;}"
                // color suppression levels
                + "body.mono .term,body.mono .verb,body.mono .kw,body.mono .name,body.mono .lit,body.mono .gloss-term{color:inherit;}"
                + "body.no-color .term,body.no-color .verb,body.no-color .kw,body.no-color .name,body.no-color .lit,body.no-color .gloss-term{color:inherit;}"
                + "body.no-color .term,body.no-color .name{text-decoration:none;}"
                + "body.no-color .verb{font-style:normal;} body.no-color .kw{font-weight:normal;}"
                // section hiding
                + "body.hide-model .sec-model{display:none;} body.hide-owl .sec-owl{display:none;} body.hide-rdf .sec-rdf{display:none;}"
                + "@media print{.toolbar,.gloss-panel{display:none;} .wrap{padding-bottom:18px;}}";
    }

    // ---- helpers ----------------------------------------------------------------------------------------

    private <T extends OWLEntity> List<T> sorted (final Set<T> set)
    { final List<T> l = new ArrayList<>( set ); l.sort( byLabel() ); return l; }

    private <T extends OWLEntity> Comparator<T> byLabel ()
    { return Comparator.comparing( (T e) -> shortForm( e.getIRI() ).toLowerCase() ).thenComparing( e -> e.getIRI().toString() ); }

    private static String ontologyIri (final OWLOntology ont)
    {
        final OWLOntologyID id = ont.getOntologyID();
        return id.getOntologyIRI().isPresent() ? id.getOntologyIRI().get().toString() : null;
    }

    static String shortForm (final IRI iri)
    {
        final String frag = iri.getFragment();
        if ( frag != null && !frag.isEmpty() ) return frag;
        final String s = iri.toString();
        final int slash = s.lastIndexOf( '/' );
        return slash >= 0 && slash < s.length() - 1 ? s.substring( slash + 1 ) : s;
    }

    /** "CheeseTopping" -> "Cheese Topping", "hasTopping" -> "has topping", "is_a" -> "is a". */
    static String humanize (final String name)
    {
        if ( name == null || name.isEmpty() ) return name;
        final String spaced = name.replace( '_', ' ' ).replace( '-', ' ' )
                .replaceAll( "([a-z0-9])([A-Z])", "$1 $2" )
                .replaceAll( "([A-Z]+)([A-Z][a-z])", "$1 $2" )
                .trim();
        if ( Character.isLowerCase( name.charAt( 0 ) ) ) return spaced.toLowerCase();
        return spaced;
    }

    private static String aAn (final String plainWord)
    {
        final char c = ( plainWord == null || plainWord.isEmpty() ) ? 'x' : Character.toLowerCase( plainWord.charAt( 0 ) );
        return ( "aeiou".indexOf( c ) >= 0 ) ? "an" : "a";
    }

    private static String join (final List<String> parts, final String sep)
    {
        final StringBuilder b = new StringBuilder();
        for ( int i = 0; i < parts.size(); i++ ) { if ( i > 0 ) b.append( sep ); b.append( parts.get( i ) ); }
        return b.toString();
    }

    private static String esc (final String s)
    {
        if ( s == null ) return "";
        final StringBuilder b = new StringBuilder( s.length() );
        for ( int i = 0; i < s.length(); i++ )
        {
            final char c = s.charAt( i );
            switch ( c )
            {
                case '&': b.append( "&amp;" ); break;
                case '<': b.append( "&lt;" );  break;
                case '>': b.append( "&gt;" );  break;
                case '"': b.append( "&quot;" );break;
                default:  b.append( c );
            }
        }
        return b.toString();
    }

    /** HTML-attribute-safe one-line text (for {@code data-gloss}). */
    private static String escAttr (final String s)
    {
        return esc( s ).replace( '\n', ' ' ).replace( '\r', ' ' ).replace( '\t', ' ' );
    }

    // ---- RenderContext: the engine's shared primitives, handed to the AxiomRenderer ---------------------

    /** Adapter exposing the engine's shared primitives to an {@link AxiomRenderer} without leaking internals;
     *  one instance ({@link #ctx}) is reused across the whole report. (The headless CLI is {@link Main}.) */
    private final class Ctx implements RenderContext
    {
        @Override public OWLOntology ontology () { return ont; }
        @Override public String term (final OWLEntity e) { return OntologyVerbalizer.this.term( e ); }
        @Override public String term (final String s) { return OntologyVerbalizer.this.term( s ); }
        @Override public String verb (final OWLEntity e) { return OntologyVerbalizer.this.verb( e ); }
        @Override public String verbRaw (final String s) { return OntologyVerbalizer.this.verbRaw( s ); }
        @Override public String name (final OWLEntity e) { return OntologyVerbalizer.this.name( e ); }
        @Override public String kw (final String s) { return OntologyVerbalizer.this.kw( s ); }
        @Override public String lit (final OWLLiteral l) { return OntologyVerbalizer.this.lit( l ); }
        @Override public String label (final OWLEntity e) { return OntologyVerbalizer.this.label( e ); }
        @Override public String esc (final String s) { return OntologyVerbalizer.esc( s ); }
        @Override public String shortForm (final IRI iri) { return OntologyVerbalizer.shortForm( iri ); }
        @Override public String join (final List<String> parts, final String sep) { return OntologyVerbalizer.join( parts, sep ); }
        @Override public String aAn (final String word) { return OntologyVerbalizer.aAn( word ); }
    }
}
