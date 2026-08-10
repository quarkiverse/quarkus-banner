package io.quarkiverse.banner.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class BannerColorTest {

    private static final String ESC = "";

    @Test
    void defaultColoursLeaveTheBannerUntouched() {
        String banner = "AB\n";
        assertSame(banner, BannerRecorder.colorize(banner, BannerColor.DEFAULT, BannerColor.DEFAULT));
    }

    @Test
    void foregroundOnlyWrapsEachLine() {
        String result = BannerRecorder.colorize("AB\nCD\n", BannerColor.RED, BannerColor.DEFAULT);
        assertEquals(ESC + "[31mAB" + ESC + "[0m\n" + ESC + "[31mCD" + ESC + "[0m\n", result);
    }

    @Test
    void backgroundOnlyUsesBackgroundCode() {
        String result = BannerRecorder.colorize("AB\n", BannerColor.DEFAULT, BannerColor.BLUE);
        assertEquals(ESC + "[44mAB" + ESC + "[0m\n", result);
    }

    @Test
    void foregroundAndBackgroundAreCombined() {
        String result = BannerRecorder.colorize("AB\n", BannerColor.BRIGHT_YELLOW, BannerColor.BLUE);
        assertEquals(ESC + "[93;44mAB" + ESC + "[0m\n", result);
    }

    @Test
    void trailingBlankLineFromFinalNewlineIsNotColoured() {
        // A single trailing newline must not produce a stray coloured empty line.
        String result = BannerRecorder.colorize("AB\n", BannerColor.GREEN, BannerColor.DEFAULT);
        assertEquals(ESC + "[32mAB" + ESC + "[0m\n", result);
    }

    @Test
    void brightColourCodes() {
        assertEquals(90, BannerColor.BRIGHT_BLACK.foregroundCode());
        assertEquals(107, BannerColor.BRIGHT_WHITE.backgroundCode());
        assertEquals(-1, BannerColor.DEFAULT.foregroundCode());
    }
}
