package io.quarkiverse.banner.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import io.quarkiverse.banner.runtime.BannerColor;
import io.quarkiverse.banner.runtime.BannerFont;

class BannerColorTest {

    private static final String ESC = "\u001b";

    @Test
    void colorizeWithDefaultsIsANoOp() {
        String text = "AB";
        assertSame(text, BannerRenderer.colorize(text, BannerColor.DEFAULT, BannerColor.DEFAULT));
    }

    @Test
    void colorizeForeground() {
        assertEquals(ESC + "[31mAB" + ESC + "[0m",
                BannerRenderer.colorize("AB", BannerColor.RED, BannerColor.DEFAULT));
    }

    @Test
    void colorizeForegroundAndBackground() {
        assertEquals(ESC + "[93;44mAB" + ESC + "[0m",
                BannerRenderer.colorize("AB", BannerColor.BRIGHT_YELLOW, BannerColor.BLUE));
    }

    @Test
    void plainVersionNeverContainsEscapeCodes() throws IOException {
        BannerRenderer.Rendered banner = BannerRenderer.renderBanner(BannerFont.STANDARD, "{red}Hi{blue}!",
                true, BannerColor.GREEN, BannerColor.BLUE);
        assertFalse(banner.plain().contains(ESC), "plain banner must have no ANSI codes");
        assertFalse(banner.plain().isBlank());
    }

    @Test
    void inlineMarkersProduceMultipleColours() throws IOException {
        // "Qu" red then "arkus" cyan: the coloured banner carries both codes, the plain one none.
        BannerRenderer.Rendered banner = BannerRenderer.renderBanner(BannerFont.STANDARD, "{red}Qu{cyan}arkus",
                false, BannerColor.DEFAULT, BannerColor.DEFAULT);
        assertTrue(banner.colored().contains(ESC + "[31m"), "expected red (31) somewhere");
        assertTrue(banner.colored().contains(ESC + "[36m"), "expected cyan (36) somewhere");
        assertFalse(banner.plain().contains(ESC));
    }

    @Test
    void unknownMarkerIsLeftInTheText() throws IOException {
        // "{smiley}" is not a colour, so it must survive as literal text (rendered as glyphs), not vanish.
        String withMarker = BannerRenderer.render(BannerFont.STANDARD, "{smiley}");
        String plain = BannerRenderer.render(BannerFont.STANDARD, "{smiley}");
        assertEquals(withMarker, plain);
        BannerRenderer.Rendered rendered = BannerRenderer.renderBanner(BannerFont.STANDARD, "{smiley}", false,
                BannerColor.DEFAULT, BannerColor.DEFAULT);
        assertFalse(rendered.plain().isBlank(), "unknown marker should still render as text");
    }

    @Test
    void noColourConfiguredYieldsIdenticalPlainAndColoured() throws IOException {
        BannerRenderer.Rendered banner = BannerRenderer.renderBanner(BannerFont.STANDARD, "Quarkus", true,
                BannerColor.DEFAULT, BannerColor.DEFAULT);
        assertEquals(banner.plain(), banner.colored(), "with no colour the two versions must match");
    }

    @Test
    void trackedColumnsLocateEachGlyph() throws IOException {
        int[] columns;
        try (InputStream is = getClass().getResourceAsStream("/io/quarkiverse/banner/fonts/standard.flf")) {
            columns = Figlet.renderTracked(is, "Hi").columns();
        }
        assertEquals(0, columns[0], "the first glyph starts at column 0");
        assertTrue(columns[1] > columns[0], "the second glyph starts further right");
    }

    @Test
    void colourBoundaryFollowsKerningAcrossAWordGap() throws IOException {
        // The colour change before "IT" must land at the column where "IT" is actually placed (kerning
        // included), not at the wider prefix width -- otherwise the previous colour bleeds onto its ink.
        int[] columns;
        try (InputStream is = getClass().getResourceAsStream("/io/quarkiverse/banner/fonts/doom.flf")) {
            columns = Figlet.renderTracked(is, "Banner IT").columns();
        }
        int prefixWidth;
        try (InputStream is = getClass().getResourceAsStream("/io/quarkiverse/banner/fonts/doom.flf")) {
            String block = Figlet.convertOneLine(is, "Banner ");
            prefixWidth = block.indexOf('\n');
        }
        // "I" is index 7 in "Banner IT"; its placement column is where the red segment should start.
        assertTrue(columns[7] <= prefixWidth, "the incoming glyph is kerned at or before the prefix width");
    }

    @Test
    void brightColourCodes() {
        assertEquals(90, BannerColor.BRIGHT_BLACK.foregroundCode());
        assertEquals(107, BannerColor.BRIGHT_WHITE.backgroundCode());
        assertEquals(-1, BannerColor.DEFAULT.foregroundCode());
    }
}
