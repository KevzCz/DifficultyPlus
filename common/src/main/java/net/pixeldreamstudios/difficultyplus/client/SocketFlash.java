package net.pixeldreamstudios.difficultyplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.difficultyplus.item.GemKind;
import net.pixeldreamstudios.difficultyplus.item.SocketData;
import net.pixeldreamstudios.difficultyplus.item.SocketedSkullItem;

public final class SocketFlash {
    private SocketFlash() {}

    private static final long DURATION = 14L;

    public static void render(GuiGraphics graphics, Slot slot) {
        ItemStack stack = slot.getItem();
        if (!(stack.getItem() instanceof SocketedSkullItem skull)) {
            return;
        }
        long socketedAt = SocketData.socketedAt(stack);
        if (socketedAt == 0L) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        long elapsed = client.level.getGameTime() - socketedAt;
        if (elapsed < 0 || elapsed >= DURATION) {
            return;
        }

        float progress = elapsed / (float) DURATION;
        int alpha = (int) ((1.0f - progress) * 200f);
        if (alpha <= 0) {
            return;
        }

        int colour = skull.kind().glowColour() & 0x00FFFFFF;
        int x = slot.x;
        int y = slot.y;

        int inset = Math.round(progress * 7f);
        graphics.fillGradient(x - 1 + inset, y - 1 + inset,
                x + 17 - inset, y + 17 - inset,
                (alpha << 24) | colour, ((alpha / 2) << 24) | colour);

        int ring = Math.round(progress * 9f);
        int ringAlpha = (int) ((1.0f - progress) * 255f);
        graphics.fill(x + 8 - ring, y - 1, x + 8 + ring, y, (ringAlpha << 24) | colour);
        graphics.fill(x + 8 - ring, y + 16, x + 8 + ring, y + 17, (ringAlpha << 24) | colour);
        graphics.fill(x - 1, y + 8 - ring, x, y + 8 + ring, (ringAlpha << 24) | colour);
        graphics.fill(x + 16, y + 8 - ring, x + 17, y + 8 + ring, (ringAlpha << 24) | colour);
    }

    public static int colourOf(GemKind kind) {
        return kind.glowColour();
    }
}
