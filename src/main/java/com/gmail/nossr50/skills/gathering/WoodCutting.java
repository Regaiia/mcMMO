package com.gmail.nossr50.skills.gathering;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Tree;
import org.bukkit.event.block.BlockBreakEvent;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.SkillType;
import com.gmail.nossr50.datatypes.mods.CustomBlock;
import com.gmail.nossr50.events.fake.FakePlayerAnimationEvent;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.spout.SpoutSounds;
import com.gmail.nossr50.util.BlockChecks;
import com.gmail.nossr50.util.Combat;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.ModChecks;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.Skills;
import com.gmail.nossr50.util.Users;

import org.getspout.spoutapi.sound.SoundEffect;

public class WoodCutting {

    private static Random random = new Random();

    /**
     * Handle the Tree Feller ability.
     *
     * @param event Event to modify
     */
    public static void treeFeller(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block firstBlock = event.getBlock();
        PlayerProfile PP = Users.getProfile(player);
        ArrayList<Block> toBeFelled = new ArrayList<Block>();

        /* NOTE: Tree Feller will cut upwards like how you actually fell trees */
        processTreeFelling(firstBlock, toBeFelled);
        removeBlocks(toBeFelled, player, PP);
    }

    /**
     * Handles removing & dropping the blocks from Tree Feller.
     *
     * @param toBeFelled List of Blocks to be removed from the tree
     * @param player The player using the ability
     * @param PP The PlayerProfile of the player
     */
    private static void removeBlocks(ArrayList<Block> toBeFelled, Player player, PlayerProfile PP) {
        if (toBeFelled.size() >= Config.getInstance().getTreeFellerThreshold()) {
            player.sendMessage(LocaleLoader.getString("Woodcutting.Skills.TreeFellerThreshold"));
            return;
        }

        int durabilityLoss = durabilityLossCalulate(toBeFelled);
        int xp = 0;
        ItemStack inHand = player.getItemInHand();

        /* Damage the tool */
        inHand.setDurability((short) (inHand.getDurability() + durabilityLoss));

        /* This is to prevent using wood axes everytime you tree fell */
        if ((inHand.getDurability() + durabilityLoss >= inHand.getType().getMaxDurability()) || inHand.getType().equals(Material.AIR)) {
            player.sendMessage(LocaleLoader.getString("Woodcutting.Skills.TreeFeller.Splinter"));

            int health = player.getHealth();

            if (health >= 2) {
                Combat.dealDamage(player, random.nextInt(health - 1));
            }
            return;
        }

        //Prepare ItemStacks
        ItemStack item = null;
        ItemStack oak = new ItemStack(Material.LOG, 1, (short) 0, TreeSpecies.GENERIC.getData());
        ItemStack spruce = new ItemStack(Material.LOG, 1, (short) 0, TreeSpecies.REDWOOD.getData());
        ItemStack birch = new ItemStack(Material.LOG, 1, (short) 0, TreeSpecies.BIRCH.getData());
        ItemStack jungle = new ItemStack(Material.LOG, 1, (short) 0, TreeSpecies.JUNGLE.getData());
        
        for (Block x : toBeFelled) {
            if (Misc.blockBreakSimulate(x, player, true)) {
                if (Config.getInstance().getBlockModsEnabled()) {
                    CustomBlock block = ModChecks.getCustomBlock(x);
                    item = block.getItemDrop();

                    if (ModChecks.isCustomLogBlock(x)) {
                        if (!mcMMO.placeStore.isTrue(x)) {
                            WoodCutting.woodCuttingProcCheck(player, x);
                            xp = block.getXpGain();
                        }

                        /* Remove the block */
                        x.setData((byte) 0x0);
                        x.setType(Material.AIR);

                        Misc.dropItem(x.getLocation(), item);
                    }
                    else if (ModChecks.isCustomLeafBlock(x)) {
                        final int SAPLING_DROP_CHANCE = 10;

                        /* Remove the block */
                        x.setData((byte) 0x0);
                        x.setType(Material.AIR);

                        Misc.randomDropItem(x.getLocation(), item, SAPLING_DROP_CHANCE);
                    }
                }
                else if (x.getType() == Material.LOG) {
                    Tree tree = (Tree) x.getState().getData();
                    TreeSpecies species = tree.getSpecies();

                    switch (species) {
                    case GENERIC:
                        item = oak;
                        break;

                    case REDWOOD:
                        item = spruce;
                        break;

                    case BIRCH:
                        item = birch;
                        break;

                    case JUNGLE:
                        item = jungle;
                        break;

                    default:
                        break;
                    }

                    if (!mcMMO.placeStore.isTrue(x)) {
                        WoodCutting.woodCuttingProcCheck(player, x);

                        switch (species) {
                            case GENERIC:
                                xp += Config.getInstance().getWoodcuttingXPOak();
                                break;

                            case REDWOOD:
                                xp += Config.getInstance().getWoodcuttingXPSpruce();
                                break;

                            case BIRCH:
                                xp += Config.getInstance().getWoodcuttingXPBirch();
                                break;

                            case JUNGLE:
                                xp += Config.getInstance().getWoodcuttingXPJungle() / 4; //Nerf XP from Jungle Trees when using Tree Feller
                                break;

                            default:
                                break;
                        }
                    }

                    /* Remove the block */
                    x.setData((byte) 0x0);
                    x.setType(Material.AIR);

                    /* Drop the block */
                    Misc.dropItem(x.getLocation(), item);
                }
                else if (x.getType() == Material.LEAVES) {
                    final int SAPLING_DROP_CHANCE = 10;

                    item = new ItemStack(Material.SAPLING, 1, (short) 0, (byte) (x.getData() & 3)); //Drop the right type of sapling
                    Misc.randomDropItem(x.getLocation(), item, SAPLING_DROP_CHANCE);

                    //Remove the block
                    x.setData((byte) 0);
                    x.setType(Material.AIR);
                }
            }
        }

        if (Permissions.getInstance().woodcutting(player)) {
            PP.addXP(player, SkillType.WOODCUTTING, xp); //Tree Feller gives nerf'd XP
            Skills.XpCheckSkill(SkillType.WOODCUTTING, player);
        }
    }

