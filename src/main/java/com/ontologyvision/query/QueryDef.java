// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.query;

import java.util.List;
import java.util.Map;

/**
 * One query: its SPARQL plus muggle-facing metadata (name, plain-English description, category, and — for
 * pattern queries — polarity/severity/remediation). Immutable. Used both for the bundled catalog entries
 * (loaded by {@link QueryCatalog}) and for a user's saved query.
 */
public final class QueryDef
{
    /** Stable id (the catalog entry's IRI fragment, e.g. {@code count-classes}). */
    public final String id;
    /** Short, muggle-facing title. */
    public final String name;
    /** What it finds, in plain English. */
    public final String description;
    /** The SPARQL text (self-contained, with its own PREFIX lines). */
    public final String sparql;
    /** {@code STATS} | {@code DISCOVERY} | {@code GOOD_PATTERN} | {@code BAD_PATTERN} | {@code USER}. */
    public final String category;
    /** For pattern queries: {@code GOOD} or {@code BAD} (else null). */
    public final String polarity;
    /** For bad patterns: {@code info} | {@code warning} | {@code error} (else null). */
    public final String severity;
    /** Plain-English fix advice for a bad-pattern hit (else null). */
    public final String remediation;
    /** A hint on reading the result, e.g. {@code "empty == healthy"} (else null). */
    public final String expectedResult;
    /** Search/filter tags. */
    public final List<String> keywords;
    /** {@code ${placeholder}} names this query expects (non-empty ⇒ a template query). */
    public final List<String> parameters;

    public QueryDef ( final String id, final String name, final String description, final String sparql,
                      final String category, final String polarity, final String severity,
                      final String remediation, final String expectedResult,
                      final List<String> keywords, final List<String> parameters )
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sparql = sparql;
        this.category = category;
        this.polarity = polarity;
        this.severity = severity;
        this.remediation = remediation;
        this.expectedResult = expectedResult;
        this.keywords = List.copyOf( keywords == null ? List.of() : keywords );
        this.parameters = List.copyOf( parameters == null ? List.of() : parameters );
    }

    /** A template query has one or more {@code ${...}} parameters to fill in before running. */
    public boolean isTemplate () { return !this.parameters.isEmpty(); }

    public boolean isBadPattern ()  { return "BAD".equalsIgnoreCase( this.polarity ); }

    public boolean isGoodPattern () { return "GOOD".equalsIgnoreCase( this.polarity ); }

    /**
     * Substitute the {@code ${name}} placeholders in the SPARQL with the supplied values.
     * @throws IllegalArgumentException if a declared parameter has no value supplied
     */
    public String expand ( final Map<String, String> values )
    {
        String out = this.sparql;
        for ( final String p : this.parameters )
        {
            final String v = values == null ? null : values.get( p );
            if ( v == null )
            {
                throw new IllegalArgumentException( "missing value for query parameter '" + p + "' in '" + this.name + "'" );
            }
            out = out.replace( "${" + p + "}", v );
        }
        return out;
    }

    @Override
    public String toString () { return this.name + " [" + this.category + "]"; }
}
