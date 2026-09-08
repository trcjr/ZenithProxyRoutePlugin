package com.trcjr.zenithroute;

import com.trcjr.zenithroute.command.RouteCommand;
import com.trcjr.zenithroute.module.OrderedRouteModule;
import com.zenith.plugin.api.Plugin;
import com.zenith.plugin.api.PluginAPI;
import com.zenith.plugin.api.ZenithProxyPlugin;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

@Plugin(
    id = BuildConstants.PLUGIN_ID,
    version = BuildConstants.VERSION,
    description = "Proof-of-concept ordered waypoint routes for ZenithProxy",
    url = "https://github.com/trcjr/ZenithProxyRoutePlugin",
    authors = {"trcjr"},
    mcVersions = {BuildConstants.MC_VERSION}
)
public class OrderedRoutesPlugin implements ZenithProxyPlugin {
    public static RouteConfig CONFIG;
    public static ComponentLogger LOG;

    @Override
    public void onLoad(PluginAPI pluginAPI) {
        LOG = pluginAPI.getLogger();
        CONFIG = pluginAPI.registerConfig(BuildConstants.PLUGIN_ID, RouteConfig.class);
        var module = new OrderedRouteModule();
        pluginAPI.registerModule(module);
        pluginAPI.registerCommand(new RouteCommand(module));
        LOG.info("Ordered Routes POC loaded with {} configured steps", CONFIG.steps.size());
    }
}
