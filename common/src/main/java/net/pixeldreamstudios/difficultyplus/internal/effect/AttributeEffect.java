package net.pixeldreamstudios.difficultyplus.internal.effect;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;
import net.pixeldreamstudios.difficultyplus.api.ZoneEffect;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import java.util.Locale;

public final class AttributeEffect implements ZoneEffect {

    private final Holder<Attribute> attribute;
    private final AttributeModifier.Operation operation;
    private final double value;

    private AttributeEffect(Holder<Attribute> attribute, AttributeModifier.Operation operation, double value) {
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
    }

    public static ZoneEffect fromJson(JsonObject json) {
        if (!json.has("attribute")) {
            DifficultyPlus.LOGGER.warn("Zone effect 'difficultyplus:attribute' is missing 'attribute'");
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(json.get("attribute").getAsString());
        if (id == null) {
            DifficultyPlus.LOGGER.warn("Malformed attribute id '{}'", json.get("attribute").getAsString());
            return null;
        }
        Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(
                ResourceKey.create(BuiltInRegistries.ATTRIBUTE.key(), id)).orElse(null);
        if (holder == null) {
            DifficultyPlus.LOGGER.warn("Unknown attribute '{}' in zone effect — entry skipped", id);
            return null;
        }

        AttributeModifier.Operation operation = parseOperation(
                json.has("operation") ? json.get("operation").getAsString() : "ADD_VALUE");
        double value = json.has("value") ? json.get("value").getAsDouble() : 0d;
        return new AttributeEffect(holder, operation, value);
    }

    private static AttributeModifier.Operation parseOperation(String raw) {
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "ADD_MULTIPLIED_BASE", "MULTIPLY_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "ADD_MULTIPLIED_TOTAL", "MULTIPLY_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    @Override
    public void apply(LivingEntity entity, ZoneContext context) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        ResourceLocation id = modifierId(context);

        if (instance.getModifier(id) != null) {
            return;
        }
        instance.addTransientModifier(new AttributeModifier(id, value, operation));
    }

    @Override
    public void remove(LivingEntity entity, ZoneContext context) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(modifierId(context));
        }
    }

    private ResourceLocation modifierId(ZoneContext context) {
        String path = "zone/" + context.difficulty() + "/" + attribute.getRegisteredName()
                .replace(':', '.').replace('/', '.');
        return ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, path);
    }
}
