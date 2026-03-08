package com.cybrisoft.redstoneeda.client;

import com.cybrisoft.redstoneeda.Redstoneeda;
import com.cybrisoft.redstoneeda.client.imgui.ImGuiImplementation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import imgui.ImGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedstoneedaClient implements ClientModInitializer {
    public static final String MOD_ID = Redstoneeda.MOD_ID;
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String version = Redstoneeda.version;

    public static boolean shouldRender = false;
    private static KeyBinding openMenuKeybind;

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
    }

    private void handleKeypresses() {
        Window window = MinecraftClient.getInstance().getWindow();
        boolean ctrlPressed = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shiftPressed = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
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
