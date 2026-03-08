package com.cybrisoft.redstoneeda.client.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SchemParser {
    public static class SchematicFormat {
        int width;
        int height;
        int length;
        byte[] blockData;
        int[] decodedIds;
        Map<Integer, String> palette;

        public SchematicFormat(int width, int height, int length, byte[] blockData, int[] decodedIds, Map<Integer, String> palette) {
            this.width = width;
            this.height = height;
            this.length = length;
            this.blockData = blockData;
            this.decodedIds = decodedIds;
            this.palette = palette;
        }
    }

    public static SchematicFormat parse(File file) throws Exception {
        NbtCompound root;
        try (FileInputStream fis = new FileInputStream(file)) {
            root = NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes());
        }

        NbtCompound blockContainer = root.getCompound("Schematic")
                .flatMap(schem -> schem.getCompound("Blocks"))
                .orElseThrow(() -> new RuntimeException("No blocks container found in schematic"));

        NbtCompound schematic = root.getCompound("Schematic")
                .orElseThrow(() -> new RuntimeException("No schematic tag found"));

        int width = schematic.getShort("Width").orElse((short) 0) & 0xFFFF;
        int height = schematic.getShort("Height").orElse((short) 0) & 0xFFFF;
        int length = schematic.getShort("Length").orElse((short) 0) & 0xFFFF;

        NbtCompound paletteNbt = blockContainer.getCompound("Palette")
                .orElseThrow(() -> new RuntimeException("No palette found"));

        byte[] blockData = blockContainer.getByteArray("Data")
                .orElseThrow(() -> new RuntimeException("No blockdata found"));

        Map<Integer, String> palette = new HashMap<>();
        for (String key : paletteNbt.getKeys()) {
            palette.put(paletteNbt.getInt(key).orElse(0), key);
        }

        int[] decodedIds = new int[width * height * length];
        int index = 0;
        int i = 0;
        while (i < blockData.length && index < decodedIds.length) {
            int value = 0;
            int varintLength = 0;
            while (true) {
                byte b = blockData[i++];
                value |= (b & 0x7F) << (varintLength++ * 7);
                if ((b & 0x80) == 0) break;
            }
            decodedIds[index++] = value;
        }

        return new SchematicFormat(width, height, length, blockData, decodedIds, palette);
    }
}