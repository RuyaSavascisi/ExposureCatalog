package io.github.mortuusars.exposure_catalog.gui.screen;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.gui.screen.ZoomableScreen;
import io.github.mortuusars.exposure.gui.screen.element.Pager;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.render.PhotographRenderer;
import io.github.mortuusars.exposure.util.ItemAndStack;
import io.github.mortuusars.exposure.util.PagingDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CatalogPhotographScreen extends ZoomableScreen implements OverlayScreen {
    public static final ResourceLocation WIDGETS_TEXTURE = Exposure.resource("textures/gui/widgets.png");

    private final Screen parent;
    private final List<ItemAndStack<PhotographItem>> photographs;

    private final Pager pager = new Pager(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get());

    public CatalogPhotographScreen(Screen parentScreen, List<ItemAndStack<PhotographItem>> photographs) {
        super(Component.empty());
        this.parent = parentScreen;
        Preconditions.checkState(!photographs.isEmpty(), "No photographs to display.");
        this.photographs = photographs;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        zoomFactor = (float) height / ExposureClient.getExposureRenderer().getSize();

        ImageButton previousButton = new ImageButton(0, (int) (height / 2f - 16 / 2f), 16, 16,
                0, 0, 16, WIDGETS_TEXTURE, 256, 256,
                button -> pager.changePage(PagingDirection.PREVIOUS), Component.translatable("gui.exposure.previous_page"));
        addRenderableWidget(previousButton);

        ImageButton nextButton = new ImageButton(width - 16, (int) (height / 2f - 16 / 2f), 16, 16,
                16, 0, 16, WIDGETS_TEXTURE, 256, 256,
                button -> pager.changePage(PagingDirection.NEXT), Component.translatable("gui.exposure.next_page"));
        addRenderableWidget(nextButton);

        pager.init(photographs.size(), true, previousButton, nextButton);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (zoom.targetZoom == zoom.minZoom) {
            close();
            return;
        }

        pager.update();

        renderBackground(guiGraphics);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500); // Otherwise exposure will overlap buttons
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();

        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().translate(width / 2f, height / 2f, 0);
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.pose().translate(ExposureClient.getExposureRenderer().getSize() / -2f, ExposureClient.getExposureRenderer().getSize() / -2f, 0);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        ArrayList<ItemAndStack<PhotographItem>> photos = new ArrayList<>(photographs);
        Collections.rotate(photos, -pager.getCurrentPage());
        PhotographRenderer.renderStackedPhotographs(photos, guiGraphics.pose(), bufferSource, LightTexture.FULL_BRIGHT, 255, 255, 255, 255);

        bufferSource.endBatch();

        guiGraphics.pose().popPose();

        ItemAndStack<PhotographItem> photograph = photographs.get(pager.getCurrentPage());

        Either<String, ResourceLocation> idOrTexture = photograph.getItem().getIdOrTexture(photograph.getStack());
        if (minecraft.player != null && minecraft.player.isCreative() && idOrTexture != null) {
            guiGraphics.drawString(font, "?", width - font.width("?") - 10, 10, 0xFFFFFFFF);

            if (mouseX > width - 20 && mouseX < width && mouseY < 20) {
                List<Component> lines = new ArrayList<>();

                String exposureName = idOrTexture.map(id -> id, ResourceLocation::toString);
                lines.add(Component.literal(exposureName));

                lines.add(Component.translatable("gui.exposure.photograph_screen.drop_as_item_tooltip", Component.literal("CTRL + I")));

                lines.add(idOrTexture.map(
                        id -> Component.translatable("gui.exposure.photograph_screen.copy_id_tooltip", "CTRL + C"),
                        texture -> Component.translatable("gui.exposure.photograph_screen.copy_texture_path_tooltip", "CTRL + C")));

                guiGraphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY + 20);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE || minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            zoom.set(0f);
            return true;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (Screen.hasControlDown() && player != null && player.isCreative()) {
            ItemAndStack<PhotographItem> photograph = photographs.get(pager.getCurrentPage());

            if (keyCode == InputConstants.KEY_C) {
                @Nullable Either<String, ResourceLocation> idOrTexture = photograph.getItem().getIdOrTexture(photograph.getStack());
                if (idOrTexture != null) {
                    String text = idOrTexture.map(id -> id, ResourceLocation::toString);
                    Minecraft.getInstance().keyboardHandler.setClipboard(text);
                    player.displayClientMessage(Component.translatable("gui.exposure.photograph_screen.copied_message", text), false);
                }
                return true;
            }

            if (keyCode == InputConstants.KEY_I) {
                if (Minecraft.getInstance().gameMode != null) {
                    Minecraft.getInstance().gameMode.handleCreativeModeItemDrop(photograph.getStack().copy());
                    player.displayClientMessage(Component.translatable("gui.exposure.photograph_screen.item_dropped_message",
                            photograph.getStack().toString()), false);
                }
                return true;
            }
        }

        return pager.handleKeyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return pager.handleKeyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button))
            return true;

        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            zoom.set(0f);
            return true;
        }

        return false;
    }

    public void close() {
        Minecraft.getInstance().setScreen(getParent());
        if (minecraft.player != null)
            minecraft.player.playSound(Exposure.SoundEvents.PHOTOGRAPH_PLACE.get(), 0.7f, 1.1f);
    }

    @Override
    public Screen getParent() {
        return parent;
    }
}
