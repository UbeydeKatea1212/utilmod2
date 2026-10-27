package com.example.utilmod;

import org.lwjgl.glfw.GLFW;

import com.example.utilmod.config.UtilModConfig;
import com.example.utilmod.freecam.FreecamController;
import com.example.utilmod.render.EspRenderer;
import com.example.utilmod.render.HudOverlay;
import com.example.utilmod.scan.BlockScanner;
import com.example.utilmod.scan.ScanTargets;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec3d;

/** Wires the three features together. Keep this class thin: it only registers things. */
public final class UtilModClient implements ClientModInitializer {
    public static final String MOD_ID = "utilmod";

    @Override
    public void onInitializeClient() {
        BlockScanner scanner = new BlockScanner(ScanTargets.DEFAULTS);

        KeyBinding freecamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.utilmod.freecam", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, "key.categories.utilmod"));
        KeyBinding espKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.utilmod.esp", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, "key.categories.utilmod"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (freecamKey.wasPressed()) {
                FreecamController.toggle(client);
            }
            while (espKey.wasPressed()) {
                UtilModConfig.espEnabled = !UtilModConfig.espEnabled;
            }

            FreecamController.tick(client);

            if (UtilModConfig.espEnabled) {
                scanner.tick(client, scanCenter(client));
            }
        });

        // Don't leave a stale camera / results around when changing worlds.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FreecamController.disable();
            scanner.reset();
        });

        EspRenderer.register(scanner);
        HudOverlay.register(scanner);
    }

    /** Scan around the camera while in freecam (to preview an area), otherwise around the player. */
    private static Vec3d scanCenter(MinecraftClient client) {
        if (FreecamController.isActive()) {
            return FreecamController.getPos();
        }
        return client.player != null ? client.player.getPos() : null;
    }
}
