package io.quarkiverse.quarkus.banner.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the banner text rendered at build time so it can be consumed by later build steps.
 */
final class GeneratedBannerBuildItem extends SimpleBuildItem {

    private final String text;

    GeneratedBannerBuildItem(String text) {
        this.text = text;
    }

    String getText() {
        return text;
    }
}
