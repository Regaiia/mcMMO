package com.gmail.nossr50.skills.repair;

import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.spout.SpoutSounds;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.Skills;
import com.gmail.nossr50.util.Users;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.SkillType;
import com.gmail.nossr50.locale.LocaleLoader;

public class Repair {

    private static Random random = new Random();
    private static Config configInstance = Config.getInstance();
    private static Permissions permInstance = Permissions.getInstance();

    /**
     * Handle the XP gain for repair events.
     *
     * @param player Player repairing the item
     * @param PP PlayerProfile of the repairing player
     * @param is Item being repaired
     * @param durabilityBefore Durability of the item before repair
     * @param modify Amount to modify the durability by
     * @param boost True if the modifier is a boost, false if the modifier is a reduction
     */
    protected static void xpHandler(Player player, PlayerProfile PP, short durabilityBefore, short durabilityAfter, double modify) {
        short dif = (short) (durabilityBefore - durabilityAfter);

        dif = (short) (dif * modify);

        PP.addXP(player, SkillType.REPAIR, dif * 10);
        Skills.XpCheckSkill(SkillType.REPAIR, player);

        //CLANG CLANG
        if (mcMMO.p.spoutEnabled) {
            SpoutSounds.playRepairNoise(player, mcMMO.p);
        }
    }

    /**
     * Get current Arcane Forging rank.
     *
     * @param skillLevel The skill level of the player whose rank is being checked
     * @return The player's current Arcane Forging rank
     */
    public static int getArcaneForgingRank(PlayerProfile PP) {
        int skillLevel = PP.getSkillLevel(SkillType.REPAIR);

        if (skillLevel >= configInstance.getArcaneForgingRankLevels4()) {
            return 4;
        }
        else if (skillLevel >= configInstance.getArcaneForgingRankLevels3()) {
            return 3;
        }
        else if (skillLevel >= configInstance.getArcaneForgingRankLevels2()) {
            return 2;
        }
        else if (skillLevel >= configInstance.getArcaneForgingRankLevels1()) {
            return 1;
        }
        else {
            return 0;
        }
    }

    /**
     * Handles removing & downgrading enchants.
     *
     * @param player Player repairing the item
     * @param is Item being repaired
     */
    protected static void addEnchants(Player player, ItemStack is) {
        Map<Enchantment, Integer> enchants = is.getEnchantments();

        if (enchants.size() == 0) {
            return;
        }

        int rank = getArcaneForgingRank(Users.getProfile(player));

        if (rank == 0 || !permInstance.arcaneForging(player)) {
            for (Enchantment x : enchants.keySet()) {
                is.removeEnchantment(x);
            }
            player.sendMessage(LocaleLoader.getString("Repair.Arcane.Lost"));
            return;
        }

        boolean downgraded = false;

        for (Entry<Enchantment, Integer> enchant : enchants.entrySet()) {
            Enchantment enchantment = enchant.getKey();

            if (random.nextInt(100) <= getEnchantChance(rank)) {
                int enchantLevel = enchant.getValue();

                if (configInstance.getArcaneForgingDowngradeEnabled() && enchantLevel > 1) {
                    if (random.nextInt(100) <= getDowngradeChance(rank)) {
                        is.addEnchantment(enchantment, enchantLevel--);
                        downgraded = true;
                    }
                }
            }
            else {
                is.removeEnchantment(enchantment);
            }
        }

        Map<Enchantment, Integer> newEnchants = is.getEnchantments();

        if (newEnchants.isEmpty()) {
            player.sendMessage(LocaleLoader.getString("Repair.Arcane.Fail"));
        }
        else if (downgraded || newEnchants.size() < enchants.size()) {
            player.sendMessage(LocaleLoader.getString("Repair.Arcane.Downgrade"));
        }
        else {
            player.sendMessage(LocaleLoader.getString("Repair.Arcane.Perfect"));
        }
    }

    /**
     * Gets chance of keeping enchantment during repair.
     *
     * @param rank Arcane Forging rank
     * @return The chance of keeping the enchantment
     */
    public static int getEnchantChance(int rank) {
        switch (rank) {
        case 4:
            return configInstance.getArcaneForgingKeepEnchantsChanceRank4();

        case 3:
            return configInstance.getArcaneForgingKeepEnchantsChanceRank3();

        case 2:
            return configInstance.getArcaneForgingKeepEnchantsChanceRank2();

        case 1:
            return configInstance.getArcaneForgingKeepEnchantsChanceRank1();

        default:
            return 0;
        }
    }

    /**
     * Gets chance of enchantment being downgraded during repair.
     *
     * @param rank Arcane Forging rank
     * @return The chance of the enchantment being downgraded
     */
    public static int getDowngradeChance(int rank) {
        switch (rank) {
        case 4:
            return configInstance.getArcaneForgingDowngradeChanceRank4();

        case 3:
            return configInstance.getArcaneForgingDowngradeChanceRank3();

        case 2:
            return configInstance.getArcaneForgingDowngradeChanceRank2();

        case 1:
            return configInstance.getArcaneForgingDowngradeChanceRank1();

        default:
            return 100;
        }
    }

    /**
     * Computes repair bonuses.
     *
     * @param player The player repairing an item
     * @param skillLevel the skillLevel of the player in Repair
     * @param durability The durability of the item being repaired
     * @param repairAmount The base amount of durability repaired to the item
     * @return The final amount of durability repaired to the item
     */
    protected static short repairCalculate(Player player, int skillLevel, short durability, int repairAmount) {
        float bonus = (float) skillLevel / 500;

        if (permInstance.repairMastery(player)) {
            bonus = (repairAmount * bonus);
            repairAmount += bonus;
        }

        if (checkPlayerProcRepair(player)) {
            repairAmount = (short) (repairAmount * 2);
        }

        durability -= repairAmount;

        if (durability < 0) {
            durability = 0;
        }

        return durability;
    }

    /**
     * Checks for Super Repair bonus.
     *
     * @param player The player repairing an item
     * @return true if bonus granted, false otherwise
     */
    public static boolean checkPlayerProcRepair(Player player) {
        final int MAX_BONUS_LEVEL = 1000;

        int skillLevel = Users.getProfile(player).getSkillLevel(SkillType.REPAIR);

        if ((skillLevel > MAX_BONUS_LEVEL || random.nextInt(1000) <= skillLevel) && permInstance.repairBonus(player)) {
            player.sendMessage(LocaleLoader.getString("Repair.Skills.FeltEasy"));
            return true;
        }

        return false;
    }

    /**
     * Handles notifications for placing an anvil.
     *
     * @param player The player placing the anvil
     * @param anvilID The item ID of the anvil block
     */
    public static void placedAnvilCheck(Player player, int anvilID) {
        PlayerProfile PP = Users.getProfile(player);

        if (!PP.getPlacedAnvil()) {
            if (mcMMO.p.spoutEnabled) {
                SpoutPlayer sPlayer = SpoutManager.getPlayer(player);

                if (sPlayer.isSpoutCraftEnabled()) {
                    sPlayer.sendNotification("[mcMMO] Anvil Placed", "Right click to repair!", Material.getMaterial(anvilID)); //TODO: Use Locale
                }
            }
            else {
                player.sendMessage(LocaleLoader.getString("Repair.Listener.Anvil"));
            }

            PP.togglePlacedAnvil();
        }
    }
}
