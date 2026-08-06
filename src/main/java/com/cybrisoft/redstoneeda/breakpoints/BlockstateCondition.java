package com.cybrisoft.redstoneeda.breakpoints;

import com.mojang.datafixers.types.templates.Tag;
import com.mojang.serialization.DataResult;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlockstateCondition implements BreakpointCondition {
    public UUID uuid = null;
    public List<BlockState> expectedStates = new ArrayList<>();
    public List<int[]> expectedPositions = new ArrayList<>();

    public BlockstateCondition(UUID uuid) {
        this.uuid = uuid;
    }

    public BlockstateCondition(UUID uuid, List<BlockState> expectedStates, List<int[]> expectedPositions) {
        this.uuid = uuid;
        this.expectedStates = expectedStates;
        this.expectedPositions = expectedPositions;
    }

    // will get executed on server only, so assumes server exclusive var access
    @Override
    public BreakpointResult evaluate(World world) {
        if (expectedPositions.size() != expectedStates.size()) {
            throw new IllegalArgumentException("ClientBlockstateCondition: expected positions and expected state counts misaligned!");
        }
        for (int i=0; i<expectedPositions.size(); i++) {
            int[] coords = expectedPositions.get(i);
            if (!world.getBlockState(new BlockPos(coords[0], coords[1], coords[2])).equals(expectedStates.get(i))) {
                return null;
            }
        }
        return new BreakpointResult(ConditionTypes.STATE, uuid, null, expectedPositions.stream().map((coords) -> new BlockPos(coords[0], coords[1], coords[2])).toArray(BlockPos[]::new), 0);
    }

    @Override
    public String getName() {
        return "Blockstate condition";
    }

    public static final PacketCodec<PacketByteBuf, BlockstateCondition> PACKET_CODEC = new PacketCodec<PacketByteBuf, BlockstateCondition>() {
        @Override
        public void encode(PacketByteBuf buf, BlockstateCondition value) {
            buf.writeUuid(value.uuid);
            buf.writeInt(value.expectedStates.size());
            for (BlockState state : value.expectedStates) {
                DataResult<NbtElement> encoded = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state);
                buf.writeNbt(encoded.getOrThrow());
            }
            buf.writeInt(value.expectedPositions.size());
            for (int[] position : value.expectedPositions) {
                buf.writeInt(position[0]);
                buf.writeInt(position[1]);
                buf.writeInt(position[2]);
            }
        }

        @Override
        public BlockstateCondition decode(PacketByteBuf buf) {
            UUID uuid = buf.readUuid();
            int stateSize = buf.readInt();
            List<BlockState> states = new ArrayList<>();
            for (int i=0; i<stateSize; i++) {
                DataResult<BlockState> decoded = BlockState.CODEC.parse(NbtOps.INSTANCE, buf.readNbt());
                states.add(decoded.getOrThrow());
            }

            int posSize = buf.readInt();
            List<int[]> positions = new ArrayList<>();
            for (int i=0; i<posSize; i++) {
                positions.add(new int[]{buf.readInt(), buf.readInt(), buf.readInt()});
            }
            return new BlockstateCondition(uuid, states, positions);
        }
    };
}
