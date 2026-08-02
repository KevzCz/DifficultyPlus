package net.pixeldreamstudios.difficultyplus.mixin.client;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = MultiElementListEntry.class, remap = false)
public interface MultiElementListEntryAccessor {

    @Accessor("entries")
    List<AbstractConfigListEntry<?>> difficultyplus$entries();

    @Accessor("children")
    List<Object> difficultyplus$children();
}
