package io.quarkiverse.banner.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BannerFontConverterTest {

    private final BannerFontConverter converter = new BannerFontConverter();

    @Test
    void resolvesFontCaseInsensitively() {
        assertEquals(BannerFont.STANDARD, converter.convert("standard"));
        assertEquals(BannerFont.STANDARD, converter.convert("STANDARD"));
        assertEquals(BannerFont.DOOM, converter.convert("Doom"));
    }

    @Test
    void resolvesFontsWithPunctuationOrDigitsInTheName() {
        assertEquals(BannerFont.SUB_ZERO, converter.convert("sub-zero"));
        assertEquals(BannerFont.F_3_D, converter.convert("3-d"));
        assertEquals(BannerFont.B1FF, converter.convert("b1ff"));
    }

    @Test
    void unknownFontIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("no-such-font"));
    }

    @Test
    void blankValueConvertsToNull() {
        assertEquals(null, converter.convert(""));
        assertEquals(null, converter.convert("   "));
    }
}
