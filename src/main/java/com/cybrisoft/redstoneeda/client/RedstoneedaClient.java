package com.cybrisoft.redstoneeda.client;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.Redstoneeda;
import com.cybrisoft.redstoneeda.breakpoints.BreakpointResult;
import com.cybrisoft.redstoneeda.client.helpers.Docks;
import com.cybrisoft.redstoneeda.client.imgui.ImGuiImplementation;
import com.cybrisoft.redstoneeda.client.rendering.*;
import com.cybrisoft.redstoneeda.client.uiElements.windows.BreakpointWindow;
import com.cybrisoft.redstoneeda.client.util.IniUtil;
import com.cybrisoft.redstoneeda.client.util.MathHelper;
import com.cybrisoft.redstoneeda.client.util.Selection;
import com.cybrisoft.redstoneeda.managers.ClientStackTraceHandler;
import com.cybrisoft.redstoneeda.managers.Trace;
import com.cybrisoft.redstoneeda.networking.C2S.C2SInfoPacket;
import com.cybrisoft.redstoneeda.networking.S2C.S2CQueryProjectsPacket;
import com.cybrisoft.redstoneeda.networking.S2C.S2CSyncProjectPacket;
import com.cybrisoft.redstoneeda.networking.S2C.S2CTriggeredBreakpointPacket;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import imgui.ImGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.network.packet.c2s.play.ChangeGameModeC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;

public class RedstoneedaClient implements ClientModInitializer {
    public static final String MOD_ID = Redstoneeda.MOD_ID;
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String version = Redstoneeda.version;

    public static List<Selection> activeSelections = new ArrayList<>();
    public static Map<UUID, String> availableProjects = new HashMap<>();

    public static boolean shouldRender = false;
    private static KeyBinding openEditorKeybind;
    private static KeyBinding openContextMenuKeybind;

    public static boolean isLeftClicking = false;
    public static boolean isRightClicking = false;

    private static Project clientProject = null;
    private static Project lastClientProject = null;

    public static final RenderPipeline VIEWPORT_RESIZE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder()
                    .withLocation(Identifier.of("redstoneeda", "pipeline/blit_screen"))
                    .withVertexShader(Identifier.of("redstoneeda", "core/blit_screen"))
                    .withFragmentShader(Identifier.of("redstoneeda", "core/blit_screen"))
                    .withSampler("InSampler")
                    .withDepthWrite(false)
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    private static final KeyBinding.Category REDSTONEEDA_EDITOR_CATEGORY = KeyBinding.Category.create(Redstoneeda.identifier("editor"));
    private static boolean drawProjectOutline = false;
    private boolean pressedLastFrame = false;

    @Override
    public void onInitializeClient() {
        openEditorKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.redstoneeda.editor", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_F8, // The keycode of the key
                REDSTONEEDA_EDITOR_CATEGORY // The translation key of the keybinding's category.
        ));
        openContextMenuKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.redstoneeda.context_menu", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_Z, // The keycode of the key
                REDSTONEEDA_EDITOR_CATEGORY // The translation key of the keybinding's category.
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openEditorKeybind.wasPressed()) {
                toggleVisibility();
            }

            ContextMenuRenderer.setDrawContextMenu(openContextMenuKeybind.isPressed());
            if (openContextMenuKeybind.isPressed()) {
                pressedLastFrame = true;
                GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                client.mouse.unlockCursor();
            } else if (pressedLastFrame) {
                // frame after
                pressedLastFrame = false;
                GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                client.mouse.lockCursor();

                if (clientProject != null) {
                    ClientPlayNetworking.send(new C2SInfoPacket(C2SInfoPacket.Ops.TOGGLE_FREEZE.id(), clientProject.getUuid().toString())); // todo temp
                }
            }

            if (drawProjectOutline) {
                if (getClientProject() != null && activeSelections.isEmpty()) {
                    lastClientProject = getClientProject();
                    OutlineRenderer.outlines.put(getClientProject().getMin(), getClientProject().getMax());
                }
            } else if (lastClientProject != null) {
                OutlineRenderer.outlines.remove(lastClientProject.getMin(), lastClientProject.getMax());
            }

            for (Selection selection : new ArrayList<>(activeSelections)) {
                if (selection.isComplete()) {
                    activeSelections.remove(selection);
                } else {
                    selection.tick();
                }
            }

            BreakpointWindow.tick();
        });

        // Prevent Minecraft from locking the cursor when clicking
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (shouldRender) {
                if (openEditorKeybind.wasPressed()) {
                    toggleVisibility();
                }
                if (!ImGuiImplementation.grabbed) {
                    KeyBinding.unpressAll();
                    unlockCursor();
                }

                handleKeypresses();
            }
        });

        WorldFeatureRenderer.init();
        OutlineRenderer.init();
        ContextMenuRenderer.init();
