package io.quarkiverse.quarkus.banner.deployment;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkiverse.quarkus.banner.runtime.BannerConfig;
import io.quarkiverse.quarkus.banner.runtime.BannerRecorder;
import io.quarkus.builder.Version;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;

// Optional<BuildItem> parameters are the idiomatic way to optionally consume a build item in Quarkus.
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class BannerProcessor {

    private static final Logger LOG = Logger.getLogger(BannerProcessor.class);
    private static final String FEATURE = "banner";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Renders the banner at build time. Produces nothing when disabled or when rendering fails, in which case
     * Quarkus' own banner is left untouched.
     */
    @BuildStep
    GeneratedBannerBuildItem generateBanner(BannerConfig config) {
        if (!config.enabled()) {
            return null;
        }

        String text = config.text()
                .orElseGet(() -> ConfigProvider.getConfig()
                        .getOptionalValue("quarkus.application.name", String.class)
                        .orElse("Quarkus"));

        try {
            String bannerText = BannerRenderer.render(config.font(), text);
            StringBuilder banner = new StringBuilder(bannerText);
            if (config.powerBy()) {
                int width = bannerText.lines().mapToInt(String::length).max().orElse(0);
                String poweredBy = "Powered by Quarkus " + Version.getVersion();
                int padding = Math.max(0, width - poweredBy.length());
                banner.append(" ".repeat(padding)).append(poweredBy).append('\n').append('\n');
            }

            LOG.debugf("Generated banner for '%s' using font '%s'", text, config.font().fileName());
            return new GeneratedBannerBuildItem(banner.toString());
        } catch (IOException ex) {
            LOG.warnf(ex, "Unable to generate banner for text '%s' with font '%s'; keeping the default banner",
                    text, config.font().fileName());
            return null;
        }
    }

    /**
     * Installs the generated banner as the console handler's banner, the same way Quarkus core does: the console
     * formatter is wrapped in a {@code TextBannerFormatter}. Providing a console formatter automatically suppresses
     * core's own banner (its formatter/banner branch only runs when no formatter is supplied), so there is nothing
     * else to disable.
     * <p>
     * Unlike core's banner (which is {@code IsTest}-gated), this is also installed in test mode, so the banner is
     * visible when running tests too.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogConsoleFormatBuildItem installBanner(Optional<GeneratedBannerBuildItem> banner, BannerRecorder recorder) {
        return banner
                .map(b -> new LogConsoleFormatBuildItem(recorder.bannerFormatter(b.getText())))
                .orElse(null);
    }
}
