package net.pixeldreamstudios.difficultyplus.internal.structure;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.api.StructureKey;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.level.LevelStore;
import net.pixeldreamstudios.difficultyplus.internal.level.StructureLevelAccess;
import net.pixeldreamstudios.difficultyplus.internal.zone.LevelRoll;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneChunkSync;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneResolver;
import net.pixeldreamstudios.difficultyplus.item.SocketData;
import net.pixeldreamstudios.difficultyplus.item.bias.BiasDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StructureRefresh {
    private StructureRefresh() {}

    public static Result inspect(ServerLevel level, StructureStart start) {
        GlobalData.StructureRefresh config = ConfigBridge.global().structure_refresh;
        if (!config.enabled) {
            return Result.disabled();
        }
        if (blacklisted(level, start, config)) {
            return Result.excluded();
        }
        if (RefreshQueue.isBusy(start)) {
            return Result.cooling(1L);
        }

        StructureLevelAccess access = (StructureLevelAccess) (Object) start;
        long now = level.getGameTime();
        long last = access.difficultyplus$refreshedAt();
        int cooldown = config.cooldownTicks();
        if (last != 0L && cooldown > 0 && now - last < cooldown) {
            return Result.cooling((cooldown - (now - last)) / 20L);
        }

        List<BlockPos> keyBlocks = keyBlocksIn(level, start, config);
        if (!keyBlocks.isEmpty()) {
            return Result.blocked(keyBlocks);
        }
        return Result.available();
    }

    public static Result apply(ServerLevel level, StructureStart start, boolean reroll,
                               ItemStack relic) {
        return apply(level, start, reroll, relic, null);
    }

    public static Result apply(ServerLevel level, StructureStart start, boolean reroll,
                               ItemStack relic, Vec3 origin) {
        return apply(level, start, reroll, relic, origin, null);
    }

    public static Result apply(ServerLevel level, StructureStart start, boolean reroll,
                               ItemStack relic, Vec3 origin, RefreshQueue.Completion onDone) {
        Result check = inspect(level, start);
        if (!check.ready()) {
            return check;
        }

        GlobalData.StructureRefresh config = ConfigBridge.global().structure_refresh;
        StructureLevelAccess access = (StructureLevelAccess) (Object) start;
        Optional<StructureKey> key = keyOf(level, start);

        boolean rerollAllowed = reroll && !rerollBlacklisted(level, start, config);
        if (rerollAllowed) {
            if (relic != null) {
                access.difficultyplus$setBias(SocketData.biasOf(relic));
                BiasDefinition definition = SocketData.definitionOf(relic);
                access.difficultyplus$setPierce(
                        (definition.pierceFloor() ? 1 : 0) | (definition.pierceCeiling() ? 2 : 0));
            }
            access.difficultyplus$setLevel(StructureLevelAccess.NONE);
            access.difficultyplus$setSalt(access.difficultyplus$salt() + 1);
            access.difficultyplus$setSource(StructureLevelAccess.SOURCE_REROLL);
            key.ifPresent(k -> LevelStore.get(level).forget(k));
        }

        String ownerKey = config.clear_owned_entities && key.isPresent()
                ? key.get().encode()
                : null;
        Vec3 from = origin != null ? origin
                : Vec3.atCenterOf(start.getBoundingBox().getCenter());

        int chunks = place(level, start, ownerKey, from, onDone);

        access.difficultyplus$setPlacements(null);
        access.difficultyplus$setRefreshedAt(level.getGameTime());

        if (rerollAllowed) {
            ZoneResolver.invalidate();
            ZoneChunkSync.resend(level, start.getBoundingBox());
        }

        return Result.done(chunks, 0, 0);
    }

    private static boolean rerollBlacklisted(ServerLevel level, StructureStart start,
                                             GlobalData.StructureRefresh config) {
        return matchesAny(level, start, config.reroll_blacklist);
    }

    private static boolean blacklisted(ServerLevel level, StructureStart start,
                                       GlobalData.StructureRefresh config) {
        return matchesAny(level, start, config.blacklist);
    }

    private static boolean matchesAny(ServerLevel level, StructureStart start,
                                      List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Holder<Structure> holder = registry.wrapAsHolder(start.getStructure());
        for (String entry : patterns) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            if (PatternMatching.universalMatch(holder, Registries.STRUCTURE, entry)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canReroll(ServerLevel level, StructureStart start) {
        GlobalData.StructureRefresh config = ConfigBridge.global().structure_refresh;
        if (rerollBlacklisted(level, start, config)) {
            return false;
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceLocation id = registry.getKey(start.getStructure());
        return LevelRoll.isRerollable(level, start.getBoundingBox().getCenter(), id);
    }

    public static boolean clearCooldown(StructureStart start) {
        StructureLevelAccess access = (StructureLevelAccess) (Object) start;
        if (access.difficultyplus$refreshedAt() == 0L) {
            return false;
        }
        access.difficultyplus$setRefreshedAt(0L);
        return true;
    }

    private static int place(ServerLevel level, StructureStart start, String ownerKey,
                             Vec3 origin, RefreshQueue.Completion onDone) {
        BoundingBox box = start.getBoundingBox();
        RandomSource random = RandomSource.create(level.getSeed()
                ^ box.getCenter().asLong()
                ^ (level.getGameTime() * 0x9E3779B97F4A7C15L));

        LongSet occupied = new LongOpenHashSet();
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox pieceBox = piece.getBoundingBox();
            for (int x = pieceBox.minX() >> 4; x <= pieceBox.maxX() >> 4; x++) {
                for (int z = pieceBox.minZ() >> 4; z <= pieceBox.maxZ() >> 4; z++) {
                    occupied.add(ChunkPos.asLong(x, z));
                }
            }
        }

        LongList ordered = new LongArrayList(occupied);
        RefreshQueue.submit(level, start, ordered, random,
                PlacementTracker.placementsOf(start), ownerKey, origin, onDone);
        return ordered.size();
    }

    static boolean isProtected(Entity entity, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        Holder<EntityType<?>> holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType());
        for (String entry : patterns) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            if (PatternMatching.universalMatch(holder, Registries.ENTITY_TYPE, entry)) {
                return true;
            }
        }
        return false;
    }

    private static AABB aabbOf(BoundingBox box) {
        return new AABB(
                box.minX(), box.minY(), box.minZ(),
                box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
    }

    private static List<BlockPos> keyBlocksIn(ServerLevel level, StructureStart start,
                                              GlobalData.StructureRefresh config) {
        List<BlockPos> found = new ArrayList<>();
        for (long packed : PlacementTracker.placementsOf(start)) {
            BlockPos pos = BlockPos.of(packed);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (isKeyBlock(state, config)) {
                found.add(pos);
            }
        }
        return found;
    }

    private static boolean isKeyBlock(BlockState state, GlobalData.StructureRefresh config) {
        if (config.key_blocks == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        for (String entry : config.key_blocks) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            if (entry.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null && state.is(TagKey.create(Registries.BLOCK, tagId))) {
                    return true;
                }
                continue;
            }
            if (id != null && id.toString().equals(entry)) {
                return true;
            }
        }
        return false;
    }

    public static Optional<StructureKey> keyOf(ServerLevel level, StructureStart start) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceLocation id = registry.getKey(start.getStructure());
        return id == null
                ? Optional.empty()
                : Optional.of(StructureKey.of(id, start.getBoundingBox().getCenter()));
    }

    public record Result(State state, long secondsLeft, List<BlockPos> keyBlocks,
                         int chunks, int entitiesCleared, int blocksRemoved) {

        public enum State { READY, DONE, DISABLED, COOLING, BLOCKED, EXCLUDED }

        public boolean ready() {
            return state == State.READY;
        }

        static Result available() {
            return new Result(State.READY, 0L, List.of(), 0, 0, 0);
        }

        static Result done(int chunks, int cleared, int removed) {
            return new Result(State.DONE, 0L, List.of(), chunks, cleared, removed);
        }

        static Result disabled() {
            return new Result(State.DISABLED, 0L, List.of(), 0, 0, 0);
        }

        static Result excluded() {
            return new Result(State.EXCLUDED, 0L, List.of(), 0, 0, 0);
        }

        static Result cooling(long secondsLeft) {
            return new Result(State.COOLING, secondsLeft, List.of(), 0, 0, 0);
        }

        static Result blocked(List<BlockPos> keyBlocks) {
            return new Result(State.BLOCKED, 0L, List.copyOf(keyBlocks), 0, 0, 0);
        }
    }
}
