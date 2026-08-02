package net.pixeldreamstudios.difficultyplus.client.config;

import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Consumer;

public class ReactiveChoiceEntry extends SelectionListEntry<String> {

    private final Runnable onChange;
    private final Consumer<String> live;
    private String lastSeen;

    public ReactiveChoiceEntry(Component fieldName, String[] values, String value,
                               Consumer<String> setter, Consumer<String> live,
                               Runnable onChange) {
        super(fieldName, values, value, Component.translatable("text.cloth-config.reset_value"),
                () -> values[0], setter, Component::literal, Optional::empty, false);
        this.onChange = onChange;
        this.live = live;
        this.lastSeen = value;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            applyIfChanged();
        }
        return handled;
    }

    private void applyIfChanged() {
        String current = getValue();
        if (current == null || current.equals(lastSeen)) {
            return;
        }
        lastSeen = current;
        if (live != null) {
            live.accept(current);
        }
        if (onChange != null) {
            onChange.run();
        }
    }
}
