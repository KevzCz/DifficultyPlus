package net.pixeldreamstudios.difficultyplus.internal.effect;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;
import net.pixeldreamstudios.difficultyplus.api.ZoneEffect;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;

public final class StatusEffectZoneEffect implements ZoneEffect {

    private final Holder<MobEffect> effect;
    private final int amplifier;
    private final boolean linger;
    private final int lingerDuration;

    private StatusEffectZoneEffect(Holder<MobEffect> effect, int amplifier, boolean linger, int lingerDuration) {
        this.effect = effect;
        this.amplifier = amplifier;
        this.linger = linger;
        this.lingerDuration = lingerDuration;
    }

    public static ZoneEffect fromJson(JsonObject json) {
        if (!json.has("effect")) {
            DifficultyPlus.LOGGER.warn("Zone effect 'difficultyplus:status_effect' is missing 'effect'");
            return null;
        }
        String raw = json.get("effect").getAsString();
        ResourceLocation id = ResourceLocation.tryParse(raw);
        Holder<MobEffect> holder = id == null ? null : BuiltInRegistries.MOB_EFFECT
                .getHolder(ResourceKey.create(BuiltInRegistries.MOB_EFFECT.key(), id)).orElse(null);
        if (holder == null) {
            DifficultyPlus.LOGGER.warn("Unknown mob effect '{}' in zone effect — entry skipped", raw);
            return null;
        }

        int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
        boolean linger = json.has("linger") && json.get("linger").getAsBoolean();
        int duration = json.has("duration") ? json.get("duration").getAsInt() : 0;
        return new StatusEffectZoneEffect(holder, amplifier, linger, duration);
    }

    @Override
    public void apply(LivingEntity entity, ZoneContext context) {

        int duration = linger && lingerDuration > 0
                ? lingerDuration
                : ConfigBridge.global().refreshDuration();

        MobEffectInstance existing = entity.getEffect(effect);

        if (existing != null && existing.getAmplifier() > amplifier) {
            return;
        }
        entity.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

}
