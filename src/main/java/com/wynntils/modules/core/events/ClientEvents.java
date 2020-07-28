/*
 *  * Copyright © Wynntils - 2018 - 2020.
 */

package com.wynntils.modules.core.events;

import com.wynntils.ModCore;
import com.wynntils.Reference;
import com.wynntils.core.events.custom.*;
import com.wynntils.core.framework.FrameworkManager;
import com.wynntils.core.framework.entities.EntityManager;
import com.wynntils.core.framework.enums.ClassType;
import com.wynntils.core.framework.enums.DamageType;
import com.wynntils.core.framework.enums.professions.GatheringMaterial;
import com.wynntils.core.framework.enums.professions.ProfessionType;
import com.wynntils.core.framework.instances.PlayerInfo;
import com.wynntils.core.framework.interfaces.Listener;
import com.wynntils.core.utils.ItemUtils;
import com.wynntils.core.utils.Utils;
import com.wynntils.core.utils.objects.Location;
import com.wynntils.core.utils.reflections.ReflectionFields;
import com.wynntils.modules.core.instances.GatheringBake;
import com.wynntils.modules.core.instances.MainMenuButtons;
import com.wynntils.modules.core.managers.*;
import com.wynntils.modules.core.managers.GuildAndFriendManager.As;
import com.wynntils.modules.core.overlays.inventories.ChestReplacer;
import com.wynntils.modules.core.overlays.inventories.HorseReplacer;
import com.wynntils.modules.core.overlays.inventories.IngameMenuReplacer;
import com.wynntils.modules.core.overlays.inventories.InventoryReplacer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.client.CPacketClientSettings;
import net.minecraft.network.play.server.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static com.wynntils.core.framework.instances.PlayerInfo.getPlayerInfo;

public class ClientEvents implements Listener {
    TotemTracker totemTracker = new TotemTracker();

    /**
     * This replace these GUIS into a "provided" format to make it more modular
     *
     * GuiInventory -> InventoryReplacer
     * GuiChest -> ChestReplacer
     * GuiScreenHorseInventory -> HorseReplacer
     * GuiIngameMenu -> IngameMenuReplacer
     *
     * Since forge doesn't provides any way to intercept these guis, like events, we need to replace them
     * this may cause conflicts with other mods that does the same thing
     *
     * @see InventoryReplacer
     * @see ChestReplacer
     * @see HorseReplacer
     * @see IngameMenuReplacer
     *
     * All of these "class replacers" emits a bunch of events that you can use to edit the selected GUI
     *
     * @param e GuiOpenEvent
     */
    @SubscribeEvent
    public void onGuiOpened(GuiOpenEvent e) {
        if (e.getGui() instanceof GuiInventory) {
            if (e.getGui() instanceof InventoryReplacer) return;

            e.setGui(new InventoryReplacer(ModCore.mc().player));
            return;
        }
        if (e.getGui() instanceof GuiChest) {
            if (e.getGui() instanceof ChestReplacer) return;

            e.setGui(new ChestReplacer(ModCore.mc().player.inventory, (IInventory) ReflectionFields.GuiChest_lowerChestInventory.getValue(e.getGui())));
            return;
        }
        if (e.getGui() instanceof GuiScreenHorseInventory) {
            if (e.getGui() instanceof HorseReplacer) return;

            e.setGui(new HorseReplacer(ModCore.mc().player.inventory, (IInventory) ReflectionFields.GuiScreenHorseInventory_horseInventory.getValue(e.getGui()), (AbstractHorse) ReflectionFields.GuiScreenHorseInventory_horseEntity.getValue(e.getGui())));
        }
        if (e.getGui() instanceof GuiIngameMenu) {
            if (e.getGui() instanceof IngameMenuReplacer) return;

            e.setGui(new IngameMenuReplacer());
        }
    }

    private static final Pattern GATHERING_STATUS = Pattern.compile("\\[\\+([0-9]*) [ⒸⒷⒿ] (.*?) XP\\] \\[([0-9]*)%\\]");
    private static final Pattern GATHERING_RESOURCE = Pattern.compile("\\[\\+([0-9]+) (.+)\\]");
    private static final Pattern MOB_DAMAGE = DamageType.compileDamagePattern();

