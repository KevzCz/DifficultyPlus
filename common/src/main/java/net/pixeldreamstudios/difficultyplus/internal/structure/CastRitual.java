package net.pixeldreamstudios.difficultyplus.internal.structure;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.pixeldreamstudios.difficultyplus.item.DPItems;
import net.pixeldreamstudios.difficultyplus.item.GemKind;
import net.pixeldreamstudios.difficultyplus.item.SocketedSkullItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CastRitual {
    private CastRitual() {}

    private static final int SHARDS = 4;

    private static final double RING_RADIUS = 1.6;

    private static final double SKULL_RISE = 2.2;

    private static final int FLIGHT_MIN_TICKS = 24;

    private static final int FLIGHT_MAX_TICKS = 70;

    private static final double TRACKING_REACH = 100.0;

    private static final double READOUT_LIFT = 2.9;

    private static final int SETTLE_TICKS = 8;

    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

    private static final class Session {
        StructureStart target;
        GemKind kind;
        InteractionHand hand;
        Display.ItemDisplay skull;
        final List<Display.ItemDisplay> shards = new ArrayList<>();
        Display.TextDisplay readout;
        float startHealth;
        int totalTicks;
        int elapsed;
        ItemStack held;
        boolean consumed;
    }

    private record Arc(Display.ItemDisplay shard, Vec3 from, Vec3 to, double peak,
                       GemKind kind) {}

    private static final class Flight {
        final List<Arc> arcs = new ArrayList<>();
        Display.ItemDisplay skull;
        int startTick;
        int duration;
        ServerPlayer caster;
        StructureStart target;
        ItemStack relic;
        boolean reroll;
        boolean applied;
        boolean consumed;
    }

    private static final List<Flight> FLIGHTS = new ArrayList<>();

    public static boolean isCasting(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }


    public static void begin(ServerPlayer player, ServerLevel level, StructureStart target,
                             SocketedSkullItem item, InteractionHand hand) {
        abort(player, false);

        Session session = new Session();
        session.target = target;
        session.kind = item.kind();
        session.hand = hand;
        session.startHealth = player.getHealth();
        session.totalTicks = item.castTicks();

        ItemStack inHand = player.getItemInHand(hand);
        session.held = inHand.copyWithCount(1);
        session.consumed = !player.isCreative();
        if (session.consumed) {
            inHand.shrink(1);
        }

        Vec3 centre = player.position().add(0.0, player.getEyeHeight() * 0.6, 0.0);
        session.skull = spawnSkull(level, centre, session.held);
        for (int i = 0; i < SHARDS; i++) {
            double angle = (Math.PI * 2.0 / SHARDS) * i;
            Vec3 at = centre.add(Math.cos(angle) * RING_RADIUS, 0.0, Math.sin(angle) * RING_RADIUS);
            Display.ItemDisplay shard = spawnShard(level, at, session.kind, i);
            if (shard != null) {
                session.shards.add(shard);
            }
        }
        session.readout = spawnReadout(level, centre.add(0.0, READOUT_LIFT, 0.0));

        ACTIVE.put(player.getUUID(), session);
        ((CastingAccess) player).difficultyplus$setCasting(true);
        StructureOutline.lock(player, level, target, session.kind.glowColour());

        level.playSound(null, player.blockPosition(), SoundEvents.VAULT_ACTIVATE,
                SoundSource.PLAYERS, 0.9f, 0.8f);
    }

    public static void tickCasts(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            Session session = ACTIVE.get(player.getUUID());
            if (session != null) {
                session.elapsed++;
                tick(player, level, session.elapsed);
                if (ACTIVE.containsKey(player.getUUID()) && session.elapsed >= session.totalTicks) {
                    complete(player, level);
                }
            }
        }
    }

    private static void tick(ServerPlayer player, ServerLevel level, int elapsed) {
        Session session = ACTIVE.get(player.getUUID());
        if (session == null) {
            return;
        }

        if (player.getHealth() < session.startHealth - 0.01f) {
            abort(player, true);
            return;
        }
        if (player.isShiftKeyDown()) {
            abort(player, true);
            return;
        }
        if (!stillValid(level, player, session)) {
            abort(player, true);
            return;
        }

        float progress = Math.min(1.0f, elapsed / (float) session.totalTicks);
        Vec3 centre = player.position().add(0.0, player.getEyeHeight() * 0.6, 0.0);

        if (session.skull != null) {
            session.skull.moveTo(centre.x, centre.y + SKULL_RISE * progress, centre.z, 0f, 0f);
            applyTransform(session.skull, 1.0f + 0.35f * progress, progress * 720f, 3);
        }

        for (int i = 0; i < session.shards.size(); i++) {
            Display.ItemDisplay shard = session.shards.get(i);
            double angle = (Math.PI * 2.0 / SHARDS) * i + progress * Math.PI * 4.0;
            double radius = RING_RADIUS * (0.4 + 0.6 * progress);
            double height = SKULL_RISE * progress;
            shard.moveTo(centre.x + Math.cos(angle) * radius,
                    centre.y + height,
                    centre.z + Math.sin(angle) * radius, 0f, 0f);
        }

        if (session.readout != null && elapsed % 4 == 0) {
            session.readout.moveTo(centre.x, centre.y + READOUT_LIFT, centre.z, 0f, 0f);
            writeText(session.readout, bar(progress));
        }

        if (elapsed % 8 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.5f, 0.9f + progress * 0.7f);
        }
    }

    private static void complete(ServerPlayer player, ServerLevel level) {
        Session session = ACTIVE.remove(player.getUUID());
        if (session == null) {
            return;
        }
        ((CastingAccess) player).difficultyplus$setCasting(false);
        StructureOutline.clear(player);

        Flight flight = launchToCorners(level, session);
        flight.caster = player;
        flight.target = session.target;
        flight.relic = session.held;
        flight.reroll = session.kind.rerolls();
        flight.consumed = session.consumed;

        level.playSound(null, player.blockPosition(), SoundEvents.VAULT_OPEN_SHUTTER,
                SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    private static void applyRefresh(ServerLevel level, Flight flight) {
        ServerPlayer player = flight.caster;
        if (player == null || flight.target == null) {
            return;
        }

        StructureRefresh.Result result = StructureRefresh.apply(
                level, flight.target, flight.reroll, flight.relic, player.position(),
                (chunks, blocks, entities) -> {
                    level.playSound(null, player.blockPosition(),
                            SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.0f, 1.2f);
                    player.displayClientMessage(Component.translatable(
                                    "message.difficultyplus.refreshed", chunks, blocks, entities)
                            .withStyle(ChatFormatting.GREEN), true);
                });

        if (result.state() == StructureRefresh.Result.State.DONE) {
            if (flight.consumed) {
                returnRemainingUses(player, flight.relic);
            }
        } else {
            player.displayClientMessage(Component.translatable("message.difficultyplus.failed")
                    .withStyle(ChatFormatting.RED), true);
            if (flight.consumed) {
                giveBack(player, flight.relic);
            }
        }
    }

    private static void returnRemainingUses(ServerPlayer player, ItemStack relic) {
        if (relic == null || relic.isEmpty()) {
            return;
        }
        ItemStack survivor = relic.copy();
        int damage = survivor.getDamageValue() + 1;
        if (damage >= survivor.getMaxDamage()) {
            return;
        }
        survivor.setDamageValue(damage);
        giveBack(player, survivor);
    }

    private static void giveBack(ServerPlayer player, ItemStack relic) {
        if (relic == null || relic.isEmpty()) {
            return;
        }
        ItemStack copy = relic.copy();
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
    }

    public static void abort(ServerPlayer player, boolean announce) {
        Session session = ACTIVE.remove(player.getUUID());
        if (session == null) {
            return;
        }
        ((CastingAccess) player).difficultyplus$setCasting(false);
        discard(session);
        StructureOutline.unlock(player);
        if (session.consumed) {
            giveBack(player, session.held);
        }

        if (announce) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.VAULT_DEACTIVATE,
                    SoundSource.PLAYERS, 0.8f, 0.7f);
            player.displayClientMessage(
                    Component.translatable("message.difficultyplus.interrupted")
                            .withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static void reportRefusal(ServerPlayer player, ServerLevel level, StructureStart target,
                                     StructureRefresh.Result result) {
        Component message = switch (result.state()) {
            case DISABLED -> Component.translatable("message.difficultyplus.disabled");
            case EXCLUDED -> Component.translatable("message.difficultyplus.blacklisted");
            case COOLING -> Component.translatable("message.difficultyplus.cooling",
                    result.secondsLeft());
            case BLOCKED -> Component.translatable("message.difficultyplus.blocked",
                    result.keyBlocks().size());
            default -> Component.translatable("message.difficultyplus.failed");
        };
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);

        if (result.state() == StructureRefresh.Result.State.BLOCKED) {
            BlockHighlight.outline(level, player, result.keyBlocks());
        }
        level.playSound(null, player.blockPosition(), SoundEvents.VAULT_DEACTIVATE,
                SoundSource.PLAYERS, 0.7f, 0.6f);
    }

    private static Flight launchToCorners(ServerLevel level, Session session) {
        BoundingBox box = session.target.getBoundingBox();
        double[][] corners = {
                {box.minX(), box.minZ()},
                {box.maxX(), box.minZ()},
                {box.maxX(), box.maxZ()},
                {box.minX(), box.maxZ()}
        };

        Flight flight = new Flight();
        flight.skull = session.skull;
        flight.startTick = level.getServer().getTickCount();

        double longest = 0.0;
        for (int i = 0; i < session.shards.size(); i++) {
            Display.ItemDisplay shard = session.shards.get(i);
            double[] corner = corners[i % corners.length];
            Vec3 from = shard.position();
            Vec3 to = clampReach(from, new Vec3(corner[0], box.maxY() + 2.0, corner[1]));

            double reach = from.distanceTo(to);
            longest = Math.max(longest, reach);
            double peak = Math.max(6.0, Math.min(reach * 0.35, 40.0));

            CompoundTag tag = new CompoundTag();
            shard.saveWithoutId(tag);
            tag.putInt("teleport_duration", 3);
            tag.put("transformation", transform(1.3f, 0f));
            tag.putInt("interpolation_duration", SETTLE_TICKS);
            tag.putInt("start_interpolation", 0);
            shard.load(tag);

            flight.arcs.add(new Arc(shard, from, to, peak, session.kind));
        }

        flight.duration = (int) Math.max(FLIGHT_MIN_TICKS,
                Math.min(FLIGHT_MAX_TICKS, longest * 0.35));

        if (session.readout != null) {
            session.readout.discard();
        }
        FLIGHTS.add(flight);
        return flight;
    }

    private static void trail(ServerLevel level, Arc arc, Vec3 at) {
        int rgb = arc.kind().glowColour();
        Vector3f colour = new Vector3f(
                ((rgb >> 16) & 0xFF) / 255f,
                ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f);

        level.sendParticles(new DustParticleOptions(colour, 1.1f),
                at.x, at.y, at.z, 3, 0.08, 0.08, 0.08, 0.0);
        level.sendParticles(ParticleTypes.END_ROD,
                at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
    }

    private static Vec3 clampReach(Vec3 from, Vec3 to) {
        double reach = from.distanceTo(to);
        if (reach <= TRACKING_REACH) {
            return to;
        }
        return from.add(to.subtract(from).normalize().scale(TRACKING_REACH));
    }

    public static void tickFlights(ServerLevel level) {
        if (FLIGHTS.isEmpty()) {
            return;
        }
        int now = level.getServer().getTickCount();
        FLIGHTS.removeIf(flight -> {
            int elapsed = now - flight.startTick;

            if (elapsed < SETTLE_TICKS) {
                return false;
            }

            int flying = elapsed - SETTLE_TICKS;
            if (flying >= flight.duration && !flight.applied) {
                flight.applied = true;
                applyRefresh(level, flight);
            }
            if (flying > flight.duration) {
                flight.arcs.forEach(arc -> arc.shard().discard());
                if (flight.skull != null) {
                    flight.skull.discard();
                }
                return true;
            }

            float progress = flying / (float) flight.duration;
            float eased = progress * progress * (3.0f - 2.0f * progress);
            double lift = Math.sin(Math.PI * progress);

            for (Arc arc : flight.arcs) {
                Vec3 flat = arc.from().lerp(arc.to(), eased);
                Vec3 at = new Vec3(flat.x, flat.y + arc.peak() * lift, flat.z);
                arc.shard().moveTo(at.x, at.y, at.z, 0f, 0f);
                trail(level, arc, at);
            }
            if (flight.skull != null) {
                applyTransform(flight.skull, Math.max(0.05f, 1.35f - progress * 1.35f),
                        0f, 3);
            }
            return false;
        });
    }

    private static boolean stillValid(ServerLevel level, ServerPlayer player, Session session) {
        if (session.target.getBoundingBox().isInside(player.blockPosition())) {
            return true;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getViewVector(1.0f).scale(StructureTargeting.RAY_REACH));
        return StructureTargeting.aabbOf(session.target.getBoundingBox())
                .clip(from, to).isPresent();
    }

    private static Display.ItemDisplay spawnSkull(ServerLevel level, Vec3 at, ItemStack held) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display == null) {
            return null;
        }
        display.moveTo(at.x, at.y, at.z, 0f, 0f);
        CompoundTag tag = new CompoundTag();
        display.saveWithoutId(tag);
        CompoundTag item = new CompoundTag();
        item.putString("id", held.getItem().builtInRegistryHolder().key().location().toString());
        item.putInt("count", 1);
        tag.put("item", item);
        tag.putString("item_display", "ground");
        tag.putFloat("view_range", 4.0f);
        tag.putInt("teleport_duration", 2);
        tag.put("brightness", brightness());
        display.load(tag);
        return level.addFreshEntity(display) ? display : null;
    }

    private static Display.ItemDisplay spawnShard(ServerLevel level, Vec3 at, GemKind kind,
                                                  int index) {
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display == null) {
            return null;
        }
        display.moveTo(at.x, at.y, at.z, 0f, 0f);

        ResourceLocation shard = DPItems.shardsFor(kind).get(index % 4).getId();
        CompoundTag tag = new CompoundTag();
        display.saveWithoutId(tag);
        CompoundTag item = new CompoundTag();
        item.putString("id", shard.toString());
        item.putInt("count", 1);
        tag.put("item", item);
        tag.putString("item_display", "ground");
        tag.putString("billboard", "center");
        tag.putFloat("view_range", 4.0f);
        tag.putInt("teleport_duration", 2);
        tag.put("brightness", brightness());
        tag.put("transformation", transform(0.6f, 0f));
        display.load(tag);
        display.setGlowingTag(true);
        return level.addFreshEntity(display) ? display : null;
    }

    private static Display.TextDisplay spawnReadout(ServerLevel level, Vec3 at) {
        Display.TextDisplay display = EntityType.TEXT_DISPLAY.create(level);
        if (display == null) {
            return null;
        }
        display.moveTo(at.x, at.y, at.z, 0f, 0f);
        CompoundTag tag = new CompoundTag();
        display.saveWithoutId(tag);
        tag.putString("billboard", "center");
        tag.putFloat("view_range", 4.0f);
        tag.putInt("background", 0);
        tag.putInt("teleport_duration", 2);
        display.load(tag);
        return level.addFreshEntity(display) ? display : null;
    }

    private static void writeText(Display.TextDisplay display, String text) {
        CompoundTag tag = new CompoundTag();
        display.saveWithoutId(tag);
        tag.putString("text", Component.Serializer.toJson(
                Component.literal(text), display.registryAccess()));
        display.load(tag);
    }

    private static void applyTransform(Display display, float scale, float yawDegrees,
                                       int interpolation) {
        CompoundTag tag = new CompoundTag();
        display.saveWithoutId(tag);
        tag.put("transformation", transform(scale, yawDegrees));
        tag.putInt("interpolation_duration", interpolation);
        tag.putInt("start_interpolation", 0);
        display.load(tag);
    }

    private static CompoundTag transform(float scale, float yawDegrees) {
        double radians = Math.toRadians(yawDegrees) * 0.5;
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);

        CompoundTag transformation = new CompoundTag();
        transformation.put("translation", floats(0f, 0f, 0f));
        transformation.put("scale", floats(scale, scale, scale));
        transformation.put("left_rotation", floats(0f, sin, 0f, cos));
        transformation.put("right_rotation", floats(0f, 0f, 0f, 1f));
        return transformation;
    }

    private static CompoundTag brightness() {
        CompoundTag brightness = new CompoundTag();
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        return brightness;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float value : values) {
            list.add(FloatTag.valueOf(value));
        }
        return list;
    }

    private static String bar(float progress) {
        int filled = Math.round(progress * 7f);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            text.append(i < filled ? '▰' : '▱');
        }
        return text.toString();
    }

    private static void discard(Session session) {
        if (session.skull != null) {
            session.skull.discard();
        }
        session.shards.forEach(Display.ItemDisplay::discard);
        if (session.readout != null) {
            session.readout.discard();
        }
    }
}
