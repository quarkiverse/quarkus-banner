package io.quarkiverse.quarkus.banner.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BannerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(jar -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.banner-generator.text", "Hello Test")
            .overrideConfigKey("quarkus.banner-generator.font", "slant");

    @Test
    public void generatorConfigIsApplied() {
        assertEquals("Hello Test",
                ConfigProvider.getConfig().getValue("quarkus.banner-generator.text", String.class));
        assertEquals("slant",
                ConfigProvider.getConfig().getValue("quarkus.banner-generator.font", String.class));
    }
}
