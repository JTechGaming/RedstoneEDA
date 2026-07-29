package com.cybrisoft.redstoneeda.client;

import com.cybrisoft.redstoneeda.Redstoneeda;
import com.cybrisoft.redstoneeda.client.helpers.EditorHelper;
import com.cybrisoft.redstoneeda.client.imgui.ImGuiImplementation;
import com.cybrisoft.redstoneeda.client.rendering.OutlineRenderer;
import com.cybrisoft.redstoneeda.client.rendering.SchematicRenderer;
import com.cybrisoft.redstoneeda.client.rendering.SplineRenderer;
import com.cybrisoft.redstoneeda.client.uiElements.windows.SchematicEditorWindow;
import com.cybrisoft.redstoneeda.client.util.MathHelper;
import com.cybrisoft.redstoneeda.client.util.SchemParser;
import com.cybrisoft.redstoneeda.client.util.SchematicLoader;
import com.cybrisoft.redstoneeda.client.util.Selection;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import imgui.ImGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RedstoneedaClient implements ClientModInitializer {
    public static final String MOD_ID = Redstoneeda.MOD_ID;
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String version = Redstoneeda.version;

    List<Selection> activeSelections = new ArrayList<>();

    public static boolean shouldRender = false;
    private static KeyBinding openMenuKeybind;

    public static boolean isLeftClicking = false;
    public static boolean isRightClicking = false;

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

    @Override
    public void onInitializeClient() {
        openMenuKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.redstoneeda.menu", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_F8, // The keycode of the key
                REDSTONEEDA_EDITOR_CATEGORY // The translation key of the keybinding's category.
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openMenuKeybind.wasPressed()) {
                toggleVisibility();
            }

            for (Selection selection : new ArrayList<>(activeSelections)) {
                if (selection.isComplete()) {
                    activeSelections.remove(selection);
                } else {
                    selection.tick();
                }
            }
        });

        // Prevent Minecraft from locking the cursor when clicking
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (shouldRender) {
                if (openMenuKeybind.wasPressed()) {
                    toggleVisibility();
                }
                if (!ImGuiImplementation.grabbed) {
                    KeyBinding.unpressAll();
                    unlockCursor();
                }

                handleKeypresses();
            }
        });

        OutlineRenderer.init();
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
    }

    private void handleKeypresses() {
        Window window = MinecraftClient.getInstance().getWindow();
        boolean ctrlPressed = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shiftPressed = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

        if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_R)) {
            ImGuiImplementation.enterGameKeyToggled = !ImGuiImplementation.enterGameKeyToggled;
        }

        if (!ImGuiImplementation.enterGameKeyToggled) {
            if (InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_I)) {
                activeSelections.add(new Selection((pos1, pos2) -> {

                }));
            }
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
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (interactionManager != null) {
            interactionManager.setGameMode(gameMode);
        }
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
}
