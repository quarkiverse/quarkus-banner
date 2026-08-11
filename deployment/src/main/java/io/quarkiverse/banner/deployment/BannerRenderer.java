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
        BannerColor[] colorForChar = colorForChar(markup, markup.cleanText().length());

        String plain = assemble(rows, null, colorForChar, BannerColor.DEFAULT, BannerColor.DEFAULT, powerBy, width);
        String colored = assemble(rows, rendered.owner(), colorForChar, foreground, background, powerBy, width);
        return new Rendered(plain, colored);
    }

    /** The colour that applies to each clean-text character, from the parsed colour transitions. */
    private static BannerColor[] colorForChar(Markup markup, int length) {
        BannerColor[] colors = new BannerColor[length];
        List<Transition> transitions = markup.transitions();
        int t = 0;
        for (int c = 0; c < length; c++) {
            while (t + 1 < transitions.size() && transitions.get(t + 1).index() <= c) {
                t++;
            }
            colors[c] = transitions.get(t).color();
        }
        return colors;
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

    /**
     * Assembles the final banner: each row painted cell-by-cell from the ink {@code owner} map, then the
     * optional tagline. When {@code owner} is {@code null} the banner is emitted plain (no colour codes).
     */
    private static String assemble(List<String> rows, int[][] owner, BannerColor[] colorForChar,
            BannerColor foreground, BannerColor background, boolean powerBy, int width) {
        StringBuilder banner = new StringBuilder();
        for (int r = 0; r < rows.size(); r++) {
            if (owner == null) {
                banner.append(rows.get(r));
            } else {
                banner.append(paintRow(rows.get(r), r < owner.length ? owner[r] : new int[0], colorForChar,
                        background));
            }
            banner.append('\n');
        }
        if (powerBy) {
            String poweredBy = "Powered by Quarkus " + Version.getVersion();
            int padding = Math.max(0, width - poweredBy.length());
            String tagline = " ".repeat(padding) + poweredBy;
            banner.append(owner == null ? tagline : colorize(tagline, foreground, background))
                    .append('\n').append('\n');
        }
        return banner.toString();
    }

    /** Paints one row: runs of cells sharing a colour (from their ink owner) are wrapped together. */
    private static String paintRow(String row, int[] ownerRow, BannerColor[] colorForChar, BannerColor background) {
        StringBuilder painted = new StringBuilder();
        int i = 0;
        int n = row.length();
        while (i < n) {
            BannerColor foreground = colorAt(ownerRow, i, colorForChar);
            int j = i + 1;
            while (j < n && colorAt(ownerRow, j, colorForChar) == foreground) {
                j++;
            }
            painted.append(colorize(row.substring(i, j), foreground, background));
            i = j;
        }
        return painted.toString();
    }

    /** The foreground colour of a single cell: its ink owner's colour, or the terminal default when blank. */
    private static BannerColor colorAt(int[] ownerRow, int column, BannerColor[] colorForChar) {
        int owner = column < ownerRow.length ? ownerRow[column] : -1;
        return owner >= 0 && owner < colorForChar.length ? colorForChar[owner] : BannerColor.DEFAULT;
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
}
