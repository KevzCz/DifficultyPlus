package net.pixeldreamstudios.difficultyplus.client.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import net.pixeldreamstudios.difficultyplus.mixin.client.MultiElementListEntryAccessor;

import java.util.ArrayList;
import java.util.List;

public final class CellRebuild {
    private CellRebuild() {}

    public static <T> void replace(MultiElementListEntry<T> cell,
                                   List<AbstractConfigListEntry<?>> replacement) {
        if (cell == null) {
            return;
        }
        MultiElementListEntryAccessor access = (MultiElementListEntryAccessor) cell;
        List<AbstractConfigListEntry<?>> entries = access.difficultyplus$entries();
        List<Object> children = access.difficultyplus$children();
        if (entries == null || children == null) {
            return;
        }

        AbstractConfigScreen screen = cell.getConfigScreen();
        if (screen != null) {
            for (AbstractConfigListEntry<?> entry : replacement) {
                entry.setScreen(screen);
            }
        }

        Object header = children.isEmpty() ? null : children.get(0);

        entries.clear();
        entries.addAll(replacement);

        children.clear();
        if (header != null) {
            children.add(header);
        }
        children.addAll(replacement);

        List<ReferenceProvider<?>> references = new ArrayList<>(replacement);
        cell.setReferenceProviderEntries(references);
    }
}
