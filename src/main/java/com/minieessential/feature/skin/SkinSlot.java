package com.minieessential.feature.skin;

/**
 * A single stored skin slot. Up to 5 of these are kept in MiniEssentialConfig.
 * The skin image lives in .minecraft/miniessential/skins/<fileName>.
 */
public class SkinSlot {
    public String name = "Unnamed";
    public String fileName = "";        // e.g. "steve.png"
    public boolean slimArms = false;    // false = Wide (Steve), true = Slim (Alex)
    public boolean active = false;      // only one slot should be active at a time

    public SkinSlot() {}

    public SkinSlot(String name, String fileName, boolean slimArms, boolean active) {
        this.name = name;
        this.fileName = fileName;
        this.slimArms = slimArms;
        this.active = active;
    }
}
