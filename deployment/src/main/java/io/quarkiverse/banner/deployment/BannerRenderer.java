package io.quarkiverse.banner.deployment;

import java.io.IOException;
import java.io.InputStream;

import com.github.lalyos.jfiglet.FigletFont;

import io.quarkiverse.banner.runtime.BannerFont;

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
