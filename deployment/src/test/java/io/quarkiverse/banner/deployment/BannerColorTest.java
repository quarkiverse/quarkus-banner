package io.quarkiverse.banner.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import io.quarkiverse.banner.runtime.BannerColor;
import io.quarkiverse.banner.runtime.BannerFont;
import io.quarkiverse.banner.runtime.ResolvedColor;

class BannerColorTest {

    private static final String ESC = "\u001b";

    private static ResolvedColor color(String token) {
        return ResolvedColor.parse(token);
    }

    @Test
    void colorizeWithDefaultsIsANoOp() {
        String text = "AB";
        assertSame(text, BannerRenderer.colorize(text, ResolvedColor.DEFAULT, ResolvedColor.DEFAULT));
    }

    @Test
    void colorizeForeground() {
        assertEquals(ESC + "[31mAB" + ESC + "[0m", BannerRenderer.colorize("AB", color("red"), ResolvedColor.DEFAULT));
    }

    @Test
    void colorizeForegroundAndBackground() {
        assertEquals(ESC + "[93;44mAB" + ESC + "[0m", BannerRenderer.colorize("AB", color("bright-yellow"),
                color("blue")));
    }

    @Test
    void orangeUsesTruecolor() {
        assertEquals(ESC + "[38;2;255;165;0mAB" + ESC + "[0m",
                BannerRenderer.colorize("AB", color("orange"), ResolvedColor.DEFAULT));
    }

    @Test
    void hexColoursUseTruecolor() {
        assertEquals(ESC + "[38;2;255;136;0mAB" + ESC + "[0m",
                BannerRenderer.colorize("AB", color("#ff8800"), ResolvedColor.DEFAULT));
        // #f80 shorthand expands to #ff8800
        assertEquals(color("#ff8800"), color("#f80"));
        // hex as a background
        assertEquals(ESC + "[48;2;0;0;255mAB" + ESC + "[0m",
                BannerRenderer.colorize("AB", ResolvedColor.DEFAULT, color("#0000ff")));
    }

    @Test
    void colourResolution() {
        assertTrue(color("default").isDefault());
        assertTrue(color("").isDefault());
        assertTrue(color(null).isDefault());
        assertEquals("31", color("red").foreground());
        assertEquals("103", color("bright-yellow").background());
        assertNull(color("not-a-colour"));
        assertNull(color("#zzzz"));
    }

    @Test
    void namedPaletteCodes() {
        assertEquals("90", BannerColor.BRIGHT_BLACK.foreground());
        assertEquals("107", BannerColor.BRIGHT_WHITE.background());
        assertEquals("38;2;255;165;0", BannerColor.ORANGE.foreground());
    }

    @Test
    void plainVersionNeverContainsEscapeCodes() throws IOException {
        BannerRenderer.Rendered banner = BannerRenderer.renderBanner(BannerFont.STANDARD, "{red}Hi{blue}!",
                true, color("green"), color("blue"));
        assertFalse(banner.plain().contains(ESC), "plain banner must have no ANSI codes");
        assertFalse(banner.plain().isBlank());
    }

    @Test
    void inlineMarkersProduceMultipleColours() throws IOException {
        BannerRenderer.Rendered banner = BannerRenderer.renderBanner(BannerFont.STANDARD, "{red}Qu{cyan}arkus",
                false, ResolvedColor.DEFAULT, ResolvedColor.DEFAULT);
        assertTrue(banner.colored().contains(ESC + "[31m"), "expected red (31)");
        assertTrue(banner.colored().contains(ESC + "[36m"), "expected cyan (36)");
        assertFalse(banner.plain().contains(ESC));
    }

    @Test
    void inlineHexMarker() throws IOException {
        BannerRenderer.Rendered banner = BannerRenderer.renderBanner(BannerFont.STANDARD, "{#ff8800}Hi", false,
                ResolvedColor.DEFAULT, ResolvedColor.DEFAULT);
        assertTrue(banner.colored().contains(ESC + "[38;2;255;136;0m"), "expected the hex colour as truecolor");
    }

    @Test
    void unknownMarkerIsLeftInTheText() throws IOException {
        BannerRenderer.Rendered rendered = BannerRenderer.renderBanner(BannerFont.STANDARD, "{smiley}", false,
                ResolvedColor.DEFAULT, ResolvedColor.DEFAULT);
        assertFalse(rendered.plain().isBlank(), "unknown marker should still render as text");
    }

    @Test
    void noColourConfiguredYieldsIdenticalPlainAndColoured() throws IOException {
        BannerRenderer.Rendered banner = BannerRenderer.renderBanner(BannerFont.STANDARD, "Quarkus", true,
                ResolvedColor.DEFAULT, ResolvedColor.DEFAULT);
        assertEquals(banner.plain(), banner.colored(), "with no colour the two versions must match");
    }

    @Test
    void ownerMapAssignsEachInkedCellToItsCharacter() throws IOException {
        Figlet.RenderResult rendered;
        try (InputStream is = getClass().getResourceAsStream("/io/quarkiverse/banner/fonts/standard.flf")) {
            rendered = Figlet.renderTracked(is, "Hi");
        }
        boolean ownedByH = false;
        boolean ownedByI = false;
        boolean blank = false;
        int maxOwner = -1;
        for (int[] row : rendered.owner()) {
            for (int owner : row) {
                ownedByH |= owner == 0;
                ownedByI |= owner == 1;
                blank |= owner == -1;
                maxOwner = Math.max(maxOwner, owner);
            }
        }
        assertTrue(ownedByH && ownedByI, "both characters own some ink");
        assertTrue(blank, "blank cells stay unowned");
        assertTrue(maxOwner <= 1, "no cell is owned by a non-existent character");
    }

    @Test
    void perCellColouringNeverBleedsOrDropsText() throws IOException {
        BannerRenderer.Rendered banner = BannerRenderer.renderBanner(BannerFont.SLANT, "{red}Quar{blue}kus", false,
                ResolvedColor.DEFAULT, ResolvedColor.DEFAULT);
        String stripped = banner.colored().replaceAll(ESC + "\\[[0-9;]*m", "");
        assertEquals(banner.plain(), stripped, "colouring only inserts codes; the text is unchanged");
        assertTrue(banner.colored().contains(ESC + "[31m"), "expected red (31)");
        assertTrue(banner.colored().contains(ESC + "[34m"), "expected blue (34)");
    }
}
