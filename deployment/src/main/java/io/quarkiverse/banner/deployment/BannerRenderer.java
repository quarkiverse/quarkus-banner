package io.quarkiverse.banner.deployment;

import java.io.IOException;
import java.io.InputStream;

import com.github.lalyos.jfiglet.FigletFont;

import io.quarkiverse.banner.runtime.BannerFont;
import io.quarkus.builder.Version;

/**
 * Renders a piece of text into a FIGlet ASCII-art banner using jfiglet, using one of the fonts bundled with
 * this extension.
 */
final class BannerRenderer {

    /** Classpath location of the FIGlet fonts bundled with this extension. */
    static final String BUNDLED_FONTS_DIR = "/io/quarkiverse/banner/fonts/";

    private BannerRenderer() {
    }

    /**
     * Renders {@code text} using the given bundled {@code font}, optionally appending the right-aligned
     * {@code Powered by Quarkus <version>} tagline. This is the full banner as installed at start-up, and is
     * shared by the build-time banner generation and the Dev UI preview so both render identically.
     *
     * @param font the bundled font to use
     * @param text the text to render
     * @param powerBy whether to append the {@code Powered by Quarkus} tagline
     * @return the rendered banner
     * @throws IOException if the font resource cannot be read or the text cannot be rendered
     */
    static String renderBanner(BannerFont font, String text, boolean powerBy) throws IOException {
        String bannerText = render(font, text);
        if (!powerBy) {
            return bannerText;
        }

        StringBuilder banner = new StringBuilder(bannerText);
        int width = bannerText.lines().mapToInt(String::length).max().orElse(0);
        String poweredBy = "Powered by Quarkus " + Version.getVersion();
        int padding = Math.max(0, width - poweredBy.length());
        banner.append(" ".repeat(padding)).append(poweredBy).append('\n').append('\n');
        return banner.toString();
    }

    /**
     * Renders {@code text} using the given bundled {@code font}.
     *
     * @param font the bundled font to use
     * @param text the text to render
     * @return the rendered banner
     * @throws IOException if the font resource cannot be read or the text cannot be rendered
     */
    static String render(BannerFont font, String text) throws IOException {
        String resource = BUNDLED_FONTS_DIR + font.fileName() + ".flf";
        try (InputStream is = BannerRenderer.class.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IOException("Bundled font resource not found: " + resource);
            }
            return FigletFont.convertOneLine(is, text);
        }
    }
}
