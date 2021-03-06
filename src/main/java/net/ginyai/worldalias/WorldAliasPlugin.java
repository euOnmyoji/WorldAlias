package net.ginyai.worldalias;

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextParseException;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author GiNYAi edited by yinyangshi
 */
@Plugin(
        id = "worldalias",
        name = "WorldAlias",
        version = "1.0",
        description = "Provides a custom world alias placeholder for nucleus and PAPI",
        authors = {
                "GiNYAi"
        },
        dependencies = {
                @Dependency(id = "placeholderapi", optional = true),
                @Dependency(id = "nucleus", optional = true)
        }
)
public class WorldAliasPlugin {

    @Inject
    private Logger logger;

    @Inject
    private PluginContainer pluginContainer;

    @Inject
    @ConfigDir(sharedRoot = true)
    private Path configDir;

    private Map<String, Map<String, Text>> alias;

    private static Text parseText(String s) {
        try {
            return TextSerializers.JSON.deserialize(s);
        } catch (TextParseException e) {
            return TextSerializers.FORMATTING_CODE.deserializeUnchecked(s);
        }
    }

    Logger getLogger() {
        return logger;
    }

    PluginContainer getContainer() {
        return pluginContainer;
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        try {
            reload();
        } catch (IOException e) {
            logger.error("Failed to load.", e);
        }
        if (Sponge.getPluginManager().isLoaded("nucleus")) {
            try {
                new NucleusHandler(this);
                logger.info("Added token to Nucleus");
            } catch (Exception e) {
                logger.error("Failed to add token to Nucleus", e);
            }
        }
        if (Sponge.getPluginManager().isLoaded("placeholderapi")) {
            try {
                new PapiHandler(this);
                logger.info("Added placeholder to PlaceholderAPI");
            } catch (Exception e) {
                logger.error("Failed to add placeholder to PlaceholderAPI", e);
            }
        }
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        try {
            logger.info("Reloading...");
            reload();
            logger.info("Reloaded");
        } catch (IOException e) {
            logger.error("Failed to reload.", e);
        }
    }

    @Nullable
    Text getAlias(World world, String group) {
        if (alias == null) {
            return null;
        } else {
            return alias.get(group).get(world.getName());
        }
    }

    private void reload() throws IOException {
        Path configPath = configDir.resolve("WorldAlias.conf");
        if (!Files.exists(configPath)) {
            Sponge.getAssetManager().getAsset(this, "default_config.conf").orElseThrow(FileNotFoundException::new)
                    .copyToFile(configPath, false);
        }
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(configPath).build();
        CommentedConfigurationNode node = loader.load();
        alias = new HashMap<>(4);
        node.getNode("WorldAlias", "Alias").getChildrenMap().forEach((group, mapperNode) -> {
            Map<String, Text> mappers = new HashMap<>(4);
            mapperNode.getChildrenMap().forEach((name, vNode) -> mappers.put(name.toString(), parseText(vNode.getString())));
            alias.put(group.toString(), mappers);
        });
    }
}
