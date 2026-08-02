package net.pixeldreamstudios.difficultyplus.mixin.client;

import me.shedaniel.clothconfig2.gui.entries.BaseListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = BaseListEntry.class, remap = false)
public interface BaseListEntryAccessor {

    @Accessor("cells")
    List<Object> difficultyplus$cells();

}
