package net.pixeldreamstudios.difficultyplus.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DifficultyPlusEvents {
    private DifficultyPlusEvents() {}

    public static final Event<ZoneTransition> ZONE_ENTERED = new Event<>();
    public static final Event<ZoneTransition> ZONE_EXITED = new Event<>();

    public static final Event<LevelResolved> LEVEL_RESOLVED = new Event<>();

    public static final Event<ZoneEffectApplied> ZONE_EFFECT_APPLIED = new Event<>();

    public static final Event<DamageResisted> DAMAGE_RESISTED = new Event<>();

    @FunctionalInterface
    public interface ZoneTransition {
        void onTransition(LivingEntity entity, ZoneContext context);
    }

    @FunctionalInterface
    public interface LevelResolved {
        void onResolved(StructureContext context, MutableLevel level);
    }

    @FunctionalInterface
    public interface ZoneEffectApplied {
        boolean allow(LivingEntity entity, ZoneContext context, ZoneEffect effect);
    }

    @FunctionalInterface
    public interface DamageResisted {
        void onResisted(LivingEntity target, DamageSource source, float before, float after);
    }

    public static final class MutableLevel {
        private int value;
        private boolean overridden;

        public MutableLevel(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        public void set(int newValue) {
            this.value = newValue;
            this.overridden = true;
        }

        public boolean isOverridden() {
            return overridden;
        }
    }

    public static final class Event<T> {
        private final List<T> listeners = new ArrayList<>();

        public void register(T listener) {
            listeners.add(listener);
        }

        public boolean isListened() {
            return !listeners.isEmpty();
        }

        public void invoke(Consumer<T> action) {
            for (int i = 0; i < listeners.size(); i++) {
                action.accept(listeners.get(i));
            }
        }

        public List<T> listeners() {
            return listeners;
        }
    }
}