//        SchematicRenderer.init();
//        SplineRenderer.init();

//        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
//            try {
//                SchemParser.SchematicFormat format = SchemParser.parse(new File("C:\\Users\\jaspe\\Downloads\\16bit_BIN_to_BCD.schem"));
//                BlockPos origin = new BlockPos(0, 100, 0);
//                SchematicRenderer.Schematic schematic = SchematicLoader.load(format, origin);
//                SchematicRenderer.getInstance().setSchematic(schematic);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });

        ClientPlayNetworking.registerGlobalReceiver(S2CSyncProjectPacket.ID, ((payload, context) -> {
            context.client().execute(() -> {
                if (payload.project() == null) return;
                setClientProject(payload.project());
                BreakpointWindow.invalidateProject();
            });
        }));

        ClientPlayNetworking.registerGlobalReceiver(S2CQueryProjectsPacket.ID, ((payload, context) -> {
            context.client().execute(() -> {
                availableProjects = payload.projects();
            });
        }));

        ClientPlayNetworking.registerGlobalReceiver(S2CTriggeredBreakpointPacket.ID, ((payload, context) -> {
            context.client().execute(() -> {
                BreakpointResult result = payload.result();

                IniUtil.scheduleIniLoad(Docks.DEBUGGER.get());
                Docks.DEBUGGER.setup();

                ClientStackTraceHandler.push(new Trace(result));
            });
        }));
    }

    private boolean rPressed = false;
    private boolean iPressed = false;
    private boolean yPressed = false;

    private void handleKeypresses() {
        Window window = MinecraftClient.getInstance().getWindow();
        boolean ctrlPressed = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shiftPressed = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

        if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_R) && !rPressed) {
            rPressed = true;
            ImGuiImplementation.enterGameKeyToggled = !ImGuiImplementation.enterGameKeyToggled;
        } else rPressed = false;

        if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_I) && !iPressed) {
            iPressed = true;

            drawProjectOutline = !drawProjectOutline;
        } else iPressed = false;

        if (ImGuiImplementation.enterGameKeyToggled) {
            // PIE

            if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_Y) && !yPressed) {
                yPressed = true;
                BlockPos currentPos = MathHelper.performRaycast(MinecraftClient.getInstance(), 15, false);
                World world = MinecraftClient.getInstance().world;
                if (world != null && currentPos != null) {
                    visited.clear();
                    if (world.getBlockState(currentPos).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                        runAlgo(world, currentPos);
                    } else if (world.getBlockState(currentPos.up()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                        runAlgo(world, currentPos.up());
                    }
                }
            } else yPressed = false;
        }