    /**
     * Handle the calculations from Tree Feller.
     *
     * @param currentBlock The current block to be removed
     * @param toBeFelled The list of blocks left to be removed
     */
    private static void processTreeFelling(Block currentBlock, ArrayList<Block> toBeFelled) {
        Material type = currentBlock.getType();
        
        if (toBeFelled.size() >= Config.getInstance().getTreeFellerThreshold()) {
            return;
        }

        if (type.equals(Material.LOG) || type.equals(Material.LEAVES)) {
            toBeFelled.add(currentBlock);
        }
        else if (Config.getInstance().getBlockModsEnabled() && (ModChecks.isCustomLogBlock(currentBlock) || ModChecks.isCustomLeafBlock(currentBlock))) {
            toBeFelled.add(currentBlock);
        }

        Block xPositive = currentBlock.getRelative(1, 0, 0);
        Block xNegative = currentBlock.getRelative(-1, 0, 0);
        Block zPositive = currentBlock.getRelative(0, 0, 1);
        Block zNegative = currentBlock.getRelative(0, 0, -1);
        Block yPositive = currentBlock.getRelative(0, 1, 0);

        if (!mcMMO.placeStore.isTrue(currentBlock)) {
            if (!isTooAggressive(currentBlock, xPositive) && BlockChecks.treeFellerCompatible(xPositive) && !toBeFelled.contains(xPositive)) {
                processTreeFelling(xPositive, toBeFelled);
            }

            if (!isTooAggressive(currentBlock, xNegative) && BlockChecks.treeFellerCompatible(xNegative) && !toBeFelled.contains(xNegative)) {
                processTreeFelling(xNegative, toBeFelled);
            }

            if (!isTooAggressive(currentBlock, zPositive) && BlockChecks.treeFellerCompatible(zPositive) && !toBeFelled.contains(zPositive)) {
                processTreeFelling(zPositive, toBeFelled);
            }

            if (!isTooAggressive(currentBlock, zNegative) && BlockChecks.treeFellerCompatible(zNegative) && !toBeFelled.contains(zNegative)) {
                processTreeFelling(zNegative, toBeFelled);
            }
        }

        if (BlockChecks.treeFellerCompatible(yPositive)) {
            if(!mcMMO.placeStore.isTrue(currentBlock) && !toBeFelled.contains(yPositive)) {
                processTreeFelling(yPositive, toBeFelled);
            }
        }
    }

