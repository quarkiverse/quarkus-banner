package io.quarkiverse.banner.runtime;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Converts a configuration value to a {@link BannerFont}, matching the bundled font's file name
 * case-insensitively. An unknown font produces a clear configuration error at build time.
 */
public class BannerFontConverter implements Converter<BannerFont> {

    @Override
    public BannerFont convert(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        BannerFont font = BannerFont.fromName(value);
        if (font == null) {
            throw new IllegalArgumentException(
                    "Unknown banner font '" + value + "'. It must be one of the fonts bundled with the extension "
                            + "(see the list in FIGLET-FONTS.md).");
        }
        return font;
    }
}
