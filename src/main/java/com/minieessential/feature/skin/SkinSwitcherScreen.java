package com.minieessential.feature.skin;

import com.minieessential.MiniEssential;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.List;

public class SkinSwitcherScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final SkinSwitcher switcher = MiniEssential.skinSwitcher;

    private int selectedSlot = 0;
    private TextFieldWidget nameField;

    public SkinSwitcherScreen() {
        super(Text.literal("Skin Switcher"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int top = 40;
        int gap = 24;

        for (int i = 0; i < SkinSwitcher.MAX_SKINS; i++) {
            int slot = i;
            ButtonWidget b = ButtonWidget.builder(Text.literal("Slot " + (i + 1)), btn -> {
                        selectedSlot = slot;
                        if (nameField != null) {
                            nameField.setText(MiniEssential.config.skins.get(selectedSlot).name);
                        }
                    })
                    .dimensions(cx - 160, top + i * gap, 120, 20)
                    .build();
            addDrawableChild(b);
        }

        int rightX = cx + 20;
        int rightY = top;

        nameField = new TextFieldWidget(mc.textRenderer, rightX, rightY, 180, 20,
                Text.literal("Skin name"));
        nameField.setText(MiniEssential.config.skins.get(selectedSlot).name);
        nameField.setChangedListener(text -> MiniEssential.config.skins.get(selectedSlot).name = text);
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
        switcher.reloadTextures();
        mc.setScreen(new SkinSwitcherScreen());
    }

    private void scanFolder() {
        List<String> files = switcher.scanAvailableSkins();
        if (files.isEmpty()) {
            return;
        }
        SkinSlot slot = MiniEssential.config.skins.get(selectedSlot);
        if (slot.fileName.isEmpty()) {
            slot.fileName = files.get(0);
            slot.name = files.get(0).replace(".png", "");
            MiniEssential.config.save();
            switcher.reloadTextures();
            mc.setScreen(new SkinSwitcherScreen());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        super.render(context, mouseX, mouseY, tickDelta);
        context.drawCenteredTextWithShadow(mc.textRenderer, title, width / 2, 16, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(
                mc.textRenderer,
                "Put skin PNGs in .minecraft/miniessential/skins/",
                width / 2, 30, 0xFF808080);

        int cx = width / 2;
        int top = 40;
        int gap = 24;
        context.fill(cx - 162, top + selectedSlot * gap - 2, cx - 40, top + selectedSlot * gap + 18, 0x60FFFFFF);

        for (int i = 0; i < SkinSwitcher.MAX_SKINS; i++) {
            SkinSlot s = MiniEssential.config.skins.get(i);
            String status = s.active ? " [ACTIVE]" : "";
            String file = s.fileName.isEmpty() ? "(empty)" : s.fileName;
            context.drawTextWithShadow(mc.textRenderer, "Slot " + (i + 1) + ": " + s.name + status + " [" + file + "]",
                    cx - 150, top + i * gap + 6, i == selectedSlot ? 0xFFFFFF00 : 0xFFFFFFFF);
        }

        SkinSlot sel = MiniEssential.config.skins.get(selectedSlot);
        context.drawTextWithShadow(mc.textRenderer, "Editing Slot " + (selectedSlot + 1), cx + 20, 28, 0xFFFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, "Slim arms: " + (sel.slimArms ? "Yes (Alex)" : "No (Steve)"),
                cx + 20, top + 140, 0xFFA0A0A0);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            mc.setScreen(null);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        MiniEssential.config.save();
        super.close();
    }
}