    /**
     * Check if Tree Feller is being too aggressive.
     *
     * @param currentBlock The current block being felled
     * @param newBlock The next block to be felled
     * @return true if Tree Feller is too aggressive, false otherwise
     */
    private static boolean isTooAggressive(Block currentBlock, Block newBlock) {
        Material currentType = currentBlock.getType();
        Material newType = newBlock.getType();

        if ((currentType.equals(Material.LEAVES) || currentType.equals(Material.AIR) || (Config.getInstance().getBlockModsEnabled() && ModChecks.isCustomLeafBlock(currentBlock))) && (newType.equals(Material.LEAVES) || newType.equals(Material.AIR) || (Config.getInstance().getBlockModsEnabled() && ModChecks.isCustomLeafBlock(currentBlock)))) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Check for double drops.
     *
     * @param player Player breaking the block
     * @param block The block being broken
     */
    private static void woodCuttingProcCheck(Player player, Block block) {
        final int MAX_SKILL_LEVEL = 1000;

        int skillLevel = Users.getProfile(player).getSkillLevel(SkillType.WOODCUTTING);
        byte type = block.getData();
        Material mat = Material.getMaterial(block.getTypeId());

        Tree tree = (Tree) block.getState().getData();
        TreeSpecies species = tree.getSpecies();

        if ((skillLevel > MAX_SKILL_LEVEL || random.nextInt(1000) <= skillLevel) && Permissions.getInstance().woodcuttingDoubleDrops(player)) {
            Config configInstance = Config.getInstance();
            ItemStack item;
            Location location;

            if (configInstance.getBlockModsEnabled() && ModChecks.isCustomLogBlock(block)) {
                item = ModChecks.getCustomBlock(block).getItemDrop();
                location = block.getLocation();
                Misc.dropItem(location, item);
                return;
            }
            else {
                item = new ItemStack(mat, 1, (short) 0, type);
                location = block.getLocation();
            }

            /* Drop the block */
            switch (species) {
            case GENERIC:
                if (configInstance.getOakDoubleDropsEnabled()) {
                    Misc.dropItem(location, item);
                }
                break;

            case REDWOOD:
                if (configInstance.getSpruceDoubleDropsEnabled()) {
                    Misc.dropItem(location, item);
                }
                break;

            case BIRCH:
                if (configInstance.getBirchDoubleDropsEnabled()) {
                    Misc.dropItem(location, item);
                }
                break;

            case JUNGLE:
                if (configInstance.getJungleDoubleDropsEnabled()) {
                    Misc.dropItem(location, item);
                }
                break;

            default:
                break;
            }
        }
    }

    /**
     * Check XP gain for woodcutting.
     *
     * @param player The player breaking the block
     * @param block The block being broken
     */
    public static void woodcuttingBlockCheck(Player player, Block block) {
        PlayerProfile PP = Users.getProfile(player);
        int xp = 0;
        TreeSpecies species = TreeSpecies.getByData(block.getData());

        if (mcMMO.placeStore.isTrue(block)) {
            return;
        }

        if (Config.getInstance().getBlockModsEnabled() && ModChecks.isCustomLogBlock(block)) {
            xp = ModChecks.getCustomBlock(block).getXpGain();
        }
        else {
            switch (species) {
            case GENERIC:
                xp += Config.getInstance().getWoodcuttingXPOak();
                break;

            case REDWOOD:
                xp += Config.getInstance().getWoodcuttingXPSpruce();
                break;

            case BIRCH:
                xp += Config.getInstance().getWoodcuttingXPBirch();
                break;

            case JUNGLE:
                xp += Config.getInstance().getWoodcuttingXPJungle();
                break;

            default:
                break;
            }
        }

        WoodCutting.woodCuttingProcCheck(player, block);
        PP.addXP(player, SkillType.WOODCUTTING, xp);
        Skills.XpCheckSkill(SkillType.WOODCUTTING, player);
    }

    /**
     * Handle the Leaf Blower ability.
     *
     * @param player Player using the ability
     * @param block Block being broken
     */
    public static void leafBlower(Player player, Block block) {
        FakePlayerAnimationEvent armswing = new FakePlayerAnimationEvent(player);
        mcMMO.p.getServer().getPluginManager().callEvent(armswing);

        if (Config.getInstance().getWoodcuttingRequiresTool()) {
            Skills.abilityDurabilityLoss(player.getItemInHand(), Config.getInstance().getAbilityToolDamage());
        }

        if (mcMMO.p.spoutEnabled) {
            SpoutSounds.playSoundForPlayer(SoundEffect.POP, player, block.getLocation());
        }
    }

    private static int durabilityLossCalulate(ArrayList<Block> toBeFelled) {
        int durabilityLoss = 0;
        for (Block x : toBeFelled) {
            if (x.getType().equals(Material.LOG) || (Config.getInstance().getBlockModsEnabled() && ModChecks.isCustomLogBlock(x))) {
                durabilityLoss++;
                durabilityLoss = durabilityLoss + Config.getInstance().getAbilityToolDamage();
            }
        }

        return durabilityLoss;
    }
}
