package io.quarkiverse.banner.deployment;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkiverse.banner.runtime.BannerConfig;
import io.quarkiverse.banner.runtime.BannerRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
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
            String banner = BannerRenderer.renderBanner(config.font(), text, config.powerBy());

            LOG.debugf("Generated banner for '%s' using font '%s'", text, config.font().fileName());
            return new GeneratedBannerBuildItem(banner);
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
     * <p>
     * In dev mode the augmentation re-runs on every restart, so the banner would otherwise be repainted on every
     * hot reload. To avoid that noise while still reflecting configuration changes, the last rendered banner is
     * remembered in the {@link LiveReloadBuildItem} context: the banner is only printed on the first start or when
     * its rendered text actually changed (a new {@code text}, {@code font} or {@code power-by}). On an unchanged
     * hot reload a plain formatter is installed instead, which keeps core's banner suppressed without repainting.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogConsoleFormatBuildItem installBanner(Optional<GeneratedBannerBuildItem> banner, BannerRecorder recorder,
            LiveReloadBuildItem liveReload) {
        if (banner.isEmpty()) {
            return null;
        }

        String text = banner.get().getText();
        BannerContext previous = liveReload.getContextObject(BannerContext.class);
        boolean showBanner = !liveReload.isLiveReload() || previous == null || !text.equals(previous.text());
        liveReload.setContextObject(BannerContext.class, new BannerContext(text));

        if (showBanner && liveReload.isLiveReload()) {
            LOG.debug("Banner changed; repainting it on live reload");
        }

        return new LogConsoleFormatBuildItem(recorder.bannerFormatter(text, showBanner));
    }

    /**
     * Remembers the banner rendered on the previous augmentation so a hot reload can tell whether it changed.
     * Stored in the {@link LiveReloadBuildItem} context, which survives dev-mode restarts.
     */
    record BannerContext(String text) {
    }
}
