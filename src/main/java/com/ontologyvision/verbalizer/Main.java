// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

import java.awt.Desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Standalone command-line entry point for the SBVR verbalizer — runs the same report engine used inside VOM
 * against any OWL ontology file, with no MagicDraw/MSOSA dependency. This is what makes the capability usable
 * from Cameo Concept Modeler (File &gt; Export Model to OWL, then run this on the exported {@code .owl}),
 * Protégé, TopBraid, or any other OWL tool.
 *
 * <p>Usage:
 * <pre>
 *   java -jar sbvr-&lt;ver&gt;-cli.jar &lt;input.owl|.ttl|.rdf&gt; [output.html]
 *        [--no-verbalization] [--no-model] [--no-owl] [--no-rdf]
 *        [--color full|mono|plain] [--no-rollover] [--title T] [--open]
 * </pre>
 * With no {@code output.html}, writes {@code &lt;input-basename&gt;-sbvr.html} next to the input.
 *
 * <p>This needs the OWL API on the runtime classpath, so it is meant to run from the self-contained jar
 * produced by {@code ./gradlew :sbvr:cliJar} (the slim {@code sbvr.jar} that ships inside MSOSA has the OWL
 * API as {@code compileOnly} and cannot run standalone).
 */
public final class Main
{
    private Main () { }

    public static void main ( final String[] args ) throws Exception
    {
        if ( args.length < 1 || args[0].equals( "-h" ) || args[0].equals( "--help" ) )
        {
            System.err.println(
                    "usage: sbvr <input.owl|.ttl|.rdf> [output.html]\n"
                  + "            [--manchester | --ose | --rosetta]   (default: SBVR Structured English;\n"
                  + "                                                 --rosetta = SBVR | OSE | Manchester side-by-side)\n"
                  + "            [--no-verbalization] [--no-model] [--no-owl] [--no-rdf]\n"
                  + "            [--color full|mono|plain] [--no-rollover] [--title T] [--open]\n"
                  + "            [--validate]   (run a reasoner: consistency + unsatisfiable classes)" );
            System.exit( args.length < 1 ? 2 : 0 );
            return;
        }

        final File in = new File( args[0] );
        File out = null;
        String title = in.getName();
        boolean open = false;
        boolean validate = false;
        final VerbalizerOptions.Builder b = VerbalizerOptions.builder();

        for ( int i = 1; i < args.length; i++ )
        {
            final String a = args[i];
            switch ( a )
            {
                case "--no-verbalization": b.includeVerbalization( false ); break;
                case "--no-model":         b.includeModelGlossary( false ); break;
                case "--no-owl":           b.includeOwlGlossary( false );   break;
                case "--no-rdf":           b.includeRdfGlossary( false );   break;
                case "--no-rollover":      b.rollover( false );             break;
                case "--manchester":       b.formats( VerbalizerOptions.Format.MANCHESTER ); break;
                case "--ose":              b.formats( VerbalizerOptions.Format.OSE ); break;
                case "--rosetta":          b.formats( VerbalizerOptions.Format.SBVR, VerbalizerOptions.Format.OSE, VerbalizerOptions.Format.MANCHESTER ); break;
                case "--open":             open = true;                     break;
                case "--validate":         validate = true;                 break;
                case "--color":            b.colorLevel( VerbalizerOptions.ColorLevel.valueOf( args[++i].toUpperCase() ) ); break;
                case "--title":            title = args[++i];               break;
                default:
                    if ( a.startsWith( "-" ) ) { System.err.println( "unknown option: " + a ); System.exit( 2 ); return; }
                    out = new File( a );
            }
        }

        if ( !in.isFile() )
        {
            System.err.println( "input ontology not found: " + in.getAbsolutePath() );
            System.exit( 2 );
            return;
        }

        // Tolerate unresolved imports (same posture as the unit tests), so a self-contained file still verbalizes.
        final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        // Resolve imports from sibling files or a catalog-v001.xml in the same folder, so multi-file ontologies
        // (e.g. the fetched IOF Core + BFO) verbalize WITH their superclasses; anything still missing is tolerated.
        final File dir = in.getAbsoluteFile().getParentFile();
        if ( dir != null && dir.isDirectory() )
        {
            try { m.getIRIMappers().add( new AutoIRIMapper( dir, false ) ); }
            catch ( final Throwable ignore ) { /* best-effort sibling/catalog import resolution */ }
        }
        final OWLOntologyLoaderConfiguration cfg = new OWLOntologyLoaderConfiguration()
                .setMissingImportHandlingStrategy( MissingImportHandlingStrategy.SILENT );
        final OWLOntology ont = m.loadOntologyFromOntologyDocument( new FileDocumentSource( in ), cfg );

        final String html = new OntologyVerbalizer().verbalizeOntology( ont, title, b.build() );

        if ( out == null )
        {
            final String base = in.getName().replaceFirst( "\\.[^.]+$", "" );
            out = new File( in.getAbsoluteFile().getParentFile(), base + "-sbvr.html" );
        }
        Files.write( out.toPath(), html.getBytes( StandardCharsets.UTF_8 ) );
        System.out.println( "Wrote " + out.getAbsolutePath() );

        if ( open )
        {
            try
            {
                if ( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported( Desktop.Action.BROWSE ) )
                {
                    Desktop.getDesktop().browse( out.toURI() );
                }
            }
            catch ( final Throwable t )   // HeadlessException / no browser — the file is already written
            {
                System.err.println( "(could not open a browser: " + t.getMessage() + ")" );
            }
        }

        if ( validate ) runValidation( ont );
    }

    /**
     * Optional reasoner-based validation (the "REASON &amp; VALIDATE" pillar). The reasoner (ELK) is loaded
     * reflectively so the slim library keeps no hard reasoner dependency — the CLI jar bundles ELK, so this
     * works out of the box from {@code ontology-to-english-<ver>-cli.jar} but degrades gracefully elsewhere.
     */
    private static void runValidation ( final OWLOntology ont )
    {
        try
        {
            final org.semanticweb.owlapi.reasoner.OWLReasonerFactory rf =
                    (org.semanticweb.owlapi.reasoner.OWLReasonerFactory)
                            Class.forName( "org.semanticweb.elk.owlapi.ElkReasonerFactory" )
                                    .getDeclaredConstructor().newInstance();
            final Validator.Result r = new Validator().validate( ont, rf );
            if ( !r.consistent )
            {
                System.out.println( "VALIDATION: INCONSISTENT — a reasoner found a contradiction in the ontology." );
            }
            else if ( r.unsatisfiable.isEmpty() )
            {
                System.out.println( "VALIDATION: consistent; no unsatisfiable classes." );
            }
            else
            {
                System.out.println( "VALIDATION: consistent, but " + r.unsatisfiable.size()
                        + " unsatisfiable class(es) — each can never have an instance:" );
                for ( final org.semanticweb.owlapi.model.OWLClass c : r.unsatisfiable )
                {
                    System.out.println( "  - " + c.getIRI() );
                }
            }
        }
        catch ( final ClassNotFoundException e )
        {
            System.err.println( "--validate needs a reasoner on the classpath. The CLI jar bundles ELK; "
                    + "if you run the slim library jar, add io.github.liveontologies:elk-owlapi." );
        }
        catch ( final Throwable t )
        {
            System.err.println( "(validation failed: " + t.getMessage() + ")" );
        }
    }
}
