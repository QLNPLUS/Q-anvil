package com.qanvil.menu;

import com.qanvil.QAnvil;
import com.qanvil.QAnvilConfig;
import com.qanvil.currency.QAnvilCosts;
import com.qanvil.currency.QAnvilCurrencyBridge;
import com.qanvil.kubejs.QAnvilEventData;
import com.qanvil.kubejs.QAnvilKubeJSBridge;
import com.qanvil.kubejs.QAnvilTakeEventData;
import com.qanvil.network.QAnvilNetwork;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

public final class QAnvilMenu extends AnvilMenu {
    private static final int HEALTH_COST_SCALE = 1000;

    /**
     * Horizontal offset of every panel element inside the GUI texture. The base
     * texture stores the panel this many pixels to the right so that the armor
     * column fits on the left edge of the panel; the screen blits the art back
     * by the same amount.
     */
    public static final int GUI_ART_OFFSET_X = 32;

    private static final int PLAYER_INVENTORY_SLOT_START = RESULT_SLOT + 1;
    private static final int PLAYER_INVENTORY_SLOT_END = PLAYER_INVENTORY_SLOT_START + 36;
    private static final int ARMOR_SLOT_START = PLAYER_INVENTORY_SLOT_END;
    private static final int ARMOR_SLOT_COUNT = 4;
    private static final int ARMOR_SLOT_END = ARMOR_SLOT_START + ARMOR_SLOT_COUNT;