//        if (EditorHelper.getCurrentEditorType() == EditorHelper.EditorType.SCHEMATIC) {
//            if (shiftPressed) {
//                if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_A)) {
//                    SchematicEditorWindow.openPopup();
//                }
//            }
//
//            if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_SPACE)) {
//                SchematicEditorWindow.openPopup();
//            }
//            if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_ESCAPE)) {
//                SchematicEditorWindow.addComponentPopupOpen = false;
//            }
//        }
//        if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_LEFT_ALT)) {
//            BlockPos pos = MathHelper.performRaycast(MinecraftClient.getInstance(), 50.0f);
//            if (pos != null) {
//                SplineRenderer.splines.get(0).addPoint(new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
//            }
//        }
    }

    private static boolean checkForIllegalStates = true;

    private final Set<BlockPos> visited = new HashSet<>();

    private void runAlgo(World world, BlockPos pos) {
        if (!visited.add(pos)) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        WireConnection northConn = state.get(Properties.NORTH_WIRE_CONNECTION);
        WireConnection eastConn = state.get(Properties.EAST_WIRE_CONNECTION);
        WireConnection southConn = state.get(Properties.SOUTH_WIRE_CONNECTION);
        WireConnection westConn = state.get(Properties.WEST_WIRE_CONNECTION);

        OutlineRenderer.outlines.put(pos, pos.down());

        switch (northConn) {
            case UP -> runAlgo(world, pos.north().up());
            case SIDE -> {
                if (world.getBlockState(pos.north()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                    runAlgo(world, pos.north());
                } else if (world.getBlockState(pos.north().down()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                    runAlgo(world, pos.north().down());
                } else {
                    if (checkForIllegalStates) {
                        System.out.println("Illegal wire state at: " + pos);
                        WorldFeatureRenderer.addWarning(pos);
                    }
                }
            }
            case NONE -> {
                if (checkForIllegalStates) {
                    if (world.getBlockState(pos.north()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                        System.out.println("Illegal wire state at: " + pos);
                    } else if (world.getBlockState(pos.north().up()).getBlock().equals(Blocks.REDSTONE_WIRE) &&
                            world.getBlockState(pos.up()).isSolidBlock(world, pos.up())) {
                        System.out.println("Illegal wire state at: " + pos);
                    }
                }
            }
        }
        switch (eastConn) {
            case UP -> runAlgo(world, pos.east().up());
            case SIDE -> {
                if (world.getBlockState(pos.east()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                    runAlgo(world, pos.east());
                } else if (world.getBlockState(pos.east().down()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                    runAlgo(world, pos.east().down());
                } else {
                    if (checkForIllegalStates) {
                        System.out.println("Illegal wire state at: " + pos);
                        WorldFeatureRenderer.addWarning(pos);
                    }
                }
            }
            case NONE -> {
                if (checkForIllegalStates) {
                    if (world.getBlockState(pos.east()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                        System.out.println("Illegal wire state at: " + pos);
                    } else if (world.getBlockState(pos.east().up()).getBlock().equals(Blocks.REDSTONE_WIRE) &&
                            world.getBlockState(pos.up()).isSolidBlock(world, pos.up())) {
                        System.out.println("Illegal wire state at: " + pos);
                    }
                }
            }
        }
        switch (southConn) {
            case UP -> runAlgo(world, pos.south().up());
            case SIDE -> {
                if (world.getBlockState(pos.south()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                    runAlgo(world, pos.south());
                } else if (world.getBlockState(pos.south().down()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                    runAlgo(world, pos.south().down());
                } else {
                    if (checkForIllegalStates) {
                        System.out.println("Illegal wire state at: " + pos);
                        WorldFeatureRenderer.addWarning(pos);
                    }
                }
            }
            case NONE -> {
                if (checkForIllegalStates) {
                    if (world.getBlockState(pos.south()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                        System.out.println("Illegal wire state at: " + pos);
                    } else if (world.getBlockState(pos.south().up()).getBlock().equals(Blocks.REDSTONE_WIRE) &&
                            world.getBlockState(pos.up()).isSolidBlock(world, pos.up())) {
                        System.out.println("Illegal wire state at: " + pos);
                    }
                }
            }
        }
        switch (westConn) {
            case UP -> runAlgo(world, pos.west().up());
            case SIDE -> {
                if (world.getBlockState(pos.west()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                    runAlgo(world, pos.west());
                } else if (world.getBlockState(pos.west().down()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                    runAlgo(world, pos.west().down());
                } else {
                    if (checkForIllegalStates) {
                        System.out.println("Illegal wire state at: " + pos);
                        WorldFeatureRenderer.addWarning(pos);
                    }
                }
            }
            case NONE -> {
                if (checkForIllegalStates) {
                    if (world.getBlockState(pos.west()).getBlock().equals(Blocks.REDSTONE_WIRE)) {
                        System.out.println("Illegal wire state at: " + pos);
                    } else if (world.getBlockState(pos.west().up()).getBlock().equals(Blocks.REDSTONE_WIRE) &&
                            world.getBlockState(pos.up()).isSolidBlock(world, pos.up())) {
                        System.out.println("Illegal wire state at: " + pos);
                    }
                }
            }
        }
    }

    private void toggleVisibility() {
        shouldRender = !shouldRender;

        if (shouldRender) {
            ImGuiImplementation.aspectRatio = (float) MinecraftClient.getInstance().getWindow().getWidth() / MinecraftClient.getInstance().getWindow().getHeight();
            ImGui.setWindowFocus("Main");
            unlockCursor();
        } else {
            lockCursor();
        }

        GameMode gameMode = shouldRender ? GameMode.SPECTATOR : getPreviousGameMode();
        changeGameMode(gameMode);

        ImGuiImplementation.shouldRender = shouldRender;
    }

    public static void changeGameMode(GameMode gameMode) {
//        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
//        if (interactionManager != null) {
//            interactionManager.setGameMode(gameMode);
//        }
        if (MinecraftClient.getInstance().player == null) return;

        MinecraftClient.getInstance().player.networkHandler.sendPacket(new ChangeGameModeC2SPacket(gameMode));
    }

    private static void unlockCursor() {
        MinecraftClient client = MinecraftClient.getInstance();
        GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        client.mouse.unlockCursor();
    }

    public static GameMode getPreviousGameMode() {
        ClientPlayerInteractionManager clientPlayerInteractionManager = MinecraftClient.getInstance().interactionManager;
        GameMode gameMode = clientPlayerInteractionManager.getPreviousGameMode();
        if (gameMode != null) {
            return gameMode;
        } else {
            return clientPlayerInteractionManager.getCurrentGameMode() == GameMode.CREATIVE ? GameMode.SURVIVAL : GameMode.CREATIVE;
        }
    }

    public static void lockCursor() {
        MinecraftClient client = MinecraftClient.getInstance();
        GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        client.mouse.lockCursor();
    }

    public static Project getClientProject() {
        return clientProject;
    }

    public static void setClientProject(Project clientProject) {
        RedstoneedaClient.clientProject = clientProject;
    }
}
