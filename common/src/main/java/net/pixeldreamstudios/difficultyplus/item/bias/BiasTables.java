package net.pixeldreamstudios.difficultyplus.item.bias;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BiasTables {
    private BiasTables() {}

    public static final String DIRECTORY = "difficultyplus_biases";

    private static List<BiasDefinition> definitions = List.of();

    public static void reload(ResourceManager manager) {
        List<BiasDefinition> loaded = new ArrayList<>();
        Map<ResourceLocation, Resource> resources =
                manager.listResources(DIRECTORY, path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation file = entry.getKey();
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                if (!json.isJsonObject()) {
                    continue;
                }
                BiasDefinition definition = read(json.getAsJsonObject(), file);
                if (definition != null) {
                    loaded.add(definition);
                }
            } catch (Exception e) {
                DifficultyPlus.LOGGER.warn("Failed to read {} file {}", DIRECTORY, file, e);
            }
        }

        definitions = List.copyOf(loaded);
        DifficultyPlus.LOGGER.info("Loaded {} garnet bias definition(s).", definitions.size());
    }

    private static BiasDefinition read(JsonObject json, ResourceLocation file) {
        JsonObject bias = json.has("bias") && json.get("bias").isJsonObject()
                ? json.getAsJsonObject("bias")
                : new JsonObject();
        JsonObject pierce = json.has("pierce") && json.get("pierce").isJsonObject()
                ? json.getAsJsonObject("pierce")
                : new JsonObject();

        int min = bias.has("min") ? bias.get("min").getAsInt() : 0;
        int max = bias.has("max") ? bias.get("max").getAsInt() : min;
        if (max < min) {
            DifficultyPlus.LOGGER.warn("Bias {} has max below min — entry skipped", file);
            return null;
        }

        return new BiasDefinition(
                file,
                json.has("name") ? json.get("name").getAsString()
                        : "item.difficultyplus.bias.unnamed",
                colourOf(json),
                Math.max(0, json.has("weight") ? json.get("weight").getAsInt() : 1),
                json.has("rarity") ? json.get("rarity").getAsString() : "common",
                min,
                max,
                pierce.has("floor") && pierce.get("floor").getAsBoolean(),
                pierce.has("ceiling") && pierce.get("ceiling").getAsBoolean(),
                minZoneLevel(json));
    }

    private static int minZoneLevel(JsonObject json) {
        if (!json.has("verifiers") || !json.get("verifiers").isJsonArray()) {
            return 0;
        }
        int highest = 0;
        for (JsonElement element : json.getAsJsonArray("verifiers")) {
            if (element.isJsonObject()) {
                JsonObject verifier = element.getAsJsonObject();
                if (verifier.has("min_zone_level")) {
                    highest = Math.max(highest, verifier.get("min_zone_level").getAsInt());
                }
            }
        }
        return highest;
    }

    private static int colourOf(JsonObject json) {
        if (!json.has("colour")) {
            return 0xFFC43D3D;
        }
        String raw = json.get("colour").getAsString().replace("#", "");
        try {
            long parsed = Long.parseLong(raw, 16);
            return raw.length() <= 6 ? (int) (0xFF000000L | parsed) : (int) parsed;
        } catch (NumberFormatException e) {
            return 0xFFC43D3D;
        }
    }

    public static BiasDefinition pick(RandomSource random, int zoneLevel) {
        List<BiasDefinition> eligible = new ArrayList<>();
        int total = 0;
        for (BiasDefinition definition : definitions) {
            if (definition.weight() <= 0 || definition.minZoneLevel() > zoneLevel) {
                continue;
            }
            eligible.add(definition);
            total += definition.weight();
        }
        if (eligible.isEmpty() || total <= 0) {
            return BiasDefinition.PLAIN;
        }

        int target = random.nextInt(total);
        for (BiasDefinition definition : eligible) {
            target -= definition.weight();
            if (target < 0) {
                return definition;
            }
        }
        return eligible.get(eligible.size() - 1);
    }

    public static BiasDefinition byId(ResourceLocation id) {
        for (BiasDefinition definition : definitions) {
            if (definition.id().equals(id)) {
                return definition;
            }
        }
        return BiasDefinition.PLAIN;
    }

    public static List<BiasDefinition> all() {
        return definitions;
    }
}