    // bake status
    private GatheringBake bakeStatus = null;

    @SubscribeEvent
    public void gatherAndDamageDetection(PacketEvent<SPacketEntityMetadata> e) {
        // makes this method always be called in main thread
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            Minecraft.getMinecraft().addScheduledTask(() -> gatherAndDamageDetection(e));
            return;
        }

        if (e.getPacket().getDataManagerEntries() == null || e.getPacket().getDataManagerEntries().isEmpty()) return;
        Entity i = Minecraft.getMinecraft().world.getEntityByID(e.getPacket().getEntityId());
        if (!(i instanceof EntityArmorStand)) return;

        for (EntityDataManager.DataEntry<?> next : e.getPacket().getDataManagerEntries()) {
            if (!(next.getValue() instanceof String)) continue;

            String value = (String) next.getValue();
            if (value == null || value.isEmpty() || value.contains("Combat") || value.contains("Guild")) continue;
            value = TextFormatting.getTextWithoutFormattingCodes(value);

            Matcher m = GATHERING_STATUS.matcher(value);
            if (m.matches()) { // first, gathering status
                if (bakeStatus == null || bakeStatus.isInvalid()) bakeStatus = new GatheringBake();

                bakeStatus.setXpAmount(Double.parseDouble(m.group(1)));
                bakeStatus.setType(ProfessionType.valueOf(m.group(2).toUpperCase()));
                bakeStatus.setXpPercentage(Double.parseDouble(m.group(3)));
            } else if ((m = GATHERING_RESOURCE.matcher(value)).matches()) { // second, gathering resource
                if (bakeStatus == null || bakeStatus.isInvalid()) bakeStatus = new GatheringBake();

                String resourceType = m.group(2).contains(" ") ? m.group(2).split(" ")[0] : m.group(2);

                bakeStatus.setMaterialAmount(Integer.parseInt(m.group(1)));
                bakeStatus.setMaterial(GatheringMaterial.valueOf(resourceType.toUpperCase()));
            } else { // third, damage detection
                HashMap<DamageType, Integer> damageList = new HashMap<>();

                m = MOB_DAMAGE.matcher(value);
                while (m.find()) {
                    damageList.put(DamageType.fromSymbol(m.group(2)), Integer.valueOf(m.group(1)));
                }

                if (damageList.isEmpty()) return;

                FrameworkManager.getEventBus().post(new GameEvent.DamageEntity(damageList, i));
                return;
            }

            if (bakeStatus == null || !bakeStatus.isReady()) return;

            Location loc = new Location(i);

            // this tries to find a valid barrier that is the center of the three
            // below the center block there's a barrier, which is what we are looking for
            // it ALWAYS have 4 blocks at it sides that we use to detect it
            if (bakeStatus.getType() == ProfessionType.WOODCUTTING) {
               Iterable<BlockPos> positions = BlockPos.getAllInBox(
                        i.getPosition().subtract(new Vec3i(-5, -3, -5)),
                        i.getPosition().subtract(new Vec3i(+5, +3, +5)));

               for (BlockPos position : positions) {
                   if (i.world.isAirBlock(position)) continue;

                   IBlockState b = i.world.getBlockState(position);
                   if (b.getMaterial() == Material.AIR || b.getMaterial() != Material.BARRIER) continue;

                   // checks if the barrier have blocks around itself
                   BlockPos north = position.north();
                   if (i.world.isAirBlock(position.north())
                           || i.world.isAirBlock(position.south())
                           || i.world.isAirBlock(position.east())
                           || i.world.isAirBlock(position.west())) continue;

                   loc = new Location(position);
                   break;
               }
            }

            FrameworkManager.getEventBus().post(
                    new GameEvent.ResourceGather(bakeStatus.getType(), bakeStatus.getMaterial(),
                            bakeStatus.getMaterialAmount(), bakeStatus.getXpAmount(), bakeStatus.getXpPercentage(), loc)
            );

            bakeStatus = null;
            break;
        }
    }

    long lastPosRequest;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void updateActionBar(PacketEvent<SPacketChat> e) {
        if (!Reference.onServer || e.getPacket().getType() != ChatType.GAME_INFO) return;

        PlayerInfo.getPlayerInfo().updateActionBar(e.getPacket().getChatComponent().getUnformattedText());
        e.setCanceled(true);
    }

    @SubscribeEvent
    public void updateChatVisibility(PacketEvent<CPacketClientSettings> e) {
        if (e.getPacket().getChatVisibility() != EntityPlayer.EnumChatVisibility.HIDDEN) return;

        ReflectionFields.CPacketClientSettings_chatVisibility.setValue(e.getPacket(), EntityPlayer.EnumChatVisibility.FULL);
    }

    /**
     * Process the packet queue if the queue is not empty
     */
    @SubscribeEvent
    public void processPacketQueue(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !PacketQueue.hasQueuedPacket()) return;

        PingManager.calculatePing();
        PacketQueue.proccessQueue();
    }

    /**
     * Renders and process fake entities
     */
    @SubscribeEvent
    public void processFakeEntities(RenderWorldLastEvent e) {
        EntityManager.tickEntities(e.getPartialTicks(), e.getContext());
    }

    GuiScreen lastScreen = null;

    /**
     *  Register the new Main Menu buttons
     */
    @SubscribeEvent
    public void addMainMenuButtons(GuiScreenEvent.InitGuiEvent.Post e) {
        GuiScreen gui = e.getGui();

        if (gui instanceof GuiMainMenu) {
            boolean resize = lastScreen != null && lastScreen instanceof GuiMainMenu;
            MainMenuButtons.addButtons((GuiMainMenu) gui, e.getButtonList(), resize);
        }

        lastScreen = gui;
    }

    /**
     *  Handles the main menu new buttons actions
     */
    @SubscribeEvent
    public void mainMenuActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post e) {
        GuiScreen gui = e.getGui();
        if (gui != gui.mc.currentScreen || !(gui instanceof GuiMainMenu)) return;

        MainMenuButtons.actionPerformed((GuiMainMenu) gui, e.getButton(), e.getButtonList());
    }

    @SubscribeEvent
    public void joinGuild(WynnSocialEvent.Guild.Join e) {
        GuildAndFriendManager.changePlayer(e.getMember(), true, As.GUILD, true);
    }

    @SubscribeEvent
    public void leaveGuild(WynnSocialEvent.Guild.Leave e) {
        GuildAndFriendManager.changePlayer(e.getMember(), false, As.GUILD, true);
    }

    /**
     * Detects the user class based on the class selection GUI
     * This detection happens when the user click on an item that contains the class name pattern, inside the class selection GUI
     *
     * @param e Represents the click event
     */
    @SubscribeEvent
    public void changeClass(GuiOverlapEvent.ChestOverlap.HandleMouseClick e) {
        if (!e.getGui().getLowerInv().getName().contains("Select a Class")) return;

        if (e.getMouseButton() != 0
            || e.getSlotIn() == null
            || !e.getSlotIn().getHasStack()
            || !e.getSlotIn().getStack().hasDisplayName()
            || !e.getSlotIn().getStack().getDisplayName().contains("[>] Select")) return;


        getPlayerInfo().setClassId(e.getSlotId());

        String classLore = ItemUtils.getLore(e.getSlotIn().getStack()).get(1);
        String classS = classLore.substring(classLore.indexOf(TextFormatting.WHITE.toString()) + 2);

        ClassType selectedClass = ClassType.NONE;

        try {
            selectedClass = ClassType.valueOf(classS.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            switch (classS) {
                case "Hunter":
                    selectedClass = ClassType.ARCHER;
                    break;
                case "Knight":
                    selectedClass = ClassType.WARRIOR;
                    break;
                case "Dark Wizard":
                    selectedClass = ClassType.MAGE;
                    break;
                case "Ninja":
                    selectedClass = ClassType.ASSASSIN;
                    break;
                case "Skyseer":
                    selectedClass = ClassType.SHAMAN;
                    break;
            }
        }

        getPlayerInfo().updatePlayerClass(selectedClass);
    }

    @SubscribeEvent
    public void addFriend(WynnSocialEvent.FriendList.Add e) {
        Collection<String> newFriends = e.getMembers();
        if (e.isSingular) {
            // Single friend added
            for (String name : newFriends) {
                GuildAndFriendManager.changePlayer(name, true, As.FRIEND, true);
            }
            return;
        }

        // Friends list updated
        for (String name : newFriends) {
            GuildAndFriendManager.changePlayer(name, true, As.FRIEND, false);
        }

        GuildAndFriendManager.tryResolveNames();
    }

    @SubscribeEvent
    public void removeFriend(WynnSocialEvent.FriendList.Remove e) {
        Collection<String> removedFriends = e.getMembers();
        if (e.isSingular) {
            // Single friend removed
            for (String name : removedFriends) {
                GuildAndFriendManager.changePlayer(name, false, As.FRIEND, true);
            }
            return;
        }

        // Friends list updated; Socket managed in addFriend
        for (String name : removedFriends) {
            GuildAndFriendManager.changePlayer(name, false, As.FRIEND, false);
        }

        GuildAndFriendManager.tryResolveNames();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        PlayerEntityManager.onWorldLoad(e.getWorld());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        PlayerEntityManager.onWorldUnload();
    }

    @SubscribeEvent
    public void entityJoin(EntityJoinWorldEvent e) {
        if (!(e.getEntity() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) e.getEntity();
        if (player.getGameProfile() == null) return;

        String name = player.getGameProfile().getName();
        if (name.contains("\u0001") || name.contains("§")) return; // avoid player npcs

        UserManager.loadUser(e.getEntity().getUniqueID());
    }

    @SubscribeEvent
    public void onTotemSpawn(PacketEvent<SPacketSpawnObject> e) {
        totemTracker.onTotemSpawn(e);
    }

    @SubscribeEvent
    public void onTotemSpellCast(SpellEvent.Cast e) {
        totemTracker.onTotemSpellCast(e);
    }

    @SubscribeEvent
    public void onTotemTeleport(PacketEvent<SPacketEntityTeleport> e) {
        totemTracker.onTotemTeleport(e);
    }

    @SubscribeEvent
    public void onTotemRename(PacketEvent<SPacketEntityMetadata> e) {
        totemTracker.onTotemRename(e);
    }

    @SubscribeEvent
    public void onTotemDestroy(PacketEvent<SPacketDestroyEntities> e) {
        totemTracker.onTotemDestroy(e);
    }

    @SubscribeEvent
    public void onTotemClassChange(WynnClassChangeEvent e) {
        totemTracker.onTotemClassChange(e);
    }

    private static class TotemTracker {
        public enum TotemState { NONE, SUMMONED, LANDING, PREPARING, ACTIVATING, ACTIVE}
        private TotemState totemState = TotemState.NONE;
        private int trackedTotemId = -1;
        private double trackedX, trackedY, trackedZ;
        private double potentialX, potentialY, potentialZ;
        private int potentialTrackedId = -1;
        private int trackedTime;
        private long spellCastTimestamp = 0;
        private long totemCreatedTimestamp = Long.MAX_VALUE;

        private int bufferedId = -1;
        private double bufferedX = -1;
        private double bufferedY = -1;
        private double bufferedZ = -1;

        private static boolean isClose(double a, double b)
        {
            double diff = Math.abs(a - b);
            return  (diff < 3);
        }

        private void postEvent(Event event) {
            ModCore.mc().addScheduledTask(() -> FrameworkManager.getEventBus().post(event));
        }

        private Entity getBufferedEntity(int entityId) {
            Entity entity = ModCore.mc().world.getEntityByID(entityId);
            if (entity != null) return entity;

            if (entityId == bufferedId) {
                return new EntityArmorStand(ModCore.mc().world, bufferedX, bufferedY, bufferedZ);
            }

            return null;
        }

        private void updateTotemPosition(double x, double y, double z) {
            trackedX = x;
            trackedY = y;
            trackedZ = z;
        }

        private void checkTotemSummoned() {
            // Check if we have both creation and spell cast at roughly the same time
            if (Math.abs(totemCreatedTimestamp - spellCastTimestamp) < 500) {
                // If we have an active totem already, first remove that one
                removeTotem(true);
                trackedTotemId = potentialTrackedId;
                trackedTime  = -1;

                updateTotemPosition(potentialX, potentialY, potentialZ);
                totemState = TotemState.SUMMONED;
                postEvent(new SpellEvent.TotemSummoned());
            }
        }

        private void removeTotem(boolean forcefullyRemoved) {
            if (totemState != TotemState.NONE) {
                totemState = TotemState.NONE;
                trackedTotemId = -1;
                trackedTime = -1;
                trackedX = 0;
                trackedY = 0;
                trackedZ = 0;
                postEvent(new SpellEvent.TotemRemoved(forcefullyRemoved));
            }
        }

        public void onTotemSpawn(PacketEvent<SPacketSpawnObject> e) {
            if (e.getPacket().getType() == 78) {
                bufferedId = e.getPacket().getEntityID();
                bufferedX = e.getPacket().getX();
                bufferedY = e.getPacket().getY();
                bufferedZ = e.getPacket().getZ();

                if (e.getPacket().getEntityID() == trackedTotemId) {
                    // Totems respawn with the same entityID when landing.
                    // Update with more precise coordinates
                    updateTotemPosition(e.getPacket().getX(), e.getPacket().getY(), e.getPacket().getZ());
                    totemState = TotemState.LANDING;
                    return;
                }

                // Is it created close to us? Then it's a potential new totem
                if (isClose(e.getPacket().getX(), Minecraft.getMinecraft().player.posX) &&
                        isClose(e.getPacket().getY(), Minecraft.getMinecraft().player.posY + 1.0) &&
                        isClose(e.getPacket().getZ(), Minecraft.getMinecraft().player.posZ)) {
                    potentialTrackedId = e.getPacket().getEntityID();
                    potentialX = e.getPacket().getX();
                    potentialY = e.getPacket().getY();
                    potentialZ = e.getPacket().getZ();
                    totemCreatedTimestamp = System.currentTimeMillis();
                    checkTotemSummoned();
                }
            }
        }

        public void onTotemSpellCast(SpellEvent.Cast e) {
            if (e.getSpell().equals("Totem")) {
                spellCastTimestamp = System.currentTimeMillis();
                checkTotemSummoned();
            }
        }

        public void onTotemTeleport(PacketEvent<SPacketEntityTeleport> e) {
            int thisId = e.getPacket().getEntityId();

            if (thisId == trackedTotemId && (totemState == TotemState.SUMMONED || totemState == TotemState.LANDING)) {
                // Now the totem has gotten it's final coordinates
                updateTotemPosition(e.getPacket().getX(), e.getPacket().getY(), e.getPacket().getZ());
                totemState = TotemState.PREPARING;
            }
        }

        public void onTotemRename(PacketEvent<SPacketEntityMetadata> e) {
            if (!Reference.onServer || !Reference.onWorld) return;

            String name = Utils.getNameFromMetadata(e.getPacket().getDataManagerEntries());
            if (name == null || name.isEmpty()) return;

            Entity entity = getBufferedEntity(e.getPacket().getEntityId());
            if (!(entity instanceof EntityArmorStand)) return;

            Pattern shamanTotemTimer = Pattern.compile("^§c([0-9][0-9]?)s$");
            Matcher m = shamanTotemTimer.matcher(name);
            if (m.find()) {
                // We got a armor stand with a timer nametag
                double distanceXZ = Math.abs(entity.posX  - trackedX) +  Math.abs(entity.posZ  - trackedZ);
                if (distanceXZ < 3.0 && entity.posY <= (trackedY + 3.0) && entity.posY >= ((trackedY + 2.0))) {
                    // ... and it's close to our totem; regard this as our timer
                    int time = Integer.parseInt(m.group(1));

                    if (trackedTime == -1) {
                        trackedTime = time;
                        totemState = TotemState.ACTIVE;
                        postEvent(new SpellEvent.TotemActivated(trackedTime, new Location(trackedX, trackedY, trackedZ)));
                    } else if (time != trackedTime) {
                        trackedTime = time;
                    }
                }
            }
        }

        public void onTotemDestroy(PacketEvent<SPacketDestroyEntities> e) {
            IntStream entityIDs = Arrays.stream(e.getPacket().getEntityIDs());
            if (entityIDs.filter(id -> id == trackedTotemId).findFirst().isPresent()) {
                if (totemState == TotemState.ACTIVE && trackedTime == 0) {
                    removeTotem(false);
                }
            }
        }

        public void onTotemClassChange(WynnClassChangeEvent e) {
            removeTotem(true);
        }
    }

}