    // The armor column hangs off the left side of the panel. Its four slots
    // start at texture coordinates (18, 86) and are spaced like the player
    // inventory rows, so the GUI coordinate is 18 - GUI_ART_OFFSET_X.
    private static final int ARMOR_SLOT_X = 18 - GUI_ART_OFFSET_X;
    private static final int ARMOR_SLOT_Y = 86;
    private static final int ARMOR_SLOT_SPACING = 18;
    private static final EquipmentSlot[] ARMOR_EQUIPMENT_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET};
    private static final int[] ARMOR_INVENTORY_SLOTS = {39, 38, 37, 36};
    // Empty-slot icons, in the same head-to-feet order as the slot column.
    private static final ResourceLocation[] ARMOR_EMPTY_ICONS = {
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

    private final DataSlot currencyCost = DataSlot.standalone();
    private final DataSlot healthCost = DataSlot.standalone();
    private final DataSlot affordability = DataSlot.standalone();
    private static final Field INPUT_SLOTS_FIELD = findInputSlotsField();
    private ItemStack lastLargeLeft = ItemStack.EMPTY;
    private ItemStack lastLargeRight = ItemStack.EMPTY;
    private ItemStack lastLargeCarried = ItemStack.EMPTY;
    private String lastPromptText;
    private String lastSyncedCurrencyId;
    private String lastSyncedCurrencyDisplayName;
    private int promptResyncTicks;
    private int materialCost = -1;
    private String currencyId;
    private String currencyDisplayName;
    private String promptText = "";
    private boolean takeEventInProgress;
    private boolean takeEventCompleted;

    public QAnvilMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(containerId, inventory, access);
        this.currencyId = QAnvilCosts.resolveCurrencyId();
        this.currencyDisplayName = QAnvilCurrencyBridge.displayName(currencyId);
        addDataSlot(currencyCost);
        addDataSlot(healthCost);
        addDataSlot(affordability);
        replaceInputContainer();
        replaceInputSlot(INPUT_SLOT);
        replaceInputSlot(ADDITIONAL_SLOT);
        addArmorSlots(inventory);
    }

    private void addArmorSlots(Inventory inventory) {
        // These slots watch the real player inventory armor entries, so equipping
        // or unequipping in the Q Anvil changes what the player is wearing.
        for (int index = 0; index < ARMOR_SLOT_COUNT; index++) {
            addSlot(new QAnvilArmorSlot(
                    inventory,
                    ARMOR_INVENTORY_SLOTS[index],
                    ARMOR_SLOT_X,
                    ARMOR_SLOT_Y + index * ARMOR_SLOT_SPACING,
                    ARMOR_EQUIPMENT_SLOTS[index],
                    ARMOR_EMPTY_ICONS[index]));
        }
    }

    @Override
    public MenuType<?> getType() {
        return QAnvil.Q_ANVIL_MENU.get();
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(QAnvil.Q_ANVIL.get());
    }

    private void replaceInputSlot(int slotIndex) {
        Slot vanillaSlot = slots.get(slotIndex);
        QAnvilInputSlot replacement = new QAnvilInputSlot(inputSlots, slotIndex, vanillaSlot.x, vanillaSlot.y);
        replacement.index = vanillaSlot.index;
        slots.set(slotIndex, replacement);
    }

    private void replaceInputContainer() {
        try {
            INPUT_SLOTS_FIELD.set(this, new QAnvilInputContainer(this));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to replace Q Anvil input container", exception);
        }
    }

    private static Field findInputSlotsField() {
        // The field name differs between official, SRG, and obfuscated runtime jars.
        // Keep the known names here because string literals are not remapped.
        String[] candidates = {"inputSlots", "f_39769_", "f_39782_", "q"};
        for (String candidate : candidates) {
            try {
                Field field = net.minecraft.world.inventory.ItemCombinerMenu.class
                        .getDeclaredField(candidate);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Try the next mapping name.
            }
        }

        // The input container is the only field declared with the exact Container
        // type; this keeps the replacement working if a future mapping renames it.
        for (Field field : net.minecraft.world.inventory.ItemCombinerMenu.class.getDeclaredFields()) {
            if (field.getType() == net.minecraft.world.Container.class) {
                field.setAccessible(true);
                return field;
            }
        }

        throw new ExceptionInInitializerError(
                new NoSuchFieldException("Unable to find ItemCombinerMenu input container"));
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }

        syncPromptText(serverPlayer);
        syncCurrencyInfo(serverPlayer);
        syncLargeStack(serverPlayer, INPUT_SLOT, inputSlots.getItem(INPUT_SLOT), lastLargeLeft);
        lastLargeLeft = largeStackCopy(inputSlots.getItem(INPUT_SLOT));
        syncLargeStack(serverPlayer, ADDITIONAL_SLOT, inputSlots.getItem(ADDITIONAL_SLOT), lastLargeRight);
        lastLargeRight = largeStackCopy(inputSlots.getItem(ADDITIONAL_SLOT));
        syncLargeStack(serverPlayer, -1, getCarried(), lastLargeCarried);
        lastLargeCarried = largeStackCopy(getCarried());
    }

    private void syncPromptText(net.minecraft.server.level.ServerPlayer player) {
        boolean changed = !Objects.equals(promptText, lastPromptText);
        if (changed || (!promptText.isEmpty() && promptResyncTicks-- <= 0)) {
            QAnvilNetwork.sendPromptText(player, containerId, getStateId(), promptText);
            lastPromptText = promptText;
            promptResyncTicks = promptText.isEmpty() ? 0 : 20;
        }
    }

    private void syncCurrencyInfo(net.minecraft.server.level.ServerPlayer player) {
        String displayName = currencyDisplayName == null || currencyDisplayName.isEmpty()
                ? currencyId : currencyDisplayName;
        boolean changed = !Objects.equals(currencyId, lastSyncedCurrencyId)
                || !Objects.equals(displayName, lastSyncedCurrencyDisplayName);
        if (changed) {
            QAnvilNetwork.sendCurrencyInfo(player, containerId, getStateId(), currencyId, displayName);
            lastSyncedCurrencyId = currencyId;
            lastSyncedCurrencyDisplayName = displayName;
        }
    }

    private void syncLargeStack(net.minecraft.server.level.ServerPlayer player, int slot,
                                ItemStack current, ItemStack previous) {
        if (current.getCount() > Byte.MAX_VALUE && !ItemStack.matches(current, previous)) {
            QAnvilNetwork.sendLargeStack(player, containerId, getStateId(), slot, current);
        }
    }

    private static ItemStack largeStackCopy(ItemStack stack) {
        return stack.getCount() > Byte.MAX_VALUE ? stack.copy() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < ARMOR_SLOT_START || index >= ARMOR_SLOT_END) {
            return super.quickMoveStack(player, index);
        }

        // The armor column lives outside the slot ranges the vanilla anvil knows
        // about, so shift-clicking equipment moves its stack into the player
        // inventory instead of being ignored.
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (!moveItemStackTo(stack, PLAYER_INVENTORY_SLOT_START, PLAYER_INVENTORY_SLOT_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return moved;
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse) {
        if (startIndex >= getResultSlot() || endIndex <= 0) {
            return super.moveItemStackTo(stack, startIndex, endIndex, reverse);
        }

        boolean moved = false;
        int index = reverse ? endIndex - 1 : startIndex;
        while (!stack.isEmpty() && (reverse ? index >= startIndex : index < endIndex)) {
            Slot slot = slots.get(index);
            ItemStack existing = slot.getItem();
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(stack, existing)) {
                int maxSize = slot.getMaxStackSize(stack);
                int transferable = Math.min(stack.getCount(), maxSize - existing.getCount());
                if (transferable > 0) {
                    stack.shrink(transferable);
                    existing.grow(transferable);
                    slot.setChanged();
                    moved = true;
                }
            }
            index += reverse ? -1 : 1;
        }

        if (!stack.isEmpty()) {
            index = reverse ? endIndex - 1 : startIndex;
            while (reverse ? index >= startIndex : index < endIndex) {
                Slot slot = slots.get(index);
                if (slot.getItem().isEmpty() && slot.mayPlace(stack)) {
                    int transferable = Math.min(stack.getCount(), slot.getMaxStackSize(stack));
                    slot.setByPlayer(stack.split(transferable));
                    slot.setChanged();
                    moved = true;
                    break;
                }
                index += reverse ? -1 : 1;
            }
        }

        return moved;
    }

    @Override
    public void createResult() {
        ItemStack input = inputSlots.getItem(INPUT_SLOT).copy();
        ItemStack addition = inputSlots.getItem(ADDITIONAL_SLOT).copy();
        super.createResult();
        normalizeVanillaEnchantmentLevels(resultSlots.getItem(RESULT_SLOT), input, addition);

        ItemStack vanillaOutput = resultSlots.getItem(RESULT_SLOT).copy();
        int vanillaLevelCost = getCost();
        if (!player.level().isClientSide) {
            this.currencyId = QAnvilCosts.resolveCurrencyId();
            this.currencyDisplayName = QAnvilCurrencyBridge.displayName(currencyId);
        }

        if (player.level().isClientSide) {
            applyCosts(vanillaOutput.isEmpty() ? 0.0D : QAnvilCosts.defaultCurrencyCost(vanillaLevelCost),
                    vanillaOutput.isEmpty() ? 0.0D : QAnvilCosts.defaultHealthCost(vanillaLevelCost));
            setMaximumCost(0);
            return;
        }

        if (input.isEmpty() || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            promptText = "";
            clearCosts();
            setMaximumCost(0);
            return;
        }

        QAnvilEventData event = new QAnvilEventData(
                serverPlayer,
                input,
                inputSlots.getItem(ADDITIONAL_SLOT).copy(),
                vanillaOutput,
                vanillaLevelCost,
                currencyId,
                QAnvilCosts.defaultCurrencyCost(vanillaLevelCost),
                QAnvilCosts.defaultHealthCost(vanillaLevelCost));

        boolean accepted = QAnvilKubeJSBridge.post(event);
        promptText = event.promptText;
        if (!accepted) {
            resultSlots.setItem(RESULT_SLOT, ItemStack.EMPTY);
            clearCosts();
        } else {
            ItemStack output = event.output;
            if (output.isEmpty()) {
                resultSlots.setItem(RESULT_SLOT, ItemStack.EMPTY);
                clearCosts();
            } else {
                resultSlots.setItem(RESULT_SLOT, output);
                this.currencyId = event.currencyId == null || event.currencyId.isBlank()
                        ? QAnvilCosts.resolveCurrencyId() : event.currencyId;
                this.currencyDisplayName = QAnvilCurrencyBridge.displayName(currencyId);
                applyCosts(event.currencyCost, event.healthCost);
                materialCost = event.materialCostSet ? Math.max(0, event.materialCost) : -1;
                affordability.set(QAnvilCosts.canAfford(serverPlayer, getCurrencyId(),
                        getCurrencyCost(), getHealthCost()) ? 1 : 0);
            }
        }

        syncCurrencyInfo(serverPlayer);

        // Send the KubeJS prompt immediately. This keeps it visible even when
        // no vanilla data slot changes during this recalculation.
        syncPromptText(serverPlayer);

        // AnvilMenu's private XP DataSlot is deliberately kept at zero.
        setMaximumCost(0);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player.level().isClientSide || slotId != getResultSlot() || !isTakeClick(clickType)
                || resultSlots.getItem(RESULT_SLOT).isEmpty() || !mayPickup(player, true)) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        ItemStack previewOutput = resultSlots.getItem(RESULT_SLOT).copy();
        if (!prepareTakeOutput(player)) {
            return;
        }

        boolean previousTakeInProgress = takeEventInProgress;
        boolean previousTakeCompleted = takeEventCompleted;
        takeEventInProgress = true;
        takeEventCompleted = false;
        try {
            super.clicked(slotId, button, clickType, player);
        } finally {
            if (!takeEventCompleted) {
                // A click can be rejected after the event ran, for example when the
                // cursor cannot accept the result. Keep the preview in that case.
                resultSlots.setItem(RESULT_SLOT, previewOutput);
            }
            takeEventInProgress = previousTakeInProgress;
            takeEventCompleted = previousTakeCompleted;
        }
    }

    private static boolean isTakeClick(ClickType clickType) {
        return clickType == ClickType.PICKUP
                || clickType == ClickType.QUICK_MOVE
                || clickType == ClickType.SWAP
                || clickType == ClickType.THROW
                || clickType == ClickType.PICKUP_ALL;
    }

    private boolean prepareTakeOutput(Player player) {
        if (player.level().isClientSide) {
            return true;
        }
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return false;
        }

        ItemStack output = resultSlots.getItem(RESULT_SLOT);
        if (output.isEmpty()) {
            return false;
        }

        QAnvilTakeEventData event = new QAnvilTakeEventData(
                serverPlayer,
                inputSlots.getItem(INPUT_SLOT),
                inputSlots.getItem(ADDITIONAL_SLOT),
                output);
        if (!QAnvilKubeJSBridge.postTake(event) || event.output.isEmpty()
                || !QAnvilCosts.canAfford(player, getCurrencyId(), getCurrencyCost(), getHealthCost())) {
            return false;
        }

        resultSlots.setItem(RESULT_SLOT, event.output.copy());
        return true;
    }

    private static void normalizeVanillaEnchantmentLevels(ItemStack result, ItemStack input, ItemStack addition) {
        if (result.isEmpty()) {
            return;
        }

        Map<Enchantment, Integer> inputEnchantments = EnchantmentHelper.getEnchantments(input);
        Map<Enchantment, Integer> additionEnchantments = EnchantmentHelper.getEnchantments(addition);
        ListTag enchantments = result.is(Items.ENCHANTED_BOOK)
                ? EnchantedBookItem.getEnchantments(result)
                : result.getEnchantmentTags();

        for (int index = 0; index < enchantments.size(); index++) {
            CompoundTag enchantmentTag = enchantments.getCompound(index);
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT
                    .getOptional(EnchantmentHelper.getEnchantmentId(enchantmentTag))
                    .orElse(null);
            if (enchantment == null) {
                continue;
            }

            int inputLevel = Math.max(0, inputEnchantments.getOrDefault(enchantment, 0));
            int additionLevel = Math.max(0, additionEnchantments.getOrDefault(enchantment, 0));
            int maximumLevel = enchantment.getMaxLevel();
            int currentLevel = EnchantmentHelper.getEnchantmentLevel(enchantmentTag);
            int normalizedLevel;
            if (inputLevel > maximumLevel || additionLevel > maximumLevel) {
                // Preserve an explicitly over-level input, but never let a
                // previous result level become the next merge's source level.
                normalizedLevel = Math.max(inputLevel, additionLevel);
            } else {
                normalizedLevel = Math.min(currentLevel, maximumLevel);
            }

            if (normalizedLevel != currentLevel) {
                // Write directly to the stored-enchantment tag. EnchantedBookItem
                // intentionally keeps the higher existing level when using
                // EnchantmentHelper.setEnchantments, so that API cannot enforce
                // a downward cap on enchanted books.
                EnchantmentHelper.setEnchantmentLevel(enchantmentTag, normalizedLevel);
            }
        }
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        return hasStack && !resultSlots.getItem(RESULT_SLOT).isEmpty()
                && QAnvilCosts.canAfford(player, getCurrencyId(), getCurrencyCost(), getHealthCost());
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        if (takeEventInProgress) {
            takeEventCompleted = true;
        }
        if (!QAnvilCosts.charge(player, getCurrencyId(), getCurrencyCost(), getHealthCost())) {
            return;
        }

        if (materialCost >= 0) {
            repairItemCountCost = materialCost;
        }

        // Let vanilla handle input consumption, repair hooks, and the normal anvil container behavior.
        // The private vanilla XP cost has already been set to zero by createResult().
        setMaximumCost(0);
        super.onTake(player, stack);
    }

    public double getCurrencyCost() {
        return currencyCost.get();
    }

    public double getHealthCost() {
        return healthCost.get() / (double) HEALTH_COST_SCALE;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public String getCurrencyDisplayName() {
        return currencyDisplayName == null || currencyDisplayName.isEmpty()
                ? getCurrencyId() : currencyDisplayName;
    }

    public boolean hasQAnvilResult() {
        return !resultSlots.getItem(RESULT_SLOT).isEmpty();
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText == null ? "" : promptText;
    }

    public void setCurrencyInfo(String currencyId, String displayName) {
        if (currencyId != null && !currencyId.isBlank()) {
            this.currencyId = currencyId;
        }
        this.currencyDisplayName = displayName == null || displayName.isEmpty()
                ? this.currencyId : displayName;
    }

    public boolean canAfford(Player player) {
        return player.level().isClientSide
                ? affordability.get() != 0
                : QAnvilCosts.canAfford(player, getCurrencyId(), getCurrencyCost(), getHealthCost());
    }

    private void applyCosts(double currency, double health) {
        currencyCost.set(toCurrencyNetworkCost(currency));
        healthCost.set(toHealthNetworkCost(health));
    }

    private void clearCosts() {
        currencyCost.set(0);
        healthCost.set(0);
        affordability.set(0);
        materialCost = -1;
    }

    private static int toHealthNetworkCost(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.round(value * HEALTH_COST_SCALE));
    }

    private static int toCurrencyNetworkCost(double value) {
        double normalized = QAnvilCosts.normalizeCurrencyCost(value);
        if (!Double.isFinite(normalized) || normalized <= 0.0D) {
            return 0;
        }
        return (int) normalized;
    }

    private static int inputStackLimit(ItemStack stack) {
        // Keep genuinely non-stackable items, such as enchanted books, at one.
        return !stack.isEmpty() && stack.getMaxStackSize() <= 1
                ? 1
                : QAnvilConfig.maxInputStackSize();
    }

    private static final class QAnvilArmorSlot extends Slot {
        private final Player owner;
        private final EquipmentSlot equipmentSlot;
        private final ResourceLocation emptyIcon;

        private QAnvilArmorSlot(Inventory inventory, int slotIndex, int x, int y,
                                EquipmentSlot equipmentSlot, ResourceLocation emptyIcon) {
            super(inventory, slotIndex, x, y);
            this.owner = inventory.player;
            this.equipmentSlot = equipmentSlot;
            this.emptyIcon = emptyIcon;
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            // Vanilla InventoryMenu fires the equip hook with the previous stack
            // before the slot contents are replaced. Doing the same here keeps
            // equipment changes, such as attribute updates and Forge events,
            // identical to equipping through the player inventory.
            if (Equipable.get(stack) != null) {
                owner.onEquipItem(equipmentSlot, getItem(), stack);
            }
            super.setByPlayer(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(equipmentSlot, owner);
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = getItem();
            return !stack.isEmpty() && !player.isCreative() && EnchantmentHelper.hasBindingCurse(stack)
                    ? false
                    : super.mayPickup(player);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, emptyIcon);
        }
    }

    private static final class QAnvilInputSlot extends Slot {
        private QAnvilInputSlot(net.minecraft.world.Container container, int slotIndex, int x, int y) {
            super(container, slotIndex, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return QAnvilConfig.maxInputStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return inputStackLimit(stack);
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int amount) {
            if (stack.isEmpty() || !mayPlace(stack)) {
                return stack;
            }

            ItemStack existing = getItem();
            int transferable = Math.min(Math.min(amount, stack.getCount()),
                    getMaxStackSize(stack) - existing.getCount());
            if (transferable <= 0) {
                return stack;
            }

            if (existing.isEmpty()) {
                setByPlayer(stack.split(transferable));
            } else if (ItemStack.isSameItemSameTags(existing, stack)) {
                stack.shrink(transferable);
                existing.grow(transferable);
                setByPlayer(existing);
            }
            return stack;
        }
    }

    private static final class QAnvilInputContainer extends SimpleContainer {
        private final QAnvilMenu menu;

        private QAnvilInputContainer(QAnvilMenu menu) {
            super(2);
            this.menu = menu;
        }

        @Override
        public int getMaxStackSize() {
            return QAnvilConfig.maxInputStackSize();
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!stack.isEmpty()) {
                int maxSize = inputStackLimit(stack);
                if (stack.getCount() > maxSize) {
                    stack = stack.copy();
                    stack.setCount(maxSize);
                }
            }
            super.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            menu.slotsChanged(this);
        }
    }
}
