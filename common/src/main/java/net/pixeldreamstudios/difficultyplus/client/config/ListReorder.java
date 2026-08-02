package net.pixeldreamstudios.difficultyplus.client.config;

import me.shedaniel.clothconfig2.gui.entries.BaseListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import net.pixeldreamstudios.difficultyplus.mixin.client.BaseListEntryAccessor;

import java.util.Collections;
import java.util.List;

public final class ListReorder {
    private ListReorder() {}

    public static void moveBy(BaseListEntry<?, ?, ?> entry, Object element, int delta) {
        if (entry == null) {
            return;
        }
        BaseListEntryAccessor accessor = (BaseListEntryAccessor) entry;
        List<Object> cells = accessor.difficultyplus$cells();

        int from = indexOf(cells, element);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= cells.size()) {
            return;
        }

        Collections.swap(cells, from, to);
    }

    private static int indexOf(List<Object> cells, Object element) {
        for (int i = 0; i < cells.size(); i++) {
            if (valueOf(cells.get(i)) == element) {
                return i;
            }
        }
        return -1;
    }

    private static Object valueOf(Object cell) {
        return cell instanceof NestedListListEntry.NestedListCell<?, ?> nested
                ? nested.getValue()
                : null;
    }
}
