package io.quarkiverse.banner.runtime;

/**
 * ANSI colours that the banner can be painted in.
 * <p>
 * {@link #DEFAULT} leaves the terminal's own colour untouched (no colour codes are emitted). The remaining
 * constants are the eight standard ANSI colours and their bright variants. Colour is only ever applied when
 * the console actually supports ANSI colour (see {@code quarkus.console.color}); otherwise the banner is
 * printed as plain text so log files and non-colour terminals stay clean.
 */
public enum BannerColor {

    DEFAULT(-1),
    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7),
    BRIGHT_BLACK(8),
    BRIGHT_RED(9),
    BRIGHT_GREEN(10),
    BRIGHT_YELLOW(11),
    BRIGHT_BLUE(12),
    BRIGHT_MAGENTA(13),
    BRIGHT_CYAN(14),
    BRIGHT_WHITE(15);

    private final int index;

    BannerColor(int index) {
        this.index = index;
    }

    /** Whether this is the terminal default (no colour codes are emitted). */
    public boolean isDefault() {
        return index < 0;
    }

    /** The ANSI SGR parameter for this colour used as a foreground, or {@code -1} for the default. */
    public int foregroundCode() {
        if (index < 0) {
            return -1;
        }
        return index < 8 ? 30 + index : 90 + (index - 8);
    }

    /** The ANSI SGR parameter for this colour used as a background, or {@code -1} for the default. */
    public int backgroundCode() {
        if (index < 0) {
            return -1;
        }
        return index < 8 ? 40 + index : 100 + (index - 8);
    }
}
