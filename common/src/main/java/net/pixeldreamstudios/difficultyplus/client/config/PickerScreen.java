package net.pixeldreamstudios.difficultyplus.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PickerScreen extends Screen {

    private final Screen parent;
    private final List<String> all;
    private final String initial;
    private final Consumer<String> onPick;

    private EditBox search;
    private Entries list;
    private int shown;

    public PickerScreen(Screen parent, String title, List<String> options, String initial,
                        Consumer<String> onPick) {
        super(Component.literal(title));
        this.parent = parent;
        this.all = List.copyOf(options);
        this.initial = initial == null ? "" : initial;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        search = new EditBox(font, width / 2 - 150, 32, 300, 20,
                Component.literal("Search"));
        search.setMaxLength(256);
        search.setValue(initial);
        search.setResponder(text -> refilter(text));
        addRenderableWidget(search);
        setInitialFocus(search);

        list = new Entries(width / 2 - 150, 60, 300, height - 110);
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(Component.literal("Use typed value"), b -> {
            onPick.accept(search.getValue());
            onClose();
        }).bounds(width / 2 - 154, height - 44, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(width / 2 + 4, height - 44, 150, 20).build());

        refilter(initial);
    }

    private void refilter(String term) {
        String needle = term == null ? "" : term.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : all) {
            if (needle.isEmpty() || option.toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(option);
            }
        }
        shown = matches.size();
        list.reset(matches);
    }

    private void choose(String value) {
        onPick.accept(value);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal(shown + " match(es) of " + all.size()),
                width / 2, height - 58, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private class Entries extends ObjectSelectionList<Entries.Row> {

        Entries(int x, int y, int listWidth, int listHeight) {
            super(PickerScreen.this.minecraft, listWidth, listHeight, y, 20);
            setX(x);
        }

        void reset(List<String> values) {
            clearEntries();
            for (String value : values) {
                addEntry(new Row(value));
            }
            setScrollAmount(0);
        }

        @Override
        public int getRowWidth() {
            return width - 12;
        }

        class Row extends ObjectSelectionList.Entry<Row> {

            private final String value;

            Row(String value) {
                this.value = value;
            }

            @Override
            public Component getNarration() {
                return Component.literal(value);
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int rowWidth,
                               int rowHeight, int mouseX, int mouseY, boolean hovered,
                               float delta) {
                graphics.drawString(PickerScreen.this.font, value, left + 4, top + 6,
                        hovered ? 0xFFFFA0 : 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    PickerScreen.this.choose(value);
                    return true;
                }
                return false;
            }
        }
    }
}
