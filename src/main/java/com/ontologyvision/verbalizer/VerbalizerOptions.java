// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

/**
 * Immutable options controlling an {@link OntologyVerbalizer} report: which sections it emits (the SBVR
 * verbalization, the model glossary, and the OWL / RDF reference glossaries), its initial SBVR color level,
 * and whether interactive rollover data is emitted. Build via {@link #builder()}; {@link #DEFAULT} is
 * everything on, full color.
 *
 * <p>Two layers cooperate: these <i>generation-time</i> flags decide what content is written into the HTML
 * (a section turned off here is simply absent); the report's embedded view-time toggles then decide what is
 * <i>visible</i> in an already-saved file.
 */
public final class VerbalizerOptions
{
    /** SBVR color rendering level. FULL = the four-role colors; MONO = drop color but keep the underline/
     *  italic/bold typographic distinctions; PLAIN = no color and no type treatment. */
    public enum ColorLevel { FULL, MONO, PLAIN }

    public final boolean includeVerbalization;
    public final boolean includeModelGlossary;
    public final boolean includeOwlGlossary;
    public final boolean includeRdfGlossary;
    /** Emit {@code data-gloss} attributes + the bottom hover panel + the inline rollover script. */
    public final boolean rollover;
    public final ColorLevel colorLevel;

    /** Everything on, full color, rollover on. */
    public static final VerbalizerOptions DEFAULT = builder().build();

    private VerbalizerOptions ( final Builder b )
    {
        this.includeVerbalization = b.includeVerbalization;
        this.includeModelGlossary = b.includeModelGlossary;
        this.includeOwlGlossary   = b.includeOwlGlossary;
        this.includeRdfGlossary   = b.includeRdfGlossary;
        this.rollover             = b.rollover;
        this.colorLevel           = b.colorLevel;
    }

    public static Builder builder () { return new Builder(); }

    /** Mutable builder for {@link VerbalizerOptions}. */
    public static final class Builder
    {
        private boolean includeVerbalization = true;
        private boolean includeModelGlossary = true;
        private boolean includeOwlGlossary   = true;
        private boolean includeRdfGlossary   = true;
        private boolean rollover             = true;
        private ColorLevel colorLevel        = ColorLevel.FULL;

        public Builder includeVerbalization ( final boolean v ) { this.includeVerbalization = v; return this; }
        public Builder includeModelGlossary ( final boolean v ) { this.includeModelGlossary = v; return this; }
        public Builder includeOwlGlossary   ( final boolean v ) { this.includeOwlGlossary   = v; return this; }
        public Builder includeRdfGlossary   ( final boolean v ) { this.includeRdfGlossary   = v; return this; }
        public Builder rollover             ( final boolean v ) { this.rollover             = v; return this; }
        public Builder colorLevel           ( final ColorLevel v ) { this.colorLevel = ( v == null ? ColorLevel.FULL : v ); return this; }

        public VerbalizerOptions build () { return new VerbalizerOptions( this ); }
    }
}
