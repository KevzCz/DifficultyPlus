package net.pixeldreamstudios.difficultyplus.internal.effect;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusEvents;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;
import net.pixeldreamstudios.difficultyplus.api.ZoneEffect;
import net.dungeon_difficulty.config.Config;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.LevelGate;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ZoneEffectManager {
    private ZoneEffectManager() {}

    private static final Map<Entity, String> ACTIVE = new IdentityHashMap<>();

    private static final Map<String, List<Parsed>> PARSED = new HashMap<>();

    private static boolean anyTargetsNonPlayers = false;

    private record Parsed(ZoneEffect effect, DifficultyPlusData.TargetFilter targets,
                          int minLevel, int maxLevel) {}

    public static void tick(ServerLevel level) {
        int interval = ConfigBridge.global().pollInterval();

        for (Player player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            if ((player.tickCount + player.getId()) % interval == 0) {
                evaluate(level, player);
            }
        }

        if (!anyTargetsNonPlayers) {
            return;
        }

        int radius = level.getServer().getPlayerList().getSimulationDistance() * 16;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || entity instanceof Player) {
                continue;
            }
            if ((entity.tickCount + entity.getId()) % interval != 0) {
                continue;
            }

            if (level.getNearestPlayer(entity, radius) == null) {
                continue;
            }
            evaluate(level, living);
        }
    }

    private static void evaluate(ServerLevel level, LivingEntity entity) {
        Optional<ZoneContext> zone = ZoneResolver.at(level, entity.blockPosition());
        String previous = ACTIVE.get(entity);
        String current = zone.map(ZoneContext::difficulty).orElse(null);

        if (previous != null && !previous.equals(current)) {
            removeAll(entity, previous, level);
        }

        if (zone.isEmpty()) {
            ACTIVE.remove(entity);
            return;
        }

        ZoneContext context = zone.get();
        boolean entering = !context.difficulty().equals(previous);
        ACTIVE.put(entity, context.difficulty());

        if (entering && DifficultyPlusEvents.ZONE_ENTERED.isListened()) {
            DifficultyPlusEvents.ZONE_ENTERED.invoke(l -> l.onTransition(entity, context));
        }

        for (Parsed parsed : effectsFor(context.difficulty())) {
            if (!TargetMatcher.matches(parsed.targets(), entity)) {
                continue;
            }
            if (!LevelGate.allows(parsed.minLevel(), parsed.maxLevel(), context.zoneLevel())) {
                parsed.effect().remove(entity, context);
                continue;
            }
            if (!allowed(entity, context, parsed.effect())) {
                continue;
            }
            parsed.effect().apply(entity, context);

            int tickInterval = parsed.effect().tickInterval();
            if (tickInterval > 0 && entity.tickCount % tickInterval == 0) {
                parsed.effect().tick(entity, context);
            }
        }
    }

    private static boolean allowed(LivingEntity entity, ZoneContext context, ZoneEffect effect) {
        if (!DifficultyPlusEvents.ZONE_EFFECT_APPLIED.isListened()) {
            return true;
        }
        for (DifficultyPlusEvents.ZoneEffectApplied listener
                : DifficultyPlusEvents.ZONE_EFFECT_APPLIED.listeners()) {
            if (!listener.allow(entity, context, effect)) {
                return false;
            }
        }
        return true;
    }

    private static void removeAll(LivingEntity entity, String difficulty, ServerLevel level) {
        ZoneContext stale = new ZoneContext(level, difficulty, 0, Optional.empty(),
                ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, "zone/" + difficulty));
        for (Parsed parsed : effectsFor(difficulty)) {
            parsed.effect().remove(entity, stale);
        }
        if (DifficultyPlusEvents.ZONE_EXITED.isListened()) {
            DifficultyPlusEvents.ZONE_EXITED.invoke(l -> l.onTransition(entity, stale));
        }
    }

    public static void forget(Entity entity) {
        ACTIVE.remove(entity);
    }

    private static List<Parsed> effectsFor(String difficulty) {
        return PARSED.computeIfAbsent(difficulty, ZoneEffectManager::parse);
    }

    private static List<Parsed> parse(String difficulty) {
        DifficultyPlusData data = ConfigBridge.forDifficulty(difficulty);
        List<Parsed> parsed = new ArrayList<>();
        if (data.zone_effects == null) {
            return parsed;
        }
        for (DifficultyPlusData.ZoneEffectEntry entry : data.zone_effects) {
            if (entry.type == null) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(entry.type);
            if (id == null) {
                DifficultyPlus.LOGGER.warn("Malformed zone effect type '{}'", entry.type);
                continue;
            }
            DifficultyPlusAPI.ZoneEffectRegistry.Factory factory = DifficultyPlusAPI.zoneEffects().get(id);
            if (factory == null) {
                DifficultyPlus.LOGGER.warn(
                        "Unknown zone effect type '{}' — its mod may not be installed; entry skipped", id);
                continue;
            }
            ZoneEffect effect = factory.create(payload(entry));
            if (effect != null) {
                parsed.add(new Parsed(effect, entry.targets, entry.min_level, entry.max_level));
            }
        }
        return parsed;
    }

    private static JsonObject payload(DifficultyPlusData.ZoneEffectEntry entry) {
        JsonObject json = entry.raw == null ? new JsonObject() : entry.raw.deepCopy();
        put(json, "duration", entry.duration);
        put(json, "interval", entry.interval);
        json.addProperty("linger", entry.linger);
        put(json, "attribute", entry.attribute);
        put(json, "operation", entry.operation);
        put(json, "value", entry.value);
        put(json, "effect", entry.effect);
        put(json, "amplifier", entry.amplifier);
        put(json, "damage_type", entry.damage_type);
        put(json, "amount", entry.amount);
        return json;
    }

    private static void put(JsonObject json, String key, String value) {
        if (value != null && !value.isEmpty()) {
            json.addProperty(key, value);
        }
    }

    private static void put(JsonObject json, String key, Number value) {
        if (value != null) {
            json.addProperty(key, value);
        }
    }

    public static void reload(Iterable<ServerLevel> levels) {
        for (ServerLevel level : levels) {
            for (Map.Entry<Entity, String> active : new HashMap<>(ACTIVE).entrySet()) {
                if (active.getKey() instanceof LivingEntity living && living.level() == level) {
                    removeAll(living, active.getValue(), level);
                }
            }
        }
        ACTIVE.clear();
        PARSED.clear();
        recomputeTargets();
    }

    public static void recomputeTargets() {
        anyTargetsNonPlayers = false;
        Config config = ConfigBridge.config();
        if (config != null && config.difficulty_types != null) {
            for (Config.DifficultyType type : config.difficulty_types) {
                if (type == null || type.name == null) {
                    continue;
                }
                DifficultyPlusData data = ConfigBridge.forDifficulty(type.name);
                if (data.zone_effects == null) {
                    continue;
                }
                for (DifficultyPlusData.ZoneEffectEntry entry : data.zone_effects) {
                    if (TargetMatcher.targetsNonPlayers(entry.targets)) {
                        anyTargetsNonPlayers = true;
                        return;
                    }
                }
            }
        }
        for (List<Parsed> effects : PARSED.values()) {
            for (Parsed parsed : effects) {
                if (TargetMatcher.targetsNonPlayers(parsed.targets())) {
                    anyTargetsNonPlayers = true;
                    return;
                }
            }
        }
    }

    public static void setTargetsNonPlayers(boolean value) {
        anyTargetsNonPlayers = value;
    }
}
