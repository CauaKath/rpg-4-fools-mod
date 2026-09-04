package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.world.CropSeasons;
import net.abakath.rpg4fools.world.CropSticks;
import net.abakath.rpg4fools.world.CropWalls;
import net.abakath.rpg4fools.world.RegrowingCropBlock;
import net.abakath.rpg4fools.world.StickedCropBlock;
import net.abakath.rpg4fools.world.WalledCropBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.Set;

/**
 * Harvests a square of ripe crops when the click that would have taken one came from a hoe.
 *
 * <p>Reach is the hoe's, and nothing else about a harvest changes: each plant in the square is taken
 * by its own rule, so a field of wheat is torn up and resown exactly as {@link CropAutoReplant}
 * would have done it one click at a time, and a tomato on a trellis is picked and left standing
 * exactly as clicking it would. This is a wider click, not a different kind of harvest.
 *
 * <p>The square is flat, at the height of the crop that was clicked. Fields are flat, and a hoe that
 * reached up and down would pull in the neighbouring terrace nobody was standing on.
 *
 * <p>Runs before the auto replant so that the whole square is settled in one place. A wooden or
 * stone hoe reaches nothing extra and is passed straight through instead of being handled here,
 * which leaves the single crop case with exactly the code it had before - and costs the player no
 * durability for a click that did no more than their bare hand would.
 *
 * <p>Berry bushes fall out of this on their own: they are not a CropBlock, and picking a row of them
 * with a farming tool is not what the tool is for.
 */
public final class HoeAreaHarvest {
  private HoeAreaHarvest() {
  }

  public static void register() {
    UseBlockCallback.EVENT.register(HoeAreaHarvest::onUseBlock);
  }

  /**
   * Server only, for the same reason the auto replant is.
   *
   * <p>The drops are rolled from loot tables the client does not have, so there is nothing here for
   * it to predict. Passing on the client lets vanilla send the use packet, and the server copy of
   * this handler answers it.
   */
  private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    if (!(world instanceof ServerLevel serverWorld)) {
      return InteractionResult.PASS;
    }

    // Sneaking means the player wants the vanilla interaction, the same bargain the auto replant
    // keeps. A wider harvest is a bigger accident to have, not a smaller one.
    if (player.isShiftKeyDown()) {
      return InteractionResult.PASS;
    }

    ItemStack tool = player.getItemInHand(hand);

    if (!(tool.getItem() instanceof HoeItem)) {
      return InteractionResult.PASS;
    }

    int radius = reachOf(tool);

    if (radius == 0) {
      return InteractionResult.PASS;
    }

    BlockPos clicked = hit.getBlockPos();

    // Nothing ripe under the cursor is not a harvest that happened to hit air; it is a click that
    // meant something else, and the crops around it are not the player's target.
    if (!harvestable(serverWorld.getBlockState(clicked))) {
      return InteractionResult.PASS;
    }

    if (sweep(serverWorld, player, hand, clicked, radius)) {
      tool.hurtAndBreak(1, player, hand);
    }

    return InteractionResult.SUCCESS;
  }

  /**
   * How far past the clicked crop the hoe reaches, in blocks.
   *
   * <p>Wood and stone reach nothing, which is the crop the player clicked and no more. Iron and gold
   * take the ring around it, diamond two rings, netherite three - a 3x3, a 5x5 and a 7x7. Gold sits
   * with iron rather than with wood because it is an iron tier tool everywhere else it matters, and
   * a golden hoe that harvested like a wooden one would only read as an oversight.
   *
   * <p>A hoe no longer carries its tool material where anything can read it, so the vanilla six are
   * named outright and everything else falls to durability, the one number every tool has to give.
   * That puts a modded hoe somewhere sensible on the ladder instead of at the bottom. It also takes
   * copper, which did not exist when these rungs were written, at its durability rather than
   * guessing - which is the same treatment any other material this mod has not heard of gets.
   *
   * <p>Gold has to be named because it is the one vanilla hoe the durability ladder gets wrong: 32
   * uses would put it below wood, where an iron tier tool everywhere else does not belong.
   */
  private static int reachOf(ItemStack tool) {
    Item hoe = tool.getItem();

    if (hoe == Items.WOODEN_HOE || hoe == Items.STONE_HOE) {
      return 0;
    }

    if (hoe == Items.IRON_HOE || hoe == Items.GOLDEN_HOE) {
      return 1;
    }

    if (hoe == Items.DIAMOND_HOE) {
      return 2;
    }

    if (hoe == Items.NETHERITE_HOE) {
      return 3;
    }

    return byDurability(tool.getMaxDamage());
  }

  private static int byDurability(int durability) {
    if (durability < 150) {
      return 0;
    }

    if (durability < 500) {
      return 1;
    }

    if (durability < 1600) {
      return 2;
    }

    return 3;
  }

  /**
   * Takes every ripe crop in the square, and answers whether anything came of it.
   *
   * <p>Plants that are harvested whole from a root - a trellis column, a wall of cucumbers - can put
   * several of their blocks inside one square, and each of those blocks would harvest the entire
   * plant. Remembering the roots already taken is what keeps one plant worth one harvest however
   * much of it the square happens to cover.
   */
  private static boolean sweep(ServerLevel world, Player player, InteractionHand hand, BlockPos centre, int radius) {
    Set<BlockPos> taken = new HashSet<>();
    boolean harvested = false;

    for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, 0, -radius), centre.offset(radius, 0, radius))) {
      harvested |= harvest(world, player, hand, pos.immutable(), taken);
    }

    return harvested;
  }

  private static boolean harvest(ServerLevel world, Player player, InteractionHand hand, BlockPos pos,
                                 Set<BlockPos> taken) {
    BlockState state = world.getBlockState(pos);

    if (!harvestable(state)) {
      return false;
    }

    // A crop that fruits again picks itself in its own onUse, where the whole plant is found and the
    // stump is left standing. Calling that is what keeps this handler from having a second opinion
    // about what picking a tomato means.
    if (state.getBlock() instanceof RegrowingCropBlock regrowing) {
      if (!taken.add(plantOf(world, state, pos))) {
        return false;
      }

      return regrowing.useWithoutItem(state, world, pos, player, at(pos)) == InteractionResult.SUCCESS;
    }

    CropAutoReplant.harvest(world, player, hand, pos, state, (CropBlock) state.getBlock());

    return true;
  }

  /** Ripe, and a crop this mod harvests by hand. The same two questions the auto replant asks. */
  private static boolean harvestable(BlockState state) {
    return state.getBlock() instanceof CropBlock crop
            && crop.isMaxAge(state)
            && CropSeasons.isCrop(state);
  }

  /**
   * The block a harvest of this crop is really against - its root, for the plants that have one.
   *
   * <p>Everything else is one block, one plant, and stands for itself.
   */
  private static BlockPos plantOf(BlockGetter world, BlockState state, BlockPos pos) {
    if (state.getBlock() instanceof WalledCropBlock) {
      return CropWalls.root(state, pos);
    }

    if (state.getBlock() instanceof StickedCropBlock) {
      return CropSticks.plantBase(world, pos);
    }

    return pos;
  }

  /**
   * A hit on the crop being swept, standing in for the one the player actually made.
   *
   * <p>The blocks reached here are ripe, and a ripe crop's onUse never looks at where it was struck;
   * only the bone meal path the player did not take would. Passing the player's own hit would name
   * the wrong block, which is the worse of the two lies.
   */
  private static BlockHitResult at(BlockPos pos) {
    return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
  }
}
