package io.quarkiverse.banner.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkiverse.banner.runtime.BannerColor;
import io.quarkiverse.banner.runtime.BannerFont;
import io.quarkus.builder.Version;

/**
 * Renders a piece of text into a FIGlet ASCII-art banner using the bundled {@link Figlet} renderer, using one
 * of the fonts bundled with this extension.
 * <p>
 * The text may contain inline colour markers of the form <code>{colour}</code> (for example
 * <code>{red}Quar{cyan}kus</code>) to paint different parts of the banner in different colours. Markers name a
 * {@link BannerColor} (the standard ANSI colours and their {@code bright-} variants, or {@code default}); an
 * unrecognised {@code {token}} is left in the text verbatim. A global foreground and background colour may also
 * be supplied and apply to any text not covered by an inline marker.
 * <p>
 * Every render produces <em>both</em> a colour and a plain version of the banner (see {@link Rendered}); the
 * runtime picks whichever suits the console, so no ANSI codes ever leak onto a colour-less terminal or into a
 * log file.
 */
final class BannerRenderer {

    /** Classpath location of the FIGlet fonts bundled with this extension. */
    static final String BUNDLED_FONTS_DIR = "/io/quarkiverse/banner/fonts/";

    private static final String ESC = "\u001b";
    private static final String RESET = ESC + "[0m";
    /** An inline colour marker: {@code {name}} where {@code name} looks like a colour (letters and hyphens). */
    private static final Pattern MARKER = Pattern.compile("\\{([A-Za-z][A-Za-z-]*)\\}");

    private BannerRenderer() {
    }

    /** A rendered banner in both its colour ({@link #colored}) and plain ({@link #plain}) forms. */
    record Rendered(String plain, String colored) {
    }

    /**
     * Renders {@code text} (which may contain inline {@code {colour}} markers) using the given bundled
     * {@code font}, optionally appending the right-aligned {@code Powered by Quarkus <version>} tagline, and
     * applying {@code foreground}/{@code background} to any text not covered by a marker.
     *
     * @return the banner in both its plain and coloured forms
     * @throws IOException if the font resource cannot be read or the text cannot be rendered
     */
    static Rendered renderBanner(BannerFont font, String text, boolean powerBy, BannerColor foreground,
            BannerColor background) throws IOException {
        Markup markup = parseMarkup(text, foreground);
        Figlet.RenderResult rendered = renderTracked(font, markup.cleanText());
        List<String> rows = splitRows(rendered.banner());
        int width = rows.isEmpty() ? 0 : rows.get(0).length();
        List<ColorSpan> spans = colorSpans(markup, rendered.columns(), width);

        String plain = assemble(rows, List.of(new ColorSpan(0, BannerColor.DEFAULT)), BannerColor.DEFAULT,
                powerBy, width, BannerColor.DEFAULT);
        String colored = assemble(rows, spans, background, powerBy, width, foreground);
        return new Rendered(plain, colored);
    }

