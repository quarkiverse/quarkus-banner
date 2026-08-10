package io.quarkiverse.banner.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

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
    void brightColourCodes() {
        assertEquals(90, BannerColor.BRIGHT_BLACK.foregroundCode());
        assertEquals(107, BannerColor.BRIGHT_WHITE.backgroundCode());
        assertEquals(-1, BannerColor.DEFAULT.foregroundCode());
    }
}
