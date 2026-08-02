package net.pixeldreamstudios.difficultyplus.client.config;

import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SearchableEntry extends StringListEntry {

    private final Supplier<List<String>> options;
    private final Button browse;

    public SearchableEntry(Component fieldName, String value, Consumer<String> setter,
                           Supplier<List<String>> options, Component tooltip) {
        super(fieldName, value == null ? "" : value,
                Component.translatable("text.cloth-config.reset_value"), () -> "",
                v -> setter.accept(v == null || v.isEmpty() ? null : v),
                tooltip == null ? Optional::empty : () -> Optional.of(new Component[]{tooltip}),
                false);
        this.options = options;
        this.browse = Button.builder(Component.literal("..."), b -> open())
                .bounds(0, 0, 20, 20)
                .build();
    }

    private void open() {
        Minecraft client = Minecraft.getInstance();
        List<String> available = options.get();
        client.setScreen(new PickerScreen(client.screen,
                getFieldName().getString(),
                available,
                getValue(),
                picked -> setValue(picked)));
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth,
                       int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight,
                mouseX, mouseY, isHovered, delta);
        browse.setY(y);
        browse.setX(x + entryWidth - 168);
        browse.active = isEditable();
        browse.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        List<GuiEventListener> all = new ArrayList<>(super.children());
        all.add(browse);
        return all;
    }
}
