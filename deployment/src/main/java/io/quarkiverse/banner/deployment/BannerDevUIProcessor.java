package io.quarkiverse.banner.deployment;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.banner.runtime.BannerColor;
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
 * Rendering runs at build time, in the deployment classloader where the bundled fonts and the {@link Figlet}
 * renderer live, through the same {@link BannerRenderer#renderBanner} used for the start-up banner, so the
 * preview matches exactly.
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

        // The selectable colours, each with the config value, a friendly label and a CSS colour for the preview.
        List<Map<String, String>> colors = Arrays.stream(BannerColor.values())
                .map(BannerDevUIProcessor::colorChoice)
                .toList();
        card.addBuildTimeData("colors", colors);

        // Seed the form with the currently configured values.
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("text", config.text().orElseGet(BannerDevUIProcessor::applicationName));
        defaults.put("font", config.font().fileName());
        defaults.put("powerBy", config.powerBy());
        defaults.put("color", configValue(config.color()));
        defaults.put("backgroundColor", configValue(config.backgroundColor()));
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

        // Render the banner, print it to the console/log (in colour), and return it.
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
            // The preview and "Print to log" both use the coloured banner; the Dev UI turns its ANSI codes into
            // styled spans for the on-screen preview and prints it verbatim to the (colour-capable) dev console.
            BannerRenderer.Rendered banner = BannerRenderer.renderBanner(font, text, powerBy,
                    toColor(params.get("color")), toColor(params.get("backgroundColor")));
            return Map.of("banner", banner.colored());
        } catch (IOException ex) {
            return Map.of("error", "Unable to render banner: " + ex.getMessage());
        }
    }

    private static String applicationName() {
        return ConfigProvider.getConfig()
                .getOptionalValue("quarkus.application.name", String.class)
                .orElse("Quarkus");
    }

    /** The {@code application.properties} value for a colour: the enum name lower-cased with {@code _} to {@code -}. */
    private static String configValue(BannerColor color) {
        return color.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** Resolves a colour config value (as sent by the Dev UI) back to a {@link BannerColor}, defaulting safely. */
    private static BannerColor toColor(Object value) {
        if (value == null) {
            return BannerColor.DEFAULT;
        }
        try {
            return BannerColor.valueOf(value.toString().trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return BannerColor.DEFAULT;
        }
    }

    /** A colour choice for the Dev UI selector: config value, friendly label and a CSS colour for the preview. */
    private static Map<String, String> colorChoice(BannerColor color) {
        Map<String, String> choice = new LinkedHashMap<>();
        choice.put("value", configValue(color));
        choice.put("label", label(color));
        choice.put("css", CSS.getOrDefault(color, ""));
        return choice;
    }

    private static String label(BannerColor color) {
        String[] words = color.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return label.toString().trim();
    }

    /** CSS colours approximating the ANSI palette, for the Dev UI preview only (empty = terminal default). */
    private static final Map<BannerColor, String> CSS = buildCssPalette();

    private static Map<BannerColor, String> buildCssPalette() {
        Map<BannerColor, String> css = new LinkedHashMap<>();
        css.put(BannerColor.DEFAULT, "");
        css.put(BannerColor.BLACK, "#000000");
        css.put(BannerColor.RED, "#cd0000");
        css.put(BannerColor.GREEN, "#00cd00");
        css.put(BannerColor.YELLOW, "#cdcd00");
        css.put(BannerColor.BLUE, "#2222ee");
        css.put(BannerColor.MAGENTA, "#cd00cd");
        css.put(BannerColor.CYAN, "#00cdcd");
        css.put(BannerColor.WHITE, "#e5e5e5");
        css.put(BannerColor.BRIGHT_BLACK, "#7f7f7f");
        css.put(BannerColor.BRIGHT_RED, "#ff0000");
        css.put(BannerColor.BRIGHT_GREEN, "#00ff00");
        css.put(BannerColor.BRIGHT_YELLOW, "#ffff00");
        css.put(BannerColor.BRIGHT_BLUE, "#5c5cff");
        css.put(BannerColor.BRIGHT_MAGENTA, "#ff00ff");
        css.put(BannerColor.BRIGHT_CYAN, "#00ffff");
        css.put(BannerColor.BRIGHT_WHITE, "#ffffff");
        return css;
    }
}
