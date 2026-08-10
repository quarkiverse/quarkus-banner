package io.quarkiverse.banner.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * Build-time configuration for the generated startup banner.
 * <p>
 * The banner is rendered at build time and installed on the console log handler so it is shown at start-up,
 * the same way Quarkus renders its own banner.
 */
@ConfigMapping(prefix = "quarkus.banner-generator")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface BannerConfig {

    /**
     * Whether the banner should be generated at build time.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The text to render as a FIGlet banner. When not set, the application name
     * ({@code quarkus.application.name}) is used.
     */
    Optional<String> text();

    /**
     * The FIGlet font used to render the banner.
     * <p>
     * Must be one of the fonts bundled with, and tested against, this extension (for example {@code standard},
     * {@code slant}, {@code doom}, {@code big} or {@code colossal}). The value is the font's name, matched
     * case-insensitively; the full list is in {@code FIGLET-FONTS.md}. An unknown font is a build-time error.
     */
    @WithDefault("standard")
    @WithConverter(BannerFontConverter.class)
    BannerFont font();

    /**
     * True if the Power by Quarkus tag line is to be displated
     */
    @WithDefault("true")
    boolean powerBy();

    /**
     * The foreground colour the banner is painted in.
     * <p>
     * Colour is only applied when the console supports ANSI colour (governed by {@code quarkus.console.color}
     * and terminal detection); otherwise the banner is printed as plain text. Defaults to {@code default}, which
     * leaves the terminal's own colour untouched.
     */
    @WithDefault("default")
    BannerColor color();

    /**
     * The background colour the banner is painted on. Because the banner is a full rectangular block, this fills
     * the whole box behind the text. Subject to the same ANSI colour support as {@link #color()}.
     */
    @WithDefault("default")
    BannerColor backgroundColor();
}
