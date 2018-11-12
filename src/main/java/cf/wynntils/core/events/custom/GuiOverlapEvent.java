/*
 *  * Copyright © Wynntils - 2018.
 */

package cf.wynntils.core.events.custom;

import cf.wynntils.modules.core.overlays.inventories.ChestReplacer;
import cf.wynntils.modules.core.overlays.inventories.HorseReplacer;
import cf.wynntils.modules.core.overlays.inventories.InventoryReplacer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.common.eventhandler.Event;

public class GuiOverlapEvent extends Event {

    public static class InventoryOverlap extends GuiOverlapEvent {

        InventoryReplacer guiInventory;

        public InventoryOverlap(InventoryReplacer guiInventory) {
            this.guiInventory = guiInventory;
        }

        public InventoryReplacer getGuiInventory() {
            return guiInventory;
        }

        public static class DrawScreen extends InventoryOverlap {

            int mouseX, mouseY; float partialTicks;

            public DrawScreen(InventoryReplacer guiInventory, int mouseX, int mouseY, float partialTicks) {
                super(guiInventory);

                this.mouseX = mouseX; this.mouseY = mouseY; this.partialTicks = partialTicks;
            }

            public float getPartialTicks() {
                return partialTicks;
            }

            public int getMouseX() {
                return mouseX;
            }

            public int getMouseY() {
                return mouseY;
            }

        }

        public static class HandleMouseClick extends InventoryOverlap {

            Slot slotIn; int slotId, mouseButton; ClickType type;

            public HandleMouseClick(InventoryReplacer guiInventory, Slot slotIn, int slotId, int mouseButton, ClickType type)  {
                super(guiInventory);

                this.slotId = slotId; this.slotIn = slotIn; this.slotId = slotId; this.mouseButton = mouseButton; this.type = type;
            }

            public boolean isCancelable()
            {
                return true;
            }

            public ClickType getType() {
                return type;
            }

            public int getMouseButton() {
                return mouseButton;
            }

            public int getSlotId() {
                return slotId;
            }

            public Slot getSlotIn() {
                return slotIn;
            }
        }

        public static class DrawGuiContainerForegroundLayer extends InventoryOverlap {

            int mouseX, mouseY;

            public DrawGuiContainerForegroundLayer(InventoryReplacer guiInventory, int mouseX, int mouseY) {
                super(guiInventory);

                this.mouseX = mouseX; this.mouseY = mouseY;
            }

            public int getMouseY() {
                return mouseY;
            }

            public int getMouseX() {
                return mouseX;
            }
        }

        public static class KeyTyped extends InventoryOverlap {

            char typedChar; int keyCode;

            public KeyTyped(InventoryReplacer guiInventory, char typedChar, int keyCode) {
                super(guiInventory);

                this.typedChar = typedChar;
                this.keyCode = keyCode;
            }

            public char getTypedChar() {
                return typedChar;
            }

            public int getKeyCode() {
                return keyCode;
            }

            public boolean isCancelable() {
                return true;
            }

        }

    }

    public static class ChestOverlap extends GuiOverlapEvent {

        ChestReplacer guiChest;

        public ChestOverlap(ChestReplacer guiInventory) {
            this.guiChest = guiInventory;
        }

        public ChestReplacer getGuiInventory() {
            return guiChest;
        }

        public static class DrawScreen extends ChestOverlap {

            int mouseX, mouseY; float partialTicks;

            public DrawScreen(ChestReplacer guiChest, int mouseX, int mouseY, float partialTicks) {
                super(guiChest);

                this.mouseX = mouseX; this.mouseY = mouseY; this.partialTicks = partialTicks;
            }

            public float getPartialTicks() {
                return partialTicks;
            }

            public int getMouseX() {
                return mouseX;
            }

            public int getMouseY() {
                return mouseY;
            }

        }

        public static class HandleMouseClick extends ChestOverlap {

            Slot slotIn; int slotId, mouseButton; ClickType type;

