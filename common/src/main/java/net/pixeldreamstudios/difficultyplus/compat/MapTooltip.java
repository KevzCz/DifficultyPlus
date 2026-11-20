package net.pixeldreamstudios.difficultyplus.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class MapTooltip {
    private MapTooltip() {}

    public record Style(int fill, int sidePad, int edgePad, int textTop, int lineHeight, int trailingGap,
                        int cursorX, int cursorY, int flipOverflow, int wrap) {}

    private static final int TEXT = 0xFFFFFF;
    private static final float DEPTH = 400f;

    public static void draw(GuiGraphics graphics, List<Component> text, int mouseX, int mouseY, Style style) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component line : text) {
            lines.addAll(font.split(line, style.wrap()));
        }
        if (lines.isEmpty()) {
            return;
        }
        int textWidth = 0;
        for (FormattedCharSequence line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        int boxWidth = textWidth + style.sidePad() * 2;
        int boxHeight = style.edgePad() * 2 + lines.size() * style.lineHeight() - style.trailingGap();
        int x = mouseX + style.cursorX();
        int y = mouseY + style.cursorY();
        int overflowX = x + boxWidth - graphics.guiWidth();
        if (overflowX > style.flipOverflow()) {
            x = mouseX - style.cursorX() - boxWidth;
        } else if (overflowX > 0) {
            x -= overflowX;
        }
        int overflowY = y + boxHeight - graphics.guiHeight();
        if (overflowY > boxHeight / 2) {
            y = mouseY - style.cursorY() - boxHeight;
        } else if (overflowY > 0) {
            y -= overflowY;
        }
        x = Math.max(0, x);
        y = Math.max(0, y);
        graphics.pose().pushPose();
        graphics.pose().translate(0f, 0f, DEPTH);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, style.fill());
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), x + style.sidePad(), y + style.textTop() + style.lineHeight() * i, TEXT);
        }
        graphics.pose().popPose();
    }
}
