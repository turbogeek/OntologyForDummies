// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The static OWL &amp; RDF reference glossaries — the meanings of the OWL/RDF constructs an ontology uses —
 * loaded from bundled TSV resources ({@code owl-glossary.tsv}, {@code rdf-glossary.tsv}). This is the single
 * source shared by the report's OWL/RDF glossary sections and the in-app help glossary pages, so the two can
 * never drift. Each entry is {@code term} + plain-English {@code meaning} + an {@code sbvrReading} example.
 *
 * <p>The definitions were authored against the W3C OWL 2 / RDF specs and adversarially verified for accuracy.
 * Editing a {@code .tsv} updates the report and the help page together — no recompile of definitions needed.
 */
public final class GlossaryData
{
    /** One reference-glossary entry: an OWL/RDF construct term, its meaning, an SBVR reading example, and the
     *  equivalent OWL 2 Manchester-syntax form (the thesaurus column; {@code "—"} when there is no keyword). */
    public static final class Entry
    {
        public final String term;
        public final String meaning;
        public final String sbvrReading;
        public final String manchester;

        Entry ( final String term, final String meaning, final String sbvrReading, final String manchester )
        {
            this.term = term;
            this.meaning = meaning;
            this.sbvrReading = sbvrReading;
            this.manchester = manchester;
        }
    }

    private static List<Entry> owl;
    private static List<Entry> rdf;

    private GlossaryData () { }

    /** @return the OWL 2 construct glossary (cached). */
    public static synchronized List<Entry> owl () { if ( owl == null ) { owl = load( "owl-glossary.tsv" ); } return owl; }

    /** @return the RDF / RDFS construct glossary (cached). */
    public static synchronized List<Entry> rdf () { if ( rdf == null ) { rdf = load( "rdf-glossary.tsv" ); } return rdf; }

    private static List<Entry> load ( final String resource )
    {
        final List<Entry> out = new ArrayList<>();
        try ( InputStream in = GlossaryData.class.getResourceAsStream( resource ) )
        {
            if ( in == null ) { return Collections.emptyList(); }
            final BufferedReader r = new BufferedReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) );
            String line;
            while ( ( line = r.readLine() ) != null )
            {
                if ( line.isEmpty() || line.charAt( 0 ) == '#' ) { continue; }
                final String[] c = line.split( "\t", -1 );
                if ( c.length < 2 || c[0].trim().isEmpty() ) { continue; }
                out.add( new Entry( c[0].trim(), c[1].trim(), c.length > 2 ? c[2].trim() : "",
                                    c.length > 3 ? c[3].trim() : "—" ) );
            }
        }
        catch ( final Exception e )
        {
            /* return whatever parsed; a missing/garbled resource degrades to an empty glossary */
        }
        return Collections.unmodifiableList( out );
    }
}