            public HandleMouseClick(ChestReplacer guiChest, Slot slotIn, int slotId, int mouseButton, ClickType type)  {
                super(guiChest);

                this.slotId = slotId; this.slotIn = slotIn; this.slotId = slotId; this.mouseButton = mouseButton; this.type = type;
            }

            public boolean isCancelable()
            {
                return true;
            }

            public ClickType getType() {
                return type;
            }

            public int getMouseButton() {
                return mouseButton;
            }

            public int getSlotId() {
                return slotId;
            }

            public Slot getSlotIn() {
                return slotIn;
            }
        }

        public static class DrawGuiContainerForegroundLayer extends ChestOverlap {

            int mouseX, mouseY;

            public DrawGuiContainerForegroundLayer(ChestReplacer guiChest, int mouseX, int mouseY) {
                super(guiChest);

                this.mouseX = mouseX; this.mouseY = mouseY;
            }

            public int getMouseY() {
                return mouseY;
            }

            public int getMouseX() {
                return mouseX;
            }
        }

        public static class KeyTyped extends ChestOverlap {

            char typedChar; int keyCode;

            public KeyTyped(ChestReplacer guiChest, char typedChar, int keyCode) {
                super(guiChest);

                this.typedChar = typedChar;
                this.keyCode = keyCode;
            }

            public char getTypedChar() {
                return typedChar;
            }

            public int getKeyCode() {
                return keyCode;
            }

            public boolean isCancelable() {
                return true;
            }

        }

    }

    public static class HorseOverlap extends GuiOverlapEvent {

        HorseReplacer guiHorse;

        public HorseOverlap(HorseReplacer guiHorse) {
            this.guiHorse = guiHorse;
        }

        public HorseReplacer getGuiInventory() {
            return guiHorse;
        }

        public static class DrawScreen extends HorseOverlap {

            int mouseX, mouseY; float partialTicks;

            public DrawScreen(HorseReplacer guiHorse, int mouseX, int mouseY, float partialTicks) {
                super(guiHorse);

                this.mouseX = mouseX; this.mouseY = mouseY; this.partialTicks = partialTicks;
            }

            public float getPartialTicks() {
                return partialTicks;
            }

            public int getMouseX() {
                return mouseX;
            }

            public int getMouseY() {
                return mouseY;
            }

        }

        public static class HandleMouseClick extends HorseOverlap {

            Slot slotIn; int slotId, mouseButton; ClickType type;

            public HandleMouseClick(HorseReplacer guiHorse, Slot slotIn, int slotId, int mouseButton, ClickType type)  {
                super(guiHorse);

                this.slotId = slotId; this.slotIn = slotIn; this.slotId = slotId; this.mouseButton = mouseButton; this.type = type;
            }

            public boolean isCancelable()
            {
                return true;
            }

            public ClickType getType() {
                return type;
            }

            public int getMouseButton() {
                return mouseButton;
            }

            public int getSlotId() {
                return slotId;
            }

            public Slot getSlotIn() {
                return slotIn;
            }
        }

        public static class DrawGuiContainerForegroundLayer extends HorseOverlap {

            int mouseX, mouseY;

            public DrawGuiContainerForegroundLayer(HorseReplacer guiHorse, int mouseX, int mouseY) {
                super(guiHorse);

                this.mouseX = mouseX; this.mouseY = mouseY;
            }

            public int getMouseY() {
                return mouseY;
            }

            public int getMouseX() {
                return mouseX;
            }
        }

        public static class KeyTyped extends HorseOverlap {

            char typedChar; int keyCode;

            public KeyTyped(HorseReplacer guiHorse, char typedChar, int keyCode) {
                super(guiHorse);

                this.typedChar = typedChar;
                this.keyCode = keyCode;
            }

            public char getTypedChar() {
                return typedChar;
            }

            public int getKeyCode() {
                return keyCode;
            }

            public boolean isCancelable() {
                return true;
            }
        }

    }

}
