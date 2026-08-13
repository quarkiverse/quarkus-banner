package io.quarkiverse.banner.runtime;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A colour resolved to its ANSI SGR parameters. {@link #foreground} / {@link #background} are the SGR params
 * for using the colour as a foreground / background respectively, or {@code null} for the terminal default.
 * <p>
 * {@link #parse(String)} accepts a {@link BannerColor} name (case-insensitively, hyphens or underscores), a
 * {@code #rgb} / {@code #rrggbb} hex colour, or {@code default} (and blank) for the terminal default.
 */
public record ResolvedColor(String foreground, String background) {

    /** The terminal default: no colour codes are emitted. */
    public static final ResolvedColor DEFAULT = new ResolvedColor(null, null);

    private static final Pattern HEX = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");

    /** Whether this is the terminal default (emits no colour). */
    public boolean isDefault() {
        return foreground == null && background == null;
    }

    /**
     * Resolves a colour token to its ANSI parameters, or returns {@code null} if the token is not a colour.
     * A {@code null} or blank token, or {@code default}, resolves to {@link #DEFAULT}.
     */
    public static ResolvedColor parse(String token) {
        if (token == null) {
            return DEFAULT;
        }
        String value = token.trim();
        if (value.isEmpty() || value.equalsIgnoreCase("default")) {
            return DEFAULT;
        }
        Matcher hex = HEX.matcher(value);
        if (hex.matches()) {
            return rgb(hex.group(1));
        }
        try {
            BannerColor named = BannerColor.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
            return new ResolvedColor(named.foreground(), named.background());
        } catch (IllegalArgumentException notANamedColour) {
            return null;
        }
    }

    private static ResolvedColor rgb(String hex) {
        if (hex.length() == 3) {
            hex = new StringBuilder().append(hex.charAt(0)).append(hex.charAt(0)).append(hex.charAt(1))
                    .append(hex.charAt(1)).append(hex.charAt(2)).append(hex.charAt(2)).toString();
        }
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new ResolvedColor("38;2;" + r + ";" + g + ";" + b, "48;2;" + r + ";" + g + ";" + b);
    }
}
