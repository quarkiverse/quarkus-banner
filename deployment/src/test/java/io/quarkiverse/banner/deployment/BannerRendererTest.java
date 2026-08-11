package io.quarkiverse.banner.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

    /**
     * Verifies the clean-room {@link Figlet} renderer reproduces the canonical FIGlet output byte-for-byte.
     * The fixtures in {@code banner-goldens.txt} were captured from the reference {@code figlet} program and
     * guard against regressions in the parser, fitting and smushing logic.
     */
    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("goldens")
    void matchesReferenceFiglet(String font, String text, String expected) throws IOException {
        String resource = "/io/quarkiverse/banner/fonts/" + font + ".flf";
        try (InputStream is = BannerRendererTest.class.getResourceAsStream(resource)) {
            assertNotNull(is, () -> "missing bundled font: " + resource);
            assertEquals(expected, Figlet.convertOneLine(is, text),
                    () -> "clean-room renderer diverged from reference figlet for font '" + font + "'");
        }
    }

    static List<Object[]> goldens() throws IOException {
        List<Object[]> cases = new ArrayList<>();
        try (InputStream is = BannerRendererTest.class.getResourceAsStream("/banner-goldens.txt");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(assertResource(is), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("@@@") || line.equals("@@@END@@@")) {
                    continue;
                }
                // Header: @@@<font>@@@<text>@@@
                String[] parts = line.split("@@@");
                String font = parts[1];
                String text = parts[2];
                StringBuilder expected = new StringBuilder();
                String body;
                while ((body = reader.readLine()) != null && !body.equals("@@@END@@@")) {
                    expected.append(body).append('\n');
                }
                cases.add(new Object[] { font, text, expected.toString() });
            }
        }
        assertFalse(cases.isEmpty(), "no golden fixtures loaded");
        return cases;
    }

    private static InputStream assertResource(InputStream is) {
        assertNotNull(is, "missing test resource: banner-goldens.txt");
        return is;
    }
}
