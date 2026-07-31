package io.quarkiverse.banner.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkiverse.banner.runtime.BannerFont;

class BannerRendererTest {

    @Test
    void rendersWithStandardFont() throws IOException {
        String banner = BannerRenderer.render(BannerFont.STANDARD, "Hi");

        assertFalse(banner.isBlank(), "banner should not be blank");
        assertTrue(banner.lines().count() > 1, "a FIGlet banner spans multiple lines");
    }

    @Test
    void everyBundledFontRenders() throws IOException {
        for (BannerFont font : BannerFont.values()) {
            String banner = BannerRenderer.render(font, "Ag");
            assertFalse(banner.isBlank(), () -> "font produced a blank banner: " + font.fileName());
        }
    }
}
