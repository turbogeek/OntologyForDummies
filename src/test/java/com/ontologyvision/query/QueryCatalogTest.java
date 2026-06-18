// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.query;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Verifies the bundled query/pattern catalog loads cleanly and the metadata + template expansion are sane. */
public class QueryCatalogTest
{
    @Test
    public void loadsWellFormedEntries ()
    {
        final List<QueryDef> all = QueryCatalog.all();
        assertTrue( "catalog should have a useful number of queries", all.size() >= 12 );
        for ( final QueryDef q : all )
        {
            assertNotNull( "id", q.id );
            assertNotNull( "name (" + q.id + ")", q.name );
            assertNotNull( "category (" + q.id + ")", q.category );
            assertNotNull( "sparql (" + q.id + ")", q.sparql );
            assertFalse( "sparql non-empty (" + q.id + ")", q.sparql.trim().isEmpty() );
        }
    }

    @Test
    public void categorisedCorrectly ()
    {
        assertFalse( "has STATS", QueryCatalog.stats().isEmpty() );
        assertFalse( "has DISCOVERY", QueryCatalog.discovery().isEmpty() );
        assertFalse( "has bad patterns", QueryCatalog.badPatterns().isEmpty() );
        assertFalse( "has good patterns", QueryCatalog.goodPatterns().isEmpty() );
        for ( final QueryDef q : QueryCatalog.badPatterns() )
        {
            assertTrue( q.isBadPattern() );
            assertEquals( "BAD_PATTERN", q.category );
            assertNotNull( "a bad-pattern query should carry remediation advice: " + q.id, q.remediation );
        }
    }

    @Test
    public void templateExpansionWorks ()
    {
        final QueryDef subOf = find( "subclasses-of" );
        assertTrue( "subclasses-of is a template", subOf.isTemplate() );
        assertTrue( "declares classIRI", subOf.parameters.contains( "classIRI" ) );
        final String expanded = subOf.expand( Map.of( "classIRI", "http://example.org/Pizza" ) );
        assertTrue( "placeholder filled", expanded.contains( "http://example.org/Pizza" ) );
        assertFalse( "no leftover ${...} placeholder", expanded.contains( "${" ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void templateMissingParamThrows ()
    {
        find( "subclasses-of" ).expand( Map.of() );
    }

    private static QueryDef find ( final String id )
    {
        for ( final QueryDef q : QueryCatalog.all() )
        {
            if ( id.equals( q.id ) ) { return q; }
        }
        throw new AssertionError( "query not found in catalog: " + id );
    }
}
