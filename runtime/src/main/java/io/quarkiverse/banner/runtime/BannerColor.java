package io.quarkiverse.banner.runtime;

/**
 * The named colours the banner can be painted in.
 * <p>
 * These are the eight standard ANSI colours, their {@code bright-} variants, and {@code orange}. Each carries
 * the ANSI SGR parameter used when it is a foreground vs a background. Standard colours use the 16-colour codes;
 * {@code orange} (which has no 16-colour slot) uses a 24-bit truecolor sequence.
 * <p>
 * Arbitrary colours are also supported as {@code #rgb} / {@code #rrggbb} hex, and the terminal default; those,
 * together with these named colours, are resolved by {@link ResolvedColor#parse(String)}. Colour is only ever
 * emitted when the console supports ANSI colour (see {@code quarkus.console.color}).
 */
public enum BannerColor {

    BLACK("30", "40"),
    RED("31", "41"),
    GREEN("32", "42"),
    YELLOW("33", "43"),
    BLUE("34", "44"),
    MAGENTA("35", "45"),
    CYAN("36", "46"),
    WHITE("37", "47"),
    ORANGE("38;2;255;165;0", "48;2;255;165;0"),
    BRIGHT_BLACK("90", "100"),
    BRIGHT_RED("91", "101"),
    BRIGHT_GREEN("92", "102"),
    BRIGHT_YELLOW("93", "103"),
    BRIGHT_BLUE("94", "104"),
    BRIGHT_MAGENTA("95", "105"),
    BRIGHT_CYAN("96", "106"),
    BRIGHT_WHITE("97", "107");

    private final String foreground;
    private final String background;

    BannerColor(String foreground, String background) {
        this.foreground = foreground;
        this.background = background;
    }

    /** The ANSI SGR parameter for this colour used as a foreground (e.g. {@code 31} or {@code 38;2;255;165;0}). */
    public String foreground() {
        return foreground;
    }

    /** The ANSI SGR parameter for this colour used as a background (e.g. {@code 41} or {@code 48;2;255;165;0}). */
    public String background() {
        return background;
    }
}
