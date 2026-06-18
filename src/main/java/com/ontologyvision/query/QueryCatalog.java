// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.query;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the bundled query / pattern catalog ({@code qm-catalog.ttl}) into a list of {@link QueryDef}s. Mirrors
 * {@code GlossaryData}: a classpath resource parsed with the OWL API (the library's existing dependency) — no
 * SPARQL engine needed to <i>read</i> the catalog. This is the single surface the VOM query window and (later)
 * the CLI's Pillar-3 validation consume; user-saved queries reuse the same {@link QueryDef} shape.
 */
public final class QueryCatalog
{
    private static final String CATALOG_RESOURCE = "/com/ontologyvision/query/qm-catalog.ttl";

    private static volatile List<QueryDef> cache;

    private QueryCatalog () { }

    /** All catalog queries (loaded once, cached). */
    public static List<QueryDef> all ()
    {
        List<QueryDef> local = cache;
        if ( local == null )
        {
            synchronized ( QueryCatalog.class )
            {
                local = cache;
                if ( local == null )
                {
                    local = load( CATALOG_RESOURCE );
                    cache = local;
                }
            }
        }
        return local;
    }

    public static List<QueryDef> byCategory ( final String category )
    {
        final List<QueryDef> out = new ArrayList<>();
        for ( final QueryDef q : all() )
        {
            if ( category.equalsIgnoreCase( q.category ) ) { out.add( q ); }
        }
        return out;
    }

    public static List<QueryDef> stats ()        { return byCategory( "STATS" ); }
    public static List<QueryDef> discovery ()    { return byCategory( "DISCOVERY" ); }

    public static List<QueryDef> goodPatterns ()
    {
        final List<QueryDef> out = new ArrayList<>();
        for ( final QueryDef q : all() ) { if ( q.isGoodPattern() ) { out.add( q ); } }
        return out;
    }

    public static List<QueryDef> badPatterns ()
    {
        final List<QueryDef> out = new ArrayList<>();
        for ( final QueryDef q : all() ) { if ( q.isBadPattern() ) { out.add( q ); } }
        return out;
    }

    /**
     * Parse a {@code qm-catalog} Turtle resource into {@link QueryDef}s: any subject IRI that carries a
     * {@code qm:queryText} annotation is a query, and its other {@code qm:*} annotations supply the metadata.
     */
    public static List<QueryDef> load ( final String resource )
    {
        try ( InputStream in = QueryCatalog.class.getResourceAsStream( resource ) )
        {
            if ( in == null )
            {
                throw new IllegalStateException( "query catalog resource not found on the classpath: " + resource );
            }
            final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
            final OWLOntology ont = m.loadOntologyFromOntologyDocument( new StreamDocumentSource( in ) );

            // group annotation-assertion literal values: subject IRI -> property-local-name -> values
            final Map<IRI, Map<String, List<String>>> bySubject = new LinkedHashMap<>();
            ont.axioms( AxiomType.ANNOTATION_ASSERTION ).forEach( ax -> {
                if ( !( ax.getSubject() instanceof IRI ) || !( ax.getValue() instanceof OWLLiteral ) )
                {
                    return;
                }
                final IRI subj = (IRI) ax.getSubject();
                final String prop = ax.getProperty().getIRI().getRemainder().orElse( "" );
                final String val = ( (OWLLiteral) ax.getValue() ).getLiteral();
                bySubject.computeIfAbsent( subj, k -> new LinkedHashMap<>() )
                         .computeIfAbsent( prop, k -> new ArrayList<>() ).add( val );
            } );

            final List<QueryDef> defs = new ArrayList<>();
            for ( final Map.Entry<IRI, Map<String, List<String>>> e : bySubject.entrySet() )
            {
                final Map<String, List<String>> a = e.getValue();
                if ( !a.containsKey( "queryText" ) )
                {
                    continue;   // only subjects carrying SPARQL are queries (skip the ontology header etc.)
                }
                defs.add( new QueryDef(
                        e.getKey().getRemainder().orElse( e.getKey().toString() ),
                        first( a, "name" ), first( a, "description" ), first( a, "queryText" ),
                        first( a, "category" ), first( a, "polarity" ), first( a, "severity" ),
                        first( a, "remediation" ), first( a, "expectedResult" ),
                        a.getOrDefault( "keyword", List.of() ), a.getOrDefault( "parameter", List.of() ) ) );
            }
            return Collections.unmodifiableList( defs );
        }
        catch ( final RuntimeException re )
        {
            throw re;
        }
        catch ( final Exception ex )
        {
            throw new RuntimeException( "could not load the query catalog '" + resource + "': " + ex.getMessage(), ex );
        }
    }

    private static String first ( final Map<String, List<String>> a, final String key )
    {
        final List<String> v = a.get( key );
        return v == null || v.isEmpty() ? null : v.get( 0 );
    }
}
