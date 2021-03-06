/*
 * Decompiled with CFR 0.150.
 */
package clientname.gui.hud;

import clientname.Client;
import clientname.gui.hud.HUDManager;
import clientname.gui.hud.IRenderer;
import clientname.gui.hud.ScreenPosition;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class HUDConfigScreen
extends GuiScreen {
    private final HashMap<IRenderer, ScreenPosition> renderers = new HashMap();
    private Optional<IRenderer> selectedRenderer = Optional.empty();
    private int prevX;
    private int prevY;
    private boolean clear;

    public HUDConfigScreen(HUDManager api, boolean clear) {
        this.clear = clear;
        Collection<IRenderer> registeredRenderers = api.getRegisteredRenderers();
        for (IRenderer ren : registeredRenderers) {
            if (!ren.isEnabled()) continue;
            ScreenPosition pos = ren.load();
            if (pos == null) {
                pos = ScreenPosition.fromRelativePosition(0.5, 0.5);
            }
            this.adjustBounds(ren, pos);
            this.renderers.put(ren, pos);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawDefaultBackground();
        float zBackup = zLevel;
        zLevel = 200.0f;
        this.drawHollowRect(0, 0, this.width - 1, this.height - 1, Client.ConfigScreenColor);
        for (IRenderer renderer : this.renderers.keySet()) {
            ScreenPosition pos = this.renderers.get(renderer);
            renderer.renderDummy(pos);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        zLevel = zBackup;
    }

    public void drawHollowRect(int x, int y, int w, int h, int color) {
        this.drawHorizontalLine(x, x + w, y, color);
        this.drawHorizontalLine(x, x + w, y + h, color);
        this.drawVerticalLine(x, y + h, y, color);
        this.drawVerticalLine(x + w, y + h, y, color);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.renderers.entrySet().forEach(entry -> ((IRenderer)entry.getKey()).save((ScreenPosition)entry.getValue()));
            this.mc.displayGuiScreen(null);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long time) {
        if (this.selectedRenderer.isPresent()) {
            this.moveSelectedRenderBy(x - this.prevX, y - this.prevY);
        }
        this.prevX = x;
        this.prevY = y;
        super.mouseClickMove(x, y, button, time);
    }

    private void moveSelectedRenderBy(int offsetX, int offsetY) {
        IRenderer renderer = this.selectedRenderer.get();
        ScreenPosition pos = this.renderers.get(renderer);
        pos.setAbsolute(pos.getAbsoluteX() + offsetX, pos.getAbsoluteY() + offsetY);
        this.adjustBounds(renderer, pos);
    }

    @Override
    public void onGuiClosed() {
        for (IRenderer renderer : this.renderers.keySet()) {
            renderer.save(this.renderers.get(renderer));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private void adjustBounds(IRenderer renderer, ScreenPosition pos) {
        ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
        int screenWidth = res.getScaledWidth();
        int screenHeight = res.getScaledHeight();
        int absoluteX = Math.max(0, Math.min(pos.getAbsoluteX(), Math.max(screenWidth - renderer.getWidth(), 0)));
        int absoluteY = Math.max(0, Math.min(pos.getAbsoluteY(), Math.max(screenHeight - renderer.getHeight(), 0)));
        pos.setAbsolute(absoluteX, absoluteY);
    }

    @Override
    protected void mouseClicked(int x, int y, int mobuttonuseButton) throws IOException {
        this.prevX = x;
        this.prevY = y;
        this.loadMouseOver(x, y);
        super.mouseClicked(x, y, mobuttonuseButton);
    }

    private void loadMouseOver(int x, int y) {
        this.selectedRenderer = this.renderers.keySet().stream().filter(new MouseOverFinder(x, y)).findFirst();
    }

    private class MouseOverFinder
    implements Predicate<IRenderer> {
        private int mouseX;
        private int mouseY;

        public MouseOverFinder(int x, int y) {
            this.mouseX = x;
            this.mouseY = y;
        }

        @Override
        public boolean test(IRenderer renderer) {
            ScreenPosition pos = (ScreenPosition)HUDConfigScreen.this.renderers.get(renderer);
            int absoluteX = pos.getAbsoluteX();
            int absoluteY = pos.getAbsoluteY();
            return this.mouseX >= absoluteX && this.mouseX <= absoluteX + renderer.getWidth() && this.mouseY >= absoluteY && this.mouseY <= absoluteY + renderer.getHeight();
        }
    }
}

