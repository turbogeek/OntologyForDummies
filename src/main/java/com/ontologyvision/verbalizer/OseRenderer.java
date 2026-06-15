// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Daniel Brookshier
package com.ontologyvision.verbalizer;

/**
 * <b>OWL Simplified English</b> (Power, 2012) — a leaner controlled-English reading. As the research found,
 * OSE is so close to SBVR Structured English that it is essentially the same axiom traversal with a couple of
 * keyword swaps: <code>Every</code> for <code>Each</code> and <code>some</code> for <code>at least one</code>.
 * So it is a thin subclass of {@link SbvrRenderer} overriding only those phrasing hooks (it keeps the same
 * role styling and hover-gloss). A fuller OSE profile (articles, hyphenated verbs) can extend this later.
 */
final class OseRenderer extends SbvrRenderer
{
    @Override public String formatId ()    { return "ose"; }
    @Override public String displayName () { return "OWL Simplified English"; }

    @Override protected String each ()     { return "Every"; }
    @Override protected String someWord () { return "some"; }
}
