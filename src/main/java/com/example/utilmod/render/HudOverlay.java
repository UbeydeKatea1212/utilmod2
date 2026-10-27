package com.example.utilmod.render;

import com.example.utilmod.config.UtilModConfig;
import com.example.utilmod.freecam.FreecamController;
import com.example.utilmod.scan.BlockScanner;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Small 2D status readout in the top-left corner. */
public final class HudOverlay {
    private HudOverlay() {}

    public static void register(BlockScanner scanner) {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context, scanner));
    }

    private static void render(DrawContext context, BlockScanner scanner) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null) {
            return;
        }

        int x = 4;
        int y = 4;
        int lineHeight = client.textRenderer.fontHeight + 2;

        if (FreecamController.isActive()) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Freecam"), x, y, 0x55FFFF);
            y += lineHeight;
        }

        if (UtilModConfig.espEnabled) {
            String status = "ESP: " + scanner.matchCount() + " found"
                    + (scanner.pendingChunks() > 0 ? " (scanning " + scanner.pendingChunks() + ")" : "");
            context.drawTextWithShadow(client.textRenderer, Text.literal(status), x, y, 0xFFFFFF);
        }
    }
}
