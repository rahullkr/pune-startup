package com.punestartup.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @Test
    void normalizeNameCollapsesWhitespaceAndLowercases() {
        assertThat(TextNormalizer.normalizeName("  Foo   BAR ")).isEqualTo("foo bar");
    }

    @Test
    void normalizeNameReturnsNullForNull() {
        assertThat(TextNormalizer.normalizeName(null)).isNull();
    }

    @Test
    void slugifyReplacesInvalidCharacters() {
        assertThat(TextNormalizer.slugify("Foo Bar & Baz!")).isEqualTo("foo-bar-baz");
    }

    @Test
    void slugifyTrimsLeadingAndTrailingHyphens() {
        assertThat(TextNormalizer.slugify("--Foo Bar--")).isEqualTo("foo-bar");
    }

    @Test
    void slugifyReturnsNullWhenNothingRemains() {
        assertThat(TextNormalizer.slugify("!!!")).isNull();
    }

    @Test
    void escapeLikeEscapesWildcards() {
        assertThat(TextNormalizer.escapeLike("100%_done")).isEqualTo("100\\%\\_done");
    }

    @Test
    void likePatternWrapsValue() {
        assertThat(TextNormalizer.likePattern("foo")).isEqualTo("%foo%");
    }
}
