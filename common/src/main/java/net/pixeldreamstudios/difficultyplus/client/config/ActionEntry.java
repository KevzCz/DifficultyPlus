package net.pixeldreamstudios.difficultyplus.client.config;

import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class ActionEntry extends BooleanListEntry {

    private final Runnable action;
    private final Component face;
    private boolean lastSeen;

    public ActionEntry(Component fieldName, Component face, Runnable action) {
        super(fieldName, false, Component.translatable("text.cloth-config.reset_value"),
                () -> false, value -> {}, Optional::empty, false);
        this.action = action;
        this.face = face;
        this.lastSeen = false;
    }

    @Override
    public Component getYesNoText(boolean value) {
        return face;
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth,
                       int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        boolean current = Boolean.TRUE.equals(super.getValue());
        if (current != lastSeen) {
            lastSeen = current;
            action.run();
        }
        super.render(graphics, index, y, x, entryWidth, entryHeight,
                mouseX, mouseY, isHovered, delta);
    }
}
