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
    public void referenceGlossaryFiltersToUsedConstructsWithThesaurus () throws Exception
    {
        final OWLOntology ont = load( "/pizza.owl" );
        final String html = new OntologyVerbalizer().verbalizeOntology( ont, "Pizza" );
        // Notation thesaurus: a Manchester column sits beside the SBVR reading in the reference glossary.
        assertTrue( "Manchester column header", html.contains( "<th>Manchester</th>" ) );
        assertTrue( "Manchester form for subClassOf", html.contains( "SubClassOf:" ) );
        // Filtered by default: the constructs pizza uses are shown, the rest are hidden extras behind a toggle.
        assertTrue( "Show-all toggle present", html.contains( "id=\"t-allconstructs\"" ) );
        assertTrue( "unused constructs hidden as extras", html.contains( "class=\"gloss-extra\"" ) );
        assertTrue( "a used construct (someValuesFrom) is listed", html.contains( "owl:someValuesFrom" ) );
        // A construct pizza does NOT use (irreflexive) must be an extra row, not a plain visible one.
        assertTrue( "unused construct is an extra row",
                    html.contains( "class=\"gloss-extra\"><td class=\"gloss-term\">owl:IrreflexiveProperty" ) );
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
        if ( Boolean.getBoolean( "vom.regenGolden" ) )
        {   // deliberate, reviewed change: rewrite the golden, then re-run without the flag to confirm it matches
            java.nio.file.Files.write( java.nio.file.Paths.get( "src/test/resources/expected-test-sbvr.html" ),
                                       html.getBytes( StandardCharsets.UTF_8 ) );
            return;
        }
        final String expected;
        try ( InputStream in = getClass().getResourceAsStream( "/expected-test-sbvr.html" ) )
        {
            assertNotNull( "missing golden /expected-test-sbvr.html", in );
            expected = new String( in.readAllBytes(), StandardCharsets.UTF_8 );
        }
        assertEquals( "SBVR output drifted — regenerate the golden only if the change is intentional.",
                      expected, html );
    }

    @Test
    public void rendersManchesterFormat () throws Exception
    {
        final OWLOntology ont = load( "/pizza.owl" );
        final String html = new OntologyVerbalizer().verbalizeOntology( ont, "Pizza",
                VerbalizerOptions.builder().formats( VerbalizerOptions.Format.MANCHESTER ).build() );
        assertTrue( "Manchester title", html.contains( "&mdash; Manchester Syntax" ) );
        assertTrue( "Manchester frame keyword (hover-glossed)", html.contains( ">SubClassOf</a>" ) );
        assertTrue( "monospace Manchester spans", html.contains( "class=\"manchester\"" ) );
        // short names come from the IRI fragment, NOT the Portuguese rdfs:label
        assertTrue( "IRI short names (not labels)", html.contains( "MozzarellaTopping" ) );
    }

    @Test
    public void rendersOseFormat () throws Exception
    {
        final OWLOntology ont = load( "/pizza.owl" );
        final String html = new OntologyVerbalizer().verbalizeOntology( ont, "Pizza",
                VerbalizerOptions.builder().formats( VerbalizerOptions.Format.OSE ).build() );
        assertTrue( "OSE title", html.contains( "&mdash; OWL Simplified English" ) );
        assertTrue( "OSE 'Every' (not SBVR 'Each')", html.contains( ">Every</a>" ) );
        assertTrue( "OSE 'some' (not SBVR 'at least one')", html.contains( ">some</a>" ) );
    }

    @Test
    public void rendersRosettaSideBySide () throws Exception
    {
        final OWLOntology ont = load( "/pizza.owl" );
        final String html = new OntologyVerbalizer().verbalizeOntology( ont, "Pizza",
                VerbalizerOptions.builder()
                        .formats( VerbalizerOptions.Format.SBVR, VerbalizerOptions.Format.MANCHESTER ).build() );
        assertTrue( "rosetta table", html.contains( "class=\"rosetta\"" ) );
        assertTrue( "SBVR column header", html.contains( "<th>SBVR Structured English</th>" ) );
        assertTrue( "Manchester column header", html.contains( "<th>Manchester Syntax</th>" ) );
        assertTrue( "SBVR existential reading present", html.contains( "at least one" ) );
        assertTrue( "Manchester reading present", html.contains( ">SubClassOf</a>" ) );
        // cross-column teaching: a Manchester "some" glosses to the same construct as SBVR "at least one"
        assertTrue( "Manchester keyword hover-glossed", html.contains( "gloss:owl%3AsomeValuesFrom" )
                    || html.contains( ">some</a>" ) );
    }
}
