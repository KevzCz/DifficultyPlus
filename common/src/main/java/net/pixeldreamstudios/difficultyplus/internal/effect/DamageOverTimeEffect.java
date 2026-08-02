package net.pixeldreamstudios.difficultyplus.internal.effect;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;
import net.pixeldreamstudios.difficultyplus.api.ZoneEffect;

public final class DamageOverTimeEffect implements ZoneEffect {

    private final ResourceKey<DamageType> damageType;
    private final float amount;
    private final int interval;

    private DamageOverTimeEffect(ResourceKey<DamageType> damageType, float amount, int interval) {
        this.damageType = damageType;
        this.amount = amount;
        this.interval = interval;
    }

    public static ZoneEffect fromJson(JsonObject json) {
        String raw = json.has("damage_type") ? json.get("damage_type").getAsString() : "minecraft:magic";
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            DifficultyPlus.LOGGER.warn("Malformed damage type '{}' in zone effect — entry skipped", raw);
            return null;
        }
        float amount = json.has("amount") ? json.get("amount").getAsFloat() : 1f;
        int interval = json.has("interval") ? json.get("interval").getAsInt() : 20;
        return new DamageOverTimeEffect(ResourceKey.create(Registries.DAMAGE_TYPE, id), amount, Math.max(1, interval));
    }

    @Override
    public void apply(LivingEntity entity, ZoneContext context) {

    }

    @Override
    public void tick(LivingEntity entity, ZoneContext context) {
        if (amount <= 0f || entity.isDeadOrDying()) {
            return;
        }
        entity.level().registryAccess().registry(Registries.DAMAGE_TYPE)
                .flatMap(registry -> registry.getHolder(damageType))
                .ifPresent(holder -> entity.hurt(new DamageSource(holder), amount));
    }

    @Override
    public int tickInterval() {
        return interval;
    }
}
