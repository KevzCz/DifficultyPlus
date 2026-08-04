package net.pixeldreamstudios.difficultyplus.client.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.difficultyplus.config.LevelColours;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.List;
import java.util.Optional;

public final class Entries {
    private Entries() {}

    private static ConfigEntryBuilder builder() {
        return ConfigEntryBuilder.create();
    }

    public static Component text(String literal) {
        return Component.literal(literal);
    }

    public static AbstractConfigListEntry<?> string(String label,
                                                    String value,
                                                    Consumer<String> setter) {
        return builder().startStrField(text(label), value == null ? "" : value)
                .setDefaultValue("")

                .setSaveConsumer(v -> setter.accept(v == null || v.isEmpty() ? null : v))
                .build();
    }

    public static AbstractConfigListEntry<?> string(String label,
                                                    String value,
                                                    Consumer<String> setter,
                                                    String tooltip) {
        return builder().startStrField(text(label), value == null ? "" : value)
                .setDefaultValue("")
                .setTooltip(text(tooltip))
                .setSaveConsumer(v -> setter.accept(v == null || v.isEmpty() ? null : v))
                .build();
    }

    public static AbstractConfigListEntry<?> colour(String label, String value,
                                                    Consumer<String> setter) {
        return builder().startColorField(text(label), LevelColours.parse(value))
                .setDefaultValue(0xFFFFFF)
                .setSaveConsumer(rgb -> setter.accept(String.format("#%06X", rgb & 0xFFFFFF)))
                .build();
    }

