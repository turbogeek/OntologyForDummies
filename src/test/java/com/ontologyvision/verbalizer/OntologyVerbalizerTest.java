// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Headless tests for the dependency-clean verbalizer (OWL API only — no MagicDraw). The OWL fixtures are
 * bundled as test resources, so this module is self-contained / CI-runnable as a standalone library.
 */
public class OntologyVerbalizerTest
{
    private OWLOntology load (final String resource) throws Exception
    {
        final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        final OWLOntologyLoaderConfiguration cfg = new OWLOntologyLoaderConfiguration()
                .setMissingImportHandlingStrategy( MissingImportHandlingStrategy.SILENT );
        try ( InputStream in = getClass().getResourceAsStream( resource ) )
        {
            assertNotNull( "missing test fixture " + resource, in );
            return m.loadOntologyFromOntologyDocument( new StreamDocumentSource( in ), cfg );
        }
    }

    @Test
    public void verbalizesPizzaToSbvrEnglish () throws Exception
    {
        final OWLOntology ont = load( "/pizza.owl" );
        final String html = new OntologyVerbalizer().verbalizeOntology( ont, "Pizza" );

        assertTrue( "complete HTML document", html.startsWith( "<!DOCTYPE html>" ) );
        assertTrue( "classes section present", html.contains( "Concepts (Classes)" ) );
        assertTrue( "existential restriction reading", html.contains( "at least one" ) );
        assertTrue( "universal restriction reading", html.contains( "only" ) );
        assertTrue( "SBVR legend present", html.contains( "SBVR Structured English" ) );
        assertTrue( "role-tagged spans (term + verb)",
                    html.contains( "class=\"term\"" ) && html.contains( "class=\"verb\"" ) );
    }

    @Test
    public void verbalizesSmallOntology () throws Exception
    {
        final OWLOntology ont = load( "/test.owl" );
        final String html = new OntologyVerbalizer().verbalizeOntology( ont, "test" );
        assertTrue( html.contains( "<!DOCTYPE html>" ) );
        assertTrue( html.contains( "Concepts (Classes)" ) );
    }

    /**
     * Golden gate: the SBVR output for test.owl must stay byte-identical. This protects the default SBVR
     * format against regressions as more formats (Manchester, OSE, ACE) are added to the multi-format engine.
     * Regenerate {@code expected-test-sbvr.html} only on a deliberate, reviewed change to the SBVR reading.
     */
    @Test
    public void sbvrOutputIsByteIdentical () throws Exception
    {
        final OWLOntology ont = load( "/test.owl" );
        final String html = new OntologyVerbalizer().verbalizeOntology( ont, "test" );
        final String expected;
        try ( InputStream in = getClass().getResourceAsStream( "/expected-test-sbvr.html" ) )
        {
            assertNotNull( "missing golden /expected-test-sbvr.html", in );
            expected = new String( in.readAllBytes(), StandardCharsets.UTF_8 );
        }
        assertEquals( "SBVR output drifted — regenerate the golden only if the change is intentional.",
                      expected, html );
    }
}
