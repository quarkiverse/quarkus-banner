package io.quarkiverse.quarkus.banner.runtime;

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
     */
    public RuntimeValue<Optional<Formatter>> bannerFormatter(String banner) {
        Config config = ConfigProvider.getConfig();

        String format = config.getOptionalValue("quarkus.log.console.format", String.class)
                .orElse(DEFAULT_CONSOLE_FORMAT);
        int darken = config.getOptionalValue("quarkus.log.console.darken", Integer.class).orElse(0);
        // Colour is controlled by "quarkus.console.color" (an Optional<Boolean>); when unset, fall back to
        // terminal detection, mirroring io.quarkus.runtime.logging.LoggingSetupRecorder#isColorEnabled.
        boolean color = config.getOptionalValue("quarkus.console.color", Boolean.class)
                .orElseGet(BannerRecorder::hasColorSupport);

        PatternFormatter delegate = color ? new ColorPatternFormatter(darken, format) : new PatternFormatter(format);

        String text = banner.endsWith("\n") ? banner : banner + "\n";
        Formatter formatter = new TextBannerFormatter(TextBannerFormatter.createStringSupplier(text), delegate);

        return new RuntimeValue<>(Optional.of(formatter));
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
