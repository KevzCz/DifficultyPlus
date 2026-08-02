package net.pixeldreamstudios.difficultyplus.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Suggestions {
    private Suggestions() {}

    private static final Map<String, List<String>> CACHE = new HashMap<>();

    public static void invalidate() {
        CACHE.clear();
    }

    public static List<String> damageTypes() {
        return dynamic(Registries.DAMAGE_TYPE, false);
    }

    public static List<String> damageTypeTags() {
        return dynamic(Registries.DAMAGE_TYPE, true);
    }

    public static List<String> attributes() {
        return sorted(BuiltInRegistries.ATTRIBUTE, false);
    }

    public static List<String> mobEffects() {
        return sorted(BuiltInRegistries.MOB_EFFECT, false);
    }

    public static List<String> items() {
        return sorted(BuiltInRegistries.ITEM, false);
    }

    public static List<String> entityTypes() {
        List<String> out = new ArrayList<>(sorted(BuiltInRegistries.ENTITY_TYPE, false));
        out.addAll(sorted(BuiltInRegistries.ENTITY_TYPE, true));
        return out;
    }

    public static List<String> structuresAndTags() {
        List<String> out = new ArrayList<>(dynamic(Registries.STRUCTURE, false));
        out.addAll(dynamic(Registries.STRUCTURE, true));
        return out;
    }

    public static List<String> biomesAndTags() {
        List<String> out = new ArrayList<>(dynamic(Registries.BIOME, false));
        out.addAll(dynamic(Registries.BIOME, true));
        return out;
    }

    public static List<String> dimensions() {
        return dynamic(Registries.LEVEL_STEM, false);
    }

    private static <T> List<String> dynamic(ResourceKey<? extends Registry<T>> key, boolean tags) {
        String cacheKey = key.location() + (tags ? "#" : "");
        List<String> cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        RegistryAccess access = registryAccess();
        if (access == null) {
            Minecraft client = Minecraft.getInstance();
            DifficultyPlus.LOGGER.warn(
                    "Difficulty+ suggestions: no registry access for {} (client={} connection={} level={} server={})",
                    key.location(),
                    client != null,
                    client != null && client.getConnection() != null,
                    client != null && client.level != null,
                    client != null && client.getSingleplayerServer() != null);
            return List.of();
        }
        Registry<T> registry = access.registry(key).orElse(null);
        if (registry == null) {
            DifficultyPlus.LOGGER.warn("Difficulty+ suggestions: registry {} absent", key.location());
            return List.of();
        }
        List<String> out = sorted(registry, tags);
        CACHE.put(cacheKey, out);
        return out;
    }

    private static RegistryAccess registryAccess() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        IntegratedServer local = client.getSingleplayerServer();
        if (local != null) {
            return local.registryAccess();
        }
        if (client.getConnection() != null) {
            return client.getConnection().registryAccess();
        }
        ClientLevel level = client.level;
        return level == null ? null : level.registryAccess();
    }

    private static <T> List<String> sorted(Registry<T> registry, boolean tags) {
        List<String> out = new ArrayList<>();
        if (tags) {
            registry.getTagNames().forEach(tag -> out.add("#" + tag.location()));
        } else {
            registry.keySet().forEach(id -> out.add(id.toString()));
        }
        out.sort(String::compareTo);
        return out;
    }
}
