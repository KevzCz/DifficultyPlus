package net.pixeldreamstudios.difficultyplus.api;

import net.minecraft.world.entity.LivingEntity;

import java.util.OptionalInt;

@FunctionalInterface
public interface OwnedLevelSource {

    OptionalInt levelOf(LivingEntity summon);
}
