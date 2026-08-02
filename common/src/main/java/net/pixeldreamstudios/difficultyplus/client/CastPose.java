package net.pixeldreamstudios.difficultyplus.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.difficultyplus.internal.structure.CastingAccess;

public final class CastPose {
    private CastPose() {}

    public static boolean isCasting(LivingEntity entity) {
        return entity instanceof Player player
                && ((CastingAccess) player).difficultyplus$casting();
    }

    public static void apply(HumanoidModel<?> model, LivingEntity entity, float age) {
        if (!isCasting(entity)) {
            return;
        }

        float sway = (float) Math.sin(age * 0.18f) * 0.15f;
        float lift = (float) Math.cos(age * 0.18f) * 0.10f;

        model.rightArm.xRot = -0.35f + lift;
        model.rightArm.yRot = -0.30f;
        model.rightArm.zRot = 1.35f + sway;

        model.leftArm.xRot = -0.35f - lift;
        model.leftArm.yRot = 0.30f;
        model.leftArm.zRot = -1.35f - sway;

        model.head.xRot = -0.45f;
    }
}
