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
}