    public static AbstractConfigListEntry<?> requiredString(String label,
                                                            String value,
                                                            Consumer<String> setter) {
        return builder().startStrField(text(label), value == null ? "" : value)
                .setDefaultValue("")
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<?> integer(String label, int value, Consumer<Integer> setter) {
        return builder().startIntField(text(label), value)
                .setDefaultValue(0)
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<?> decimal(String label, float value,
                                                     Consumer<Float> setter, String tooltip) {
        return builder().startFloatField(text(label), value)
                .setDefaultValue(0f)
                .setTooltip(text(tooltip))
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<?> decimal(String label, float value, Consumer<Float> setter) {
        return builder().startFloatField(text(label), value)
                .setDefaultValue(0f)
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<?> toggle(String label, boolean value, Consumer<Boolean> setter) {
        return builder().startBooleanToggle(text(label), value)
                .setDefaultValue(false)
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<?> toggle(String label, boolean value,
                                                    Consumer<Boolean> setter, String tooltip) {
        return builder().startBooleanToggle(text(label), value)
                .setDefaultValue(false)
                .setTooltip(text(tooltip))
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<?> optionalInt(String label,
                                                         Integer value,
                                                         Consumer<Integer> setter) {
        return builder().startStrField(text(label), value == null ? "" : String.valueOf(value))
                .setDefaultValue("")
                .setTooltip(text("Leave empty to follow the base level."))
                .setErrorSupplier(v -> v == null || v.isBlank() || v.matches("-?\\d+")
                        ? Optional.empty()
                        : Optional.of(text("Must be a whole number, or empty.")))
                .setSaveConsumer(v -> setter.accept(
                        v == null || v.isBlank() ? null : Integer.valueOf(v.trim())))
                .build();
    }

    public static AbstractConfigListEntry<?> optionalToggle(String label,
                                                            Boolean value,
                                                            Consumer<Boolean> setter) {
        String current = value == null ? "inherit" : String.valueOf(value);
        return builder().startSelector(text(label),
                        new String[]{"inherit", "true", "false"}, current)
                .setDefaultValue("inherit")
                .setSaveConsumer(v -> setter.accept("inherit".equals(v) ? null : Boolean.valueOf(v)))
                .build();
    }

    public static AbstractConfigListEntry<?> optionalDouble(String label,
                                                            Double value,
                                                            Consumer<Double> setter) {
        return builder().startStrField(text(label), value == null ? "" : String.valueOf(value))
                .setDefaultValue("")
                .setTooltip(text("Leave empty to leave unset."))
                .setErrorSupplier(v -> {
                    if (v == null || v.isBlank()) {
                        return Optional.empty();
                    }
                    try {
                        Double.parseDouble(v.trim());
                        return Optional.empty();
                    } catch (NumberFormatException e) {
                        return Optional.of(text("Must be a number, or empty."));
                    }
                })
                .setSaveConsumer(v -> setter.accept(
                        v == null || v.isBlank() ? null : Double.valueOf(v.trim())))
                .build();
    }

    public static <E extends Enum<E>> AbstractConfigListEntry<?> selector(String label,
                                                                          E[] values,
                                                                          E value,
                                                                          Consumer<E> setter) {
        return builder().startSelector(text(label), values, value == null ? values[0] : value)
                .setDefaultValue(values[0])
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<?> choice(String label,
                                                    List<String> choices,
                                                    String value,
                                                    Consumer<String> setter) {
        List<String> options = new ArrayList<>(choices);
        if (value != null && !value.isEmpty() && !options.contains(value)) {
            options.add(0, value);
        }
        if (options.isEmpty()) {
            return requiredString(label, value, setter);
        }
        String current = value == null || value.isEmpty() ? options.get(0) : value;
        return builder().startSelector(text(label), options.toArray(new String[0]), current)
                .setDefaultValue(options.get(0))
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<?> reactiveChoice(String label,
                                                          List<String> choices,
                                                          String value,
                                                          Consumer<String> setter) {
        List<String> options = new ArrayList<>(choices);
        if (value != null && !value.isEmpty() && !options.contains(value)) {
            options.add(0, value);
        }
        if (options.isEmpty()) {
            return requiredString(label, value, setter);
        }
        String current = value == null || value.isEmpty() ? options.get(0) : value;
        Runnable rebuild = rebuildHook();
        return new ReactiveChoiceEntry(text(label), options.toArray(new String[0]), current,
                setter, setter, rebuild);
    }

    public static AbstractConfigListEntry<?> searchable(String label,
                                                       Supplier<List<String>> suggestions,
                                                       String value,
                                                       Consumer<String> setter) {
        return searchable(label, suggestions, value, setter, null);
    }

    public static AbstractConfigListEntry<?> searchable(String label,
                                                       Supplier<List<String>> suggestions,
                                                       String value,
                                                       Consumer<String> setter,
                                                       String tooltip) {
        return new SearchableEntry(text(label), value, setter, suggestions,
                tooltip == null ? null : text(tooltip));
    }

    public static AbstractConfigListEntry<?> stringList(String label,
                                                       List<String> values,
                                                       Consumer<List<String>> setter) {
        return builder().startStrList(text(label),
                        values == null ? new ArrayList<>() : new ArrayList<>(values))
                .setDefaultValue(ArrayList::new)
                .setExpanded(false)
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<?> group(String label, List<AbstractConfigListEntry<?>> children) {
        var sub = builder().startSubCategory(text(label));
        for (AbstractConfigListEntry<?> child : children) {
            sub.add(child);
        }
        return sub.setExpanded(false).build();
    }

    public static <T> AbstractConfigListEntry<?> objectList(String label,
                                                            List<T> values,
                                                            Consumer<List<T>> setter,
                                                            Supplier<T> factory,
                                                            Function<T, String> naming,
                                                            Function<T, List<AbstractConfigListEntry<?>>> editor) {
        return objectList(label, values, setter, factory, naming, editor, false);
    }

    public static <T> AbstractConfigListEntry<?> objectList(String label,
                                                            List<T> values,
                                                            Consumer<List<T>> setter,
                                                            Supplier<T> factory,
                                                            Function<T, String> naming,
                                                            Function<T, List<AbstractConfigListEntry<?>>> editor,
                                                            boolean ordered) {
        return objectList(label, values, setter, factory, naming, editor, ordered ? () -> {} : null);
    }

    public static <T> AbstractConfigListEntry<?> objectList(String label,
                                                            List<T> values,
                                                            Consumer<List<T>> setter,
                                                            Supplier<T> factory,
                                                            Function<T, String> naming,
                                                            Function<T, List<AbstractConfigListEntry<?>>> editor,
                                                            Runnable afterMove) {
        List<T> safe = values == null ? new ArrayList<>() : values;

        final NestedListListEntry<T, MultiElementListEntry<T>>[] self = new NestedListListEntry[1];

        BiFunction<T, NestedListListEntry<T, MultiElementListEntry<T>>, MultiElementListEntry<T>> cell =
                (value, parent) -> {
                    T element = value != null ? value : (factory != null ? factory.get() : null);
                    final MultiElementListEntry<T>[] holder = new MultiElementListEntry[1];

                    List<AbstractConfigListEntry<?>> children = element == null
                            ? new ArrayList<>()
                            : buildChildren(element, editor, self, afterMove, holder);

                    holder[0] = new MultiElementListEntry<>(
                            text(element == null ? "(new)" : naming.apply(element)),
                            element,
                            children,
                            false);
                    return holder[0];
                };

        self[0] = new NestedListListEntry<>(
                text(label),
                safe,
                false,
                Optional::empty,
                setter,
                factory != null ? () -> {
                    List<T> created = new ArrayList<>();
                    created.add(factory.get());
                    return created;
                } : ArrayList::new,
                text("Reset"),
                true,
                true,
                cell);
        return self[0];
    }

    private static <T> List<AbstractConfigListEntry<?>> buildChildren(
            T element,
            Function<T, List<AbstractConfigListEntry<?>>> editor,
            NestedListListEntry<T, MultiElementListEntry<T>>[] owner,
            Runnable afterMove,
            MultiElementListEntry<T>[] holder) {

        List<AbstractConfigListEntry<?>> children;
        Runnable rebuild = () -> CellRebuild.replace(holder[0],
                buildChildren(element, editor, owner, afterMove, holder));

        REBUILD.set(rebuild);
        try {
            children = editor.apply(element);
        } finally {
            REBUILD.remove();
        }

        if (afterMove != null) {
            children = withMoveControls(owner, element, children);
        }
        return children;
    }

    private static final ThreadLocal<Runnable> REBUILD = new ThreadLocal<>();

    public static Runnable rebuildHook() {
        return REBUILD.get();
    }

    private static <T> List<AbstractConfigListEntry<?>> withMoveControls(
            NestedListListEntry<T, MultiElementListEntry<T>>[] owner, T element, List<AbstractConfigListEntry<?>> children) {

        List<AbstractConfigListEntry<?>> result = new ArrayList<>();

        result.add(new ActionEntry(text("Move up"), text("▲ Up"),
                () -> {
                    if (owner[0] != null) {
                        ListReorder.moveBy(owner[0], element, -1);
                    }
                }));
        result.add(new ActionEntry(text("Move down"), text("▼ Down"),
                () -> {
                    if (owner[0] != null) {
                        ListReorder.moveBy(owner[0], element, 1);
                    }
                }));

        result.addAll(children);
        return result;
    }

    public static List<AbstractConfigListEntry<?>> of(AbstractConfigListEntry<?>... entries) {
        List<AbstractConfigListEntry<?>> list = new ArrayList<>();
        for (AbstractConfigListEntry<?> entry : entries) {
            if (entry != null) {
                list.add(entry);
            }
        }
        return list;
    }
}
