#!/usr/bin/env groovy
// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
//
// sbvr.groovy — generate an SBVR Structured-English HTML "translation" of an OWL ontology. This is the Groovy
// equivalent of com.ontologyvision.verbalizer.Main: same arguments, same output.
//
// It assumes the required jars are on the classpath — the Ontology-to-English jar plus the OWL API and its
// runtime closure. The easiest way is the self-contained CLI jar (OWL API bundled):
//
//   ./gradlew cliJar
//   groovy -cp build/libs/ontology-to-english-<ver>-cli.jar scripts/sbvr.groovy pizza.owl --open
//
//   # no Groovy on PATH? run it through java:
//   java -cp "groovy-all.jar:build/libs/ontology-to-english-<ver>-cli.jar" \
//        groovy.ui.GroovyMain scripts/sbvr.groovy pizza.owl

import com.ontologyvision.verbalizer.OntologyVerbalizer
import com.ontologyvision.verbalizer.VerbalizerOptions
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration

if ( args.length < 1 || args[0] in ['-h', '--help'] ) {
    System.err.println '''usage: sbvr.groovy <input.owl|.ttl|.rdf> [output.html]
            [--no-verbalization] [--no-model] [--no-owl] [--no-rdf]
            [--color full|mono|plain] [--no-rollover] [--title T] [--open]'''
    return
}

File input = new File( args[0] )
File output = null
String title = input.name
boolean open = false
VerbalizerOptions.Builder b = VerbalizerOptions.builder()

for ( int i = 1; i < args.length; i++ ) {
    switch ( args[i] ) {
        case '--no-verbalization': b.includeVerbalization( false ); break
        case '--no-model':         b.includeModelGlossary( false ); break
        case '--no-owl':           b.includeOwlGlossary( false );   break
        case '--no-rdf':           b.includeRdfGlossary( false );   break
        case '--no-rollover':      b.rollover( false );             break
        case '--open':             open = true;                     break
        case '--color':            b.colorLevel( VerbalizerOptions.ColorLevel.valueOf( args[++i].toUpperCase() ) ); break
        case '--title':            title = args[++i];               break
        default:
            if ( args[i].startsWith( '-' ) ) { System.err.println "unknown option: ${args[i]}"; return }
            output = new File( args[i] )
    }
}

if ( !input.isFile() ) { System.err.println "input ontology not found: ${input.absolutePath}"; return }

// Tolerate unresolved imports so a self-contained file still verbalizes.
def mgr = OWLManager.createOWLOntologyManager()
def cfg = new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy( MissingImportHandlingStrategy.SILENT )
def ont = mgr.loadOntologyFromOntologyDocument( new FileDocumentSource( input ), cfg )

String html = new OntologyVerbalizer().verbalizeOntology( ont, title, b.build() )

if ( output == null ) {
    String base = input.name.replaceFirst( /\.[^.]+$/, '' )
    output = new File( input.absoluteFile.parentFile, base + '-sbvr.html' )
}
output.text = html
println "Wrote ${output.absolutePath}"

if ( open ) {
    try {
        if ( java.awt.Desktop.isDesktopSupported() &&
             java.awt.Desktop.desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {
            java.awt.Desktop.desktop.browse( output.toURI() )
        }
    } catch ( Throwable t ) {
        System.err.println "(could not open a browser: ${t.message})"
    }
}