    /**
     * Renders {@code text} using the given bundled {@code font} (no colour, no tagline). Used by the tests and
     * as the building block for {@link #renderBanner}.
     *
     * @throws IOException if the font resource cannot be read or the text cannot be rendered
     */
    static String render(BannerFont font, String text) throws IOException {
        String resource = BUNDLED_FONTS_DIR + font.fileName() + ".flf";
        try (InputStream is = BannerRenderer.class.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IOException("Bundled font resource not found: " + resource);
            }
            return Figlet.convertOneLine(is, text);
        }
    }

    /** Renders {@code text} and reports the output column where each character's glyph starts. */
    private static Figlet.RenderResult renderTracked(BannerFont font, String text) throws IOException {
        String resource = BUNDLED_FONTS_DIR + font.fileName() + ".flf";
        try (InputStream is = BannerRenderer.class.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IOException("Bundled font resource not found: " + resource);
            }
            return Figlet.renderTracked(is, text);
        }
    }

    /** Assembles the final banner: each row painted by {@code spans}, then the optional tagline. */
    private static String assemble(List<String> rows, List<ColorSpan> spans, BannerColor background,
            boolean powerBy, int width, BannerColor taglineColor) {
        StringBuilder banner = new StringBuilder();
        for (String row : rows) {
            banner.append(paint(row, spans, background)).append('\n');
        }
        if (powerBy) {
            String poweredBy = "Powered by Quarkus " + Version.getVersion();
            int padding = Math.max(0, width - poweredBy.length());
            banner.append(colorize(" ".repeat(padding) + poweredBy, taglineColor, background))
                    .append('\n').append('\n');
        }
        return banner.toString();
    }

    /** Splits a rendered block into its rows, dropping the trailing empty element left by the final newline. */
    private static List<String> splitRows(String block) {
        String[] parts = block.split("\n", -1);
        List<String> rows = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            if (i == parts.length - 1 && parts[i].isEmpty()) {
                break;
            }
            rows.add(parts[i]);
        }
        return rows;
    }

    /** Paints one banner row: each column span wrapped in its colour (over the shared {@code background}). */
    private static String paint(String row, List<ColorSpan> spans, BannerColor background) {
        StringBuilder painted = new StringBuilder();
        for (int i = 0; i < spans.size(); i++) {
            int start = Math.min(spans.get(i).start(), row.length());
            int end = (i + 1 < spans.size()) ? Math.min(spans.get(i + 1).start(), row.length()) : row.length();
            if (start >= end) {
                continue;
            }
            painted.append(colorize(row.substring(start, end), spans.get(i).color(), background));
        }
        return painted.toString();
    }

    /** Wraps a single piece of text in the ANSI sequence for {@code foreground}/{@code background}. */
    static String colorize(String text, BannerColor foreground, BannerColor background) {
        String prefix = sgr(foreground, background);
        return prefix.isEmpty() ? text : prefix + text + RESET;
    }

    /** The ANSI SGR prefix for a foreground/background pair, or {@code ""} when both are the default. */
    private static String sgr(BannerColor foreground, BannerColor background) {
        if (foreground.isDefault() && background.isDefault()) {
            return "";
        }
        StringBuilder sgr = new StringBuilder(ESC).append('[');
        if (!foreground.isDefault()) {
            sgr.append(foreground.foregroundCode());
        }
        if (!background.isDefault()) {
            if (!foreground.isDefault()) {
                sgr.append(';');
            }
            sgr.append(background.backgroundCode());
        }
        return sgr.append('m').toString();
    }

    /**
     * Maps the parsed colour transitions to output columns, giving the colour spans that cover {@code [0, width)}.
     * A transition before clean-text index {@code k} starts at the column where character {@code k}'s glyph is
     * placed, so a colour change lands exactly where the next character's ink begins -- kerning included.
     */
    private static List<ColorSpan> colorSpans(Markup markup, int[] columns, int width) {
        List<ColorSpan> spans = new ArrayList<>();
        for (Transition transition : markup.transitions()) {
            int index = transition.index();
            int column = index <= 0 ? 0 : (index < columns.length ? Math.min(width, columns[index]) : width);
            if (!spans.isEmpty() && spans.get(spans.size() - 1).start() == column) {
                spans.set(spans.size() - 1, new ColorSpan(column, transition.color()));
            } else {
                spans.add(new ColorSpan(column, transition.color()));
            }
        }
        return spans;
    }

    /** Splits {@code text} into its clean (marker-free) form and the colour transitions over its indices. */
    private static Markup parseMarkup(String text, BannerColor defaultColor) {
        StringBuilder clean = new StringBuilder();
        List<Transition> transitions = new ArrayList<>();
        transitions.add(new Transition(0, defaultColor));

        Matcher matcher = MARKER.matcher(text);
        int last = 0;
        while (matcher.find()) {
            BannerColor color = colorFor(matcher.group(1));
            if (color == null) {
                continue; // not a colour name: leave the "{token}" in the text verbatim
            }
            clean.append(text, last, matcher.start());
            transitions.add(new Transition(clean.length(), color));
            last = matcher.end();
        }
        clean.append(text, last, text.length());
        return new Markup(clean.toString(), transitions);
    }

    /** Resolves a marker name (e.g. {@code bright-red}) to a {@link BannerColor}, or {@code null} if unknown. */
    private static BannerColor colorFor(String name) {
        try {
            return BannerColor.valueOf(name.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException notAColour) {
            return null;
        }
    }

    /** The marker-free text and the colour transitions over its character indices. */
    private record Markup(String cleanText, List<Transition> transitions) {
    }

    /** A colour change starting at a clean-text character index. */
    private record Transition(int index, BannerColor color) {
    }

    /** A colour applied from a starting output column until the next span (or the end of the row). */
    private record ColorSpan(int start, BannerColor color) {
    }
}
