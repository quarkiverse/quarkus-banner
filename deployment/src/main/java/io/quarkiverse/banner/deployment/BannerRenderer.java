package io.quarkiverse.banner.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkiverse.banner.runtime.Alignment;
import io.quarkiverse.banner.runtime.BannerFont;
import io.quarkiverse.banner.runtime.ResolvedColor;
import io.quarkus.builder.Version;

/**
 * Renders a piece of text into a FIGlet ASCII-art banner using the bundled {@link Figlet} renderer, using one
 * of the fonts bundled with this extension.
 * <p>
 * The text may contain inline colour markers of the form <code>{colour}</code> (for example
 * <code>{red}Quar{cyan}kus</code>) to paint different parts of the banner in different colours. A marker is a
 * colour name, {@code orange}, a {@code #rrggbb} hex colour, or {@code default} (see
 * {@link ResolvedColor#parse}); an unrecognised {@code {token}} is left in the text verbatim. A global
 * foreground and background colour may also be supplied and apply to any text not covered by an inline marker.
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
    private static final Pattern MARKER = Pattern.compile("\\{([A-Za-z0-9#-]+)\\}");
    /** A line break in the banner text: an actual newline or a literal backslash-n. */
    private static final Pattern LINE_BREAK = Pattern.compile("\\r\\n|\\r|\\n|\\\\n");

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
    /** Left-aligned, single/multi-line convenience used by the tests. */
    static Rendered renderBanner(BannerFont font, String text, boolean powerBy, ResolvedColor foreground,
            ResolvedColor background) throws IOException {
        return renderBanner(font, text, powerBy, foreground, background, Alignment.LEFT, 1);
    }

    static Rendered renderBanner(BannerFont font, String text, boolean powerBy, ResolvedColor foreground,
            ResolvedColor background, Alignment alignment, int lineSpacing) throws IOException {
        List<LineBlock> blocks = new ArrayList<>();
        int maxWidth = 0;
        for (String line : splitLines(text)) {
            LineBlock block = renderLineBlock(font, line, foreground, background);
            blocks.add(block);
            maxWidth = Math.max(maxWidth, block.width());
        }

        int spacing = Math.max(0, lineSpacing);
        String plain = assemble(blocks, maxWidth, alignment, spacing, ResolvedColor.DEFAULT, ResolvedColor.DEFAULT,
                powerBy, false);
        String colored = assemble(blocks, maxWidth, alignment, spacing, foreground, background, powerBy, true);
        return new Rendered(plain, colored);
    }

    /** Splits the banner text into lines on {@code \n} (an actual newline or a literal backslash-n). */
    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>(List.of(LINE_BREAK.split(text, -1)));
        while (lines.size() > 1 && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    /** Renders one line into a FIGlet block, keeping both its plain rows and its cell-coloured rows. */
    private static LineBlock renderLineBlock(BannerFont font, String line, ResolvedColor foreground,
            ResolvedColor background) throws IOException {
        Markup markup = parseMarkup(line, foreground);
        Figlet.RenderResult rendered = renderTracked(font, markup.cleanText());
        List<String> rows = splitRows(rendered.banner());
        int width = rows.isEmpty() ? 0 : rows.get(0).length();
        ResolvedColor[] colorForChar = colorForChar(markup, markup.cleanText().length());
        int[][] owner = rendered.owner();
        List<String> coloredRows = new ArrayList<>(rows.size());
        for (int r = 0; r < rows.size(); r++) {
            coloredRows.add(paintRow(rows.get(r), r < owner.length ? owner[r] : new int[0], colorForChar, background));
        }
        return new LineBlock(rows, coloredRows, width);
    }

    /** One rendered line: its plain rows, its cell-coloured rows, and its width. */
    private record LineBlock(List<String> plainRows, List<String> coloredRows, int width) {
    }

    /** The colour that applies to each clean-text character, from the parsed colour transitions. */
    private static ResolvedColor[] colorForChar(Markup markup, int length) {
        ResolvedColor[] colors = new ResolvedColor[length];
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
     * Assembles the final banner: each line's block padded to {@code maxWidth} per {@code alignment}, stacked with
     * {@code lineSpacing} blank rows between lines, then the optional tagline. {@code colored} selects the coloured
     * or plain rows. The background fills the banner box (blocks and the gaps between lines) but not the tagline.
     */
    private static String assemble(List<LineBlock> blocks, int maxWidth, Alignment alignment, int lineSpacing,
            ResolvedColor taglineForeground, ResolvedColor background, boolean powerBy, boolean colored) {
        StringBuilder banner = new StringBuilder();
        for (int b = 0; b < blocks.size(); b++) {
            LineBlock block = blocks.get(b);
            int left = leadingPad(alignment, maxWidth, block.width());
            int right = maxWidth - block.width() - left;
            List<String> rows = colored ? block.coloredRows() : block.plainRows();
            for (String row : rows) {
                banner.append(pad(left, background, colored)).append(row).append(pad(right, background, colored))
                        .append('\n');
            }
            if (b < blocks.size() - 1) {
                for (int s = 0; s < lineSpacing; s++) {
                    banner.append(pad(maxWidth, background, colored)).append('\n');
                }
            }
        }
        if (powerBy) {
            String poweredBy = "Powered by Quarkus " + Version.getVersion();
            int padding = Math.max(0, maxWidth - poweredBy.length());
            String tagline = " ".repeat(padding) + poweredBy;
            // The tagline gets the foreground colour but never the background box.
            banner.append(colored ? colorize(tagline, taglineForeground, ResolvedColor.DEFAULT) : tagline)
                    .append('\n').append('\n');
        }
        return banner.toString();
    }

    /** Leading padding for a block of {@code width} within {@code maxWidth}, per the alignment. */
    private static int leadingPad(Alignment alignment, int maxWidth, int width) {
        int slack = Math.max(0, maxWidth - width);
        return switch (alignment) {
            case LEFT -> 0;
            case RIGHT -> slack;
            case CENTER -> slack / 2;
        };
    }

    /** {@code n} padding spaces, carrying the background colour when producing the coloured banner. */
    private static String pad(int n, ResolvedColor background, boolean colored) {
        if (n <= 0) {
            return "";
        }
        String spaces = " ".repeat(n);
        return colored ? colorize(spaces, ResolvedColor.DEFAULT, background) : spaces;
    }

    /** Paints one row: runs of cells sharing a colour (from their ink owner) are wrapped together. */
    private static String paintRow(String row, int[] ownerRow, ResolvedColor[] colorForChar,
            ResolvedColor background) {
        StringBuilder painted = new StringBuilder();
        int i = 0;
        int n = row.length();
        while (i < n) {
            ResolvedColor foreground = colorAt(ownerRow, i, colorForChar);
            int j = i + 1;
            while (j < n && colorAt(ownerRow, j, colorForChar).equals(foreground)) {
                j++;
            }
            painted.append(colorize(row.substring(i, j), foreground, background));
            i = j;
        }
        return painted.toString();
    }

    /** The foreground colour of a single cell: its ink owner's colour, or the terminal default when blank. */
    private static ResolvedColor colorAt(int[] ownerRow, int column, ResolvedColor[] colorForChar) {
        int owner = column < ownerRow.length ? ownerRow[column] : -1;
        return owner >= 0 && owner < colorForChar.length ? colorForChar[owner] : ResolvedColor.DEFAULT;
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
    static String colorize(String text, ResolvedColor foreground, ResolvedColor background) {
        String prefix = sgr(foreground, background);
        return prefix.isEmpty() ? text : prefix + text + RESET;
    }

    /** The ANSI SGR prefix for a foreground/background pair, or {@code ""} when both are the default. */
    private static String sgr(ResolvedColor foreground, ResolvedColor background) {
        if (foreground.isDefault() && background.isDefault()) {
            return "";
        }
        StringBuilder sgr = new StringBuilder(ESC).append('[');
        if (!foreground.isDefault()) {
            sgr.append(foreground.foreground());
        }
        if (!background.isDefault()) {
            if (!foreground.isDefault()) {
                sgr.append(';');
            }
            sgr.append(background.background());
        }
        return sgr.append('m').toString();
    }

    /** Splits {@code text} into its clean (marker-free) form and the colour transitions over its indices. */
    private static Markup parseMarkup(String text, ResolvedColor defaultColor) {
        StringBuilder clean = new StringBuilder();
        List<Transition> transitions = new ArrayList<>();
        transitions.add(new Transition(0, defaultColor));

        Matcher matcher = MARKER.matcher(text);
        int last = 0;
        while (matcher.find()) {
            ResolvedColor color = ResolvedColor.parse(matcher.group(1));
            if (color == null) {
                continue; // not a colour: leave the "{token}" in the text verbatim
            }
            clean.append(text, last, matcher.start());
            transitions.add(new Transition(clean.length(), color));
            last = matcher.end();
        }
        clean.append(text, last, text.length());
        return new Markup(clean.toString(), transitions);
    }

    /** The marker-free text and the colour transitions over its character indices. */
    private record Markup(String cleanText, List<Transition> transitions) {
    }

    /** A colour change starting at a clean-text character index. */
    private record Transition(int index, ResolvedColor color) {
    }
}
