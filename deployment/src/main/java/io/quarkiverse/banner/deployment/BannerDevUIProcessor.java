package io.quarkiverse.banner.deployment;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.banner.runtime.BannerConfig;
import io.quarkiverse.banner.runtime.BannerFont;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * Contributes a Dev UI card for the extension that lets you preview the banner with any bundled font and text
 * and print it to the console — without editing {@code application.properties} or restarting the application.
 * <p>
 * Rendering runs at build time, in the deployment classloader where the bundled fonts and jfiglet live, through
 * the same {@link BannerRenderer#renderBanner} used for the start-up banner, so the preview matches exactly.
 */
class BannerDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem card(BannerConfig config) {
        CardPageBuildItem card = new CardPageBuildItem();

        // A small flag mark shown as the card logo (served from src/main/resources/dev-ui). Data URIs are
        // blocked by the Dev UI content-security policy, so the logo must be a served resource.
        card.setLogo("banner-logo.svg", "banner-logo.svg");

        // Show the extension name and its version as a badge on the card. Dev UI resolves the version of the
        // given group:artifact from the application's dependencies, so the name must not repeat it.
        card.addLibraryVersion("io.quarkiverse.banner", "quarkus-banner", "Quarkus Banner",
                "https://github.com/quarkiverse/quarkus-banner");

        // The bundled fonts, by configuration name, for the font selector.
        List<String> fonts = Arrays.stream(BannerFont.values())
                .map(BannerFont::fileName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        card.addBuildTimeData("fonts", fonts);

        // Seed the form with the currently configured values.
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("text", config.text().orElseGet(BannerDevUIProcessor::applicationName));
        defaults.put("font", config.font().fileName());
        defaults.put("powerBy", config.powerBy());
        card.addBuildTimeData("defaults", defaults);

        card.addPage(Page.webComponentPageBuilder()
                .title("Preview")
                .componentLink("qwc-banner.js")
                .icon("font-awesome-solid:flag"));

        return card;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    BuildTimeActionBuildItem actions() {
        BuildTimeActionBuildItem actions = new BuildTimeActionBuildItem();

        // Render the banner and return it for the live preview.
        actions.actionBuilder()
                .methodName("render")
                .function(BannerDevUIProcessor::render)
                .build();

        // Render the banner, print it to the console/log, and return it.
        actions.actionBuilder()
                .methodName("display")
                .function(params -> {
                    Map<String, String> result = render(params);
                    String banner = result.get("banner");
                    if (banner != null) {
                        System.out.println(System.lineSeparator() + banner);
                    }
                    return result;
                })
                .build();

        return actions;
    }

    /**
     * Renders the banner described by {@code params} ({@code text}, {@code font}, {@code powerBy}). Returns a
     * map with either a {@code banner} entry (the rendered ASCII art) or an {@code error} entry.
     * <p>
     * The values arrive as their JSON types ({@code powerBy} is a boolean, not a string), so they are read as
     * objects to avoid a {@link ClassCastException} from the erased {@code Map<String, String>} signature.
     */
    private static Map<String, String> render(Map<String, ?> params) {
        String text = Objects.toString(params.get("text"), "");
        String fontName = Objects.toString(params.get("font"), null);
        boolean powerBy = Boolean.parseBoolean(Objects.toString(params.get("powerBy"), "false"));

        BannerFont font = BannerFont.fromName(fontName);
        if (font == null) {
            return Map.of("error", "Unknown font: " + fontName);
        }

        try {
            return Map.of("banner", BannerRenderer.renderBanner(font, text, powerBy));
        } catch (IOException ex) {
            return Map.of("error", "Unable to render banner: " + ex.getMessage());
        }
    }

    private static String applicationName() {
        return ConfigProvider.getConfig()
                .getOptionalValue("quarkus.application.name", String.class)
                .orElse("Quarkus");
    }
}
