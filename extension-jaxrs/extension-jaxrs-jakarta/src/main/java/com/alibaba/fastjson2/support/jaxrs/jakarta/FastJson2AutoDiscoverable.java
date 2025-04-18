package com.alibaba.fastjson2.support.jaxrs.jakarta;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;

/**
 * FastJson2AutoDiscoverable
 * 参考: com.alibaba.fastjson.support.jaxrs.FastJsonAutoDiscoverable
 *
 * @author 张治保
 * @since 2024/10/16
 * @see AutoDiscoverable
 */
@Priority(AutoDiscoverable.DEFAULT_PRIORITY - 1)
public class FastJson2AutoDiscoverable
        implements AutoDiscoverable {
    public static final String FASTJSON_AUTO_DISCOVERABLE = "fastjson.auto.discoverable";
    public static volatile boolean autoDiscover;

    static {
        autoDiscover = Boolean.parseBoolean(
                System.getProperty(FASTJSON_AUTO_DISCOVERABLE, Boolean.TRUE.toString()));
    }

    @Override
    public void configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        // Register FastJson.
        if (!config.isRegistered(FastJson2Feature.class) && autoDiscover) {
            context.register(FastJson2Feature.class);
        }
    }
}
