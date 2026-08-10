package io.quarkiverse.banner.runtime;

import java.util.Optional;
import java.util.logging.Formatter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.formatters.TextBannerFormatter;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BannerRecorder {

    private static final String DEFAULT_CONSOLE_FORMAT = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n";

    /**
     * Builds a console {@link Formatter} that prints the generated {@code banner} as a header and then delegates
     * to the standard Quarkus console formatter for the actual log records. This mirrors how Quarkus core renders
     * its own banner (via {@link TextBannerFormatter}), while honouring the user's console format and colour
     * settings.
     * <p>
     * When {@code showBanner} is {@code false} the plain delegate formatter is returned instead, so the banner is
     * not repainted. A formatter is still supplied in that case so Quarkus core's own banner stays suppressed; this
     * lets dev-mode hot reloads that don't change the banner stay quiet while still forcing a reprint when it does
     * change.
     */
    public RuntimeValue<Optional<Formatter>> bannerFormatter(String banner, boolean showBanner, BannerColor foreground,
            BannerColor background) {
        Config config = ConfigProvider.getConfig();

        String format = config.getOptionalValue("quarkus.log.console.format", String.class)
                .orElse(DEFAULT_CONSOLE_FORMAT);
        int darken = config.getOptionalValue("quarkus.log.console.darken", Integer.class).orElse(0);
        // Colour is controlled by "quarkus.console.color" (an Optional<Boolean>); when unset, fall back to
        // terminal detection, mirroring io.quarkus.runtime.logging.LoggingSetupRecorder#isColorEnabled.
        boolean color = config.getOptionalValue("quarkus.console.color", Boolean.class)
                .orElseGet(BannerRecorder::hasColorSupport);

        PatternFormatter delegate = color ? new ColorPatternFormatter(darken, format) : new PatternFormatter(format);

        if (!showBanner) {
            return new RuntimeValue<>(Optional.of(delegate));
        }

        String text = banner.endsWith("\n") ? banner : banner + "\n";
        // Only emit ANSI colour codes when the console actually supports colour, so log files and non-colour
        // terminals keep a plain banner.
        if (color) {
            text = colorize(text, foreground, background);
        }
        Formatter formatter = new TextBannerFormatter(TextBannerFormatter.createStringSupplier(text), delegate);

        return new RuntimeValue<>(Optional.of(formatter));
    }

    /**
     * Wraps every line of the banner in an ANSI SGR sequence for the given foreground and background colours,
     * resetting at the end of each line so the colour never bleeds into the log output that follows. Each line is
     * coloured in full (including its trailing padding), so a background colour fills the whole banner box.
     * Returns the banner unchanged when both colours are {@link BannerColor#DEFAULT}.
     */
    public static String colorize(String banner, BannerColor foreground, BannerColor background) {
        if (foreground.isDefault() && background.isDefault()) {
            return banner;
        }
        StringBuilder sgr = new StringBuilder("[");
        if (!foreground.isDefault()) {
            sgr.append(foreground.foregroundCode());
        }
        if (!background.isDefault()) {
            if (!foreground.isDefault()) {
                sgr.append(';');
            }
            sgr.append(background.backgroundCode());
        }
        String prefix = sgr.append('m').toString();
        String reset = "[0m";

        String[] lines = banner.split("\n", -1);
        StringBuilder out = new StringBuilder(banner.length() + lines.length * (prefix.length() + reset.length()));
        for (int i = 0; i < lines.length; i++) {
            // split() leaves a trailing empty element after the banner's final newline; don't colour it.
            if (i == lines.length - 1 && lines[i].isEmpty()) {
                break;
            }
            out.append(prefix).append(lines[i]).append(reset).append('\n');
        }
        return out.toString();
    }

    /**
     * Portable colour detection with no dependency on Quarkus internals (so the extension stays compatible with
     * older Quarkus versions): use colour when attached to a terminal, unless disabled via the {@code NO_COLOR}
     * convention or a {@code dumb} terminal.
     */
    private static boolean hasColorSupport() {
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        return System.console() != null && !"dumb".equals(System.getenv("TERM"));
    }
}
