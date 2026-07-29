package com.cybrisoft.redstoneeda.client.util;

import com.cybrisoft.redstoneeda.client.rendering.SchematicRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class SchematicLoader {

    public static SchematicRenderer.Schematic load(SchemParser.SchematicFormat format, BlockPos origin) {
        SchematicRenderer.Schematic schematic = new SchematicRenderer.Schematic();

        // Pre-resolve palette entries to BlockStates once, not per-block
        BlockState[] resolvedPalette = new BlockState[format.palette.size()];
        for (Map.Entry<Integer, String> entry : format.palette.entrySet()) {
            resolvedPalette[entry.getKey()] = parseBlockState(entry.getValue());
        }

        int width = format.width;
        int height = format.height;
        int length = format.length;

        // Schematics are stored Y→Z→X order
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;
                    int paletteId = format.decodedIds[index];

                    BlockState state = resolvedPalette[paletteId];
                    if (state == null || state.isAir()) continue;

                    BlockPos pos = origin.add(x, y, z);
                    schematic.setBlock(pos, state);
                }
            }
        }

        return schematic;
    }

    /**
     * Parses a palette string like "minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]"
     * into a BlockState.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState parseBlockState(String raw) {
        String blockId;
        String propsString = null;

        int bracketIndex = raw.indexOf('[');
        if (bracketIndex != -1) {
            blockId = raw.substring(0, bracketIndex);
            propsString = raw.substring(bracketIndex + 1, raw.length() - 1);
        } else {
            blockId = raw;
        }

        Block block = Registries.BLOCK.get(Identifier.of(blockId));
        BlockState state = block.getDefaultState();

        if (propsString != null && !propsString.isEmpty()) {
            for (String prop : propsString.split(",")) {
                String[] kv = prop.split("=", 2);
                if (kv.length != 2) continue;

                String propName = kv[0].trim();
                String propValue = kv[1].trim();

                Property property = state.getBlock().getStateManager().getProperty(propName);
                if (property != null) {
                    state = parseProperty(state, property, propValue);
                }
            }
        }

        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Comparable<T>> BlockState parseProperty(
            BlockState state, Property<T> property, String value
    ) {
        return property.parse(value)
                .map(v -> state.withIfExists(property, v))
                .orElse(state);
    }
}