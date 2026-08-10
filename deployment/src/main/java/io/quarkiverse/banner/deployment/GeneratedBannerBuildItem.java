package io.quarkiverse.banner.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the banner rendered at build time so it can be consumed by later build steps. Both a colour and a plain
 * version are kept; the runtime installs whichever suits the console.
 */
final class GeneratedBannerBuildItem extends SimpleBuildItem {

    private final String plain;
    private final String colored;

    GeneratedBannerBuildItem(String plain, String colored) {
        this.plain = plain;
        this.colored = colored;
    }

    /** The banner without any ANSI colour codes. */
    String getPlain() {
        return plain;
    }

    /** The banner with ANSI colour codes (equal to {@link #getPlain()} when no colour was configured). */
    String getColored() {
        return colored;
    }
}
