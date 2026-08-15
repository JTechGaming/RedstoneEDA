package com.cybrisoft.redstoneeda.io;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.breakpoints.BinaryCondition;
import com.cybrisoft.redstoneeda.breakpoints.BlockstateCondition;
import com.cybrisoft.redstoneeda.breakpoints.BreakpointCondition;
import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerProjectStorage {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockState.class, new TypeAdapter<BlockState>() {
                @Override
                public void write(JsonWriter out, BlockState state) throws IOException {
                    DataResult<JsonElement> encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state);
                    Streams.write(encoded.getOrThrow(), out);
                }

                @Override
                public BlockState read(JsonReader in) throws IOException {
                    JsonElement element = JsonParser.parseReader(in);
                    DataResult<BlockState> decoded = BlockState.CODEC.parse(JsonOps.INSTANCE, element);
                    return decoded.getOrThrow();
                }
            })
            .registerTypeAdapterFactory(new TypeAdapterFactory() {
                @Override
                public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                    if (!type.getRawType().equals(RegistryKey.class)) return null;

                    TypeAdapter<T> adapter = new TypeAdapter<T>() {
                        @Override
                        public void write(JsonWriter out, T key) throws IOException {
                            out.value(((RegistryKey<World>) key).getValue().toString());
                        }

                        @Override
                        public T read(JsonReader in) throws IOException {
                            return (T) RegistryKey.of(RegistryKeys.WORLD, Identifier.of(in.nextString()));
                        }
                    };

                    return adapter;
                }
            })
            .registerTypeAdapterFactory(new BreakpointConditionAdapterFactory())
            .create();

    public static void saveProject(Project project) {
        Path projectPath = FabricLoader.getInstance().getGameDir().resolve("redstoneeda/eda-projects/" + project.getUuid().toString() + ".reproj");

        if (!Files.exists(projectPath)) {
            try {
                projectPath.getParent().toFile().mkdirs();
                Files.createFile(projectPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create project file", e);
            }
        }

        try (Writer writer = Files.newBufferedWriter(projectPath)) {
            GSON.toJson(project, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write project file", e);
        }
    }

    public static Project readProject(UUID projectUUID) {
        Path projectPath = FabricLoader.getInstance().getGameDir().resolve("redstoneeda/eda-projects/" + projectUUID + ".reproj");

        if (!Files.exists(projectPath)) {
            try {
                projectPath.getParent().toFile().mkdirs();
                Files.createFile(projectPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create project file", e);
            }
        }

        try (Reader reader = Files.newBufferedReader(projectPath)) {
            return GSON.fromJson(reader, Project.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write project file", e);
        }
    }

    public static List<Project> queryProjects() {
        Path projectsFolder = FabricLoader.getInstance().getGameDir().resolve("redstoneeda/eda-projects/");

        projectsFolder.toFile().mkdirs();

        List<Project> projects = new ArrayList<>();

        try {
            Files.walk(projectsFolder).forEach((path) -> {
                if (path.toString().contains(".reproj")) {
                    String name = path.getFileName().toString().replace(".reproj", "");
                    try {
                        UUID uuid = UUID.fromString(name);
                        Project project = readProject(uuid);

                        projects.add(project);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Tried to parse a non-project file");
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return projects;
    }

    public static class BreakpointConditionAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!BreakpointCondition.class.isAssignableFrom(type.getRawType())) return null;

            TypeAdapter<T> adapter = new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T condition) throws IOException {
                    int type = condition instanceof BinaryCondition ? 0
                            : condition instanceof BlockstateCondition ? 1
                            : -1;

                    JsonObject wrapper = new JsonObject();
                    wrapper.addProperty("type", type);
                    wrapper.add("data", toJsonTreeUnchecked(gson, condition));

                    Streams.write(wrapper, out);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    JsonObject wrapper = JsonParser.parseReader(in).getAsJsonObject();
                    int type = wrapper.get("type").getAsInt();
                    JsonElement data = wrapper.get("data");

                    return (T) switch (type) {
                        case 0 -> gson.getDelegateAdapter(BreakpointConditionAdapterFactory.this, TypeToken.get(BinaryCondition.class)).fromJsonTree(data);
                        case 1 -> gson.getDelegateAdapter(BreakpointConditionAdapterFactory.this, TypeToken.get(BlockstateCondition.class)).fromJsonTree(data);
                        default -> null;
                    };
                }

                private <U> JsonElement toJsonTreeUnchecked(Gson gson, U value) {
                    TypeAdapter<U> adapter = (TypeAdapter<U>) gson.getDelegateAdapter(BreakpointConditionAdapterFactory.this, TypeToken.get(value.getClass()));
                    return adapter.toJsonTree(value);
                }
            };

            return adapter;
        }
    }
}
