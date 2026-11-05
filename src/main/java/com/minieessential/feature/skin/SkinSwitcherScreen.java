package com.minieessential.feature.skin;

import com.minieessential.MiniEssential;
import com.minieessential.config.MiniEssentialConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Feature 3 - Offline Skin Switcher: in-game screen.
 *
 * Lets the user manage up to 5 local skin slots:
 *   - Scan the local skins folder for .png files
 *   - Assign a file to a slot, set name, toggle Wide/Slim arms
 *   - Activate one slot (instant client-side rendering via mixins)
 *   - Remove a slot
 *
 * Fully local, no network, no AWT.
 */
public class SkinSwitcherScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final SkinSwitcher switcher = new SkinSwitcher();

    private int selectedSlot = 0;          // slot being edited (0-4)
    private String newName = "";
    private TextFieldWidget nameField;

    public SkinSwitcherScreen() {
        super(Text.literal("Skin Switcher"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int top = 40;
        int gap = 24;

        // Slot buttons (0-4) on the left
        for (int i = 0; i < SkinSwitcher.MAX_SKINS; i++) {
            int slot = i;
            ButtonWidget b = ButtonWidget.builder(Text.literal("Slot " + (i + 1)), btn -> selectedSlot = slot)
                    .dimensions(cx - 160, top + i * gap, 120, 20)
                    .build();
            addDrawableChild(b);
        }

        // Right side: controls for selected slot
        int rightX = cx + 20;
        int rightY = top;

        nameField = new TextFieldWidget(mc.textRenderer, rightX, rightY, 180, 20,
                Text.literal("Skin name"));
        nameField.setText(MiniEssential.config.skins.get(selectedSlot).name);
        addDrawableChild(nameField);

        ButtonWidget slimBtn = ButtonWidget.builder(
                Text.literal(MiniEssential.config.skins.get(selectedSlot).slimArms ? "Slim Arms" : "Wide Arms"),
                btn -> toggleSlim())
                .dimensions(rightX, rightY + 28, 180, 20).build();
        addDrawableChild(slimBtn);

        ButtonWidget activateBtn = ButtonWidget.builder(Text.literal("Activate Skin"), btn -> activateSlot())
                .dimensions(rightX, rightY + 56, 180, 20).build();
        addDrawableChild(activateBtn);

        ButtonWidget removeBtn = ButtonWidget.builder(Text.literal("Remove Skin"), btn -> removeSlot())
                .dimensions(rightX, rightY + 84, 180, 20).build();
        addDrawableChild(removeBtn);

        ButtonWidget scanBtn = ButtonWidget.builder(Text.literal("Scan Skins Folder"), btn -> scanFolder())
                .dimensions(rightX, rightY + 112, 180, 20).build();
        addDrawableChild(scanBtn);

        ButtonWidget backBtn = ButtonWidget.builder(Text.literal("Back"), btn -> mc.setScreen(null))
                .dimensions(width / 2 - 100, height - 30, 200, 20).build();
        addDrawableChild(backBtn);
    }

    private void toggleSlim() {
        SkinSlot slot = MiniEssential.config.skins.get(selectedSlot);
        slot.slimArms = !slot.slimArms;
        MiniEssential.config.save();
        // rebuild screen to reflect toggle
        mc.setScreen(new SkinSwitcherScreen());
    }

    private void activateSlot() {
        switcher.setActive(selectedSlot);
        mc.setScreen(null);
    }

    private void removeSlot() {
        SkinSlot slot = MiniEssential.config.skins.get(selectedSlot);
        slot.name = "Unnamed";
        slot.fileName = "";
        slot.slimArms = false;
        slot.active = false;
        MiniEssential.config.save();
        mc.setScreen(new SkinSwitcherScreen());
    }

    private void scanFolder() {
        List<String> files = switcher.scanAvailableSkins();
        if (files.isEmpty()) {
            return;
        }
        // assign first available file to selected slot if empty
        SkinSlot slot = MiniEssential.config.skins.get(selectedSlot);
        if (slot.fileName.isEmpty() && !files.isEmpty()) {
            slot.fileName = files.get(0);
            slot.name = files.get(0).replace(".png", "");
            MiniEssential.config.save();
            switcher.reloadTextures();
            mc.setScreen(new SkinSwitcherScreen());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        renderBackground(graphics);
        graphics.drawCenteredText(mc.textRenderer, title, width / 2, 16, 0xFFFFFF);

        // Instructions
        graphics.drawCenteredText(
                mc.textRenderer,
                "Put skin PNGs in .minecraft/miniessential/skins/",
                width / 2, 30, 0x808080);

        // Highlight selected slot
        int cx = width / 2;
        int top = 40;
        int gap = 24;
        graphics.fill(cx - 162, top + selectedSlot * gap - 2, cx - 40, top + selectedSlot * gap + 18, 0x60FFFFFF);

        // List slot status on left
        for (int i = 0; i < SkinSwitcher.MAX_SKINS; i++) {
            SkinSlot s = MiniEssential.config.skins.get(i);
            String status = s.active ? " [ACTIVE]" : "";
            String file = s.fileName.isEmpty() ? "(empty)" : s.fileName;
            graphics.drawString(mc.textRenderer, "Slot " + (i + 1) + ": " + s.name + status + " [" + file + "]",
                    cx - 150, top + i * gap + 6, i == selectedSlot ? 0xFFFF00 : 0xFFFFFF);
        }

        // Selected slot details on right
        SkinSlot sel = MiniEssential.config.skins.get(selectedSlot);
        graphics.drawString(mc.textRenderer, "Editing Slot " + (selectedSlot + 1), cx + 20, 38, 0xFFFFFF);
        graphics.drawString(mc.textRenderer, "Slim arms: " + (sel.slimArms ? "Yes (Alex)" : "No (Steve)"),
                cx + 20, top + 114, 0xA0A0A0);

        if (nameField != null) {
            nameField.render(graphics, mouseX, mouseY, tickDelta);
        }

        super.render(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            mc.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (nameField != null) {
            nameField.tick();
            MiniEssential.config.skins.get(selectedSlot).name = nameField.getText();
        }
    }

    @Override
    public void close() {
        MiniEssential.config.save();
        super.close();
    }
}
