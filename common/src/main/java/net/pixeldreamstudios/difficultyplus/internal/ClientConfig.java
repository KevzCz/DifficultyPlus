package net.pixeldreamstudios.difficultyplus.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.config.ClientData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientConfig {
    private ClientConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ClientData data;

    public static ClientData get() {
        if (data == null) {
            data = load();
        }
        return data;
    }

    public static void save() {
        Path file = file();
        if (file == null || data == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            DifficultyPlus.LOGGER.warn("Difficulty+ could not save client config", e);
        }
    }

    private static ClientData load() {
        Path file = file();
        if (file == null || !Files.isRegularFile(file)) {
            return new ClientData();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            ClientData loaded = GSON.fromJson(reader, ClientData.class);
            return loaded == null ? new ClientData() : loaded;
        } catch (Exception e) {
            DifficultyPlus.LOGGER.warn("Difficulty+ could not read client config", e);
            return new ClientData();
        }
    }

    private static Path file() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        return client.gameDirectory.toPath()
                .resolve("config")
                .resolve("difficultyplus-client.json");
    }
}
