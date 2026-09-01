package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.world.CropSeasons;
import net.abakath.rpg4fools.world.CropSticks;
import net.abakath.rpg4fools.world.CropWalls;
import net.abakath.rpg4fools.world.RegrowingCropBlock;
import net.abakath.rpg4fools.world.StickedCropBlock;
import net.abakath.rpg4fools.world.WalledCropBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

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
  private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    if (!(world instanceof ServerWorld serverWorld)) {
      return ActionResult.PASS;
    }

    // Sneaking means the player wants the vanilla interaction, the same bargain the auto replant
    // keeps. A wider harvest is a bigger accident to have, not a smaller one.
    if (player.isSneaking()) {
      return ActionResult.PASS;
    }

    ItemStack tool = player.getStackInHand(hand);

    if (!(tool.getItem() instanceof HoeItem hoe)) {
      return ActionResult.PASS;
    }

    int radius = reachOf(hoe);

    if (radius == 0) {
      return ActionResult.PASS;
    }

    BlockPos clicked = hit.getBlockPos();

    // Nothing ripe under the cursor is not a harvest that happened to hit air; it is a click that
    // meant something else, and the crops around it are not the player's target.
    if (!harvestable(serverWorld.getBlockState(clicked))) {
      return ActionResult.PASS;
    }

    if (sweep(serverWorld, player, hand, clicked, radius)) {
      tool.damage(1, player, LivingEntity.getSlotForHand(hand));
    }

    return ActionResult.SUCCESS;
  }

  /**
   * How far past the clicked crop the hoe reaches, in blocks.
   *
   * <p>Wood and stone reach nothing, which is the crop the player clicked and no more. Iron and gold
   * take the ring around it, diamond two rings, netherite three - a 3x3, a 5x5 and a 7x7. Gold sits
   * with iron rather than with wood because it is an iron tier tool everywhere else it matters, and
   * a golden hoe that harvested like a wooden one would only read as an oversight.
   *
   * <p>A material this mod has never heard of is placed by its durability, the one number every tool
   * material has to give. It puts a modded hoe somewhere sensible on the ladder instead of at the
   * bottom, and the tiers above are still what decide the vanilla ones.
   */
  private static int reachOf(HoeItem hoe) {
    ToolMaterial material = hoe.getMaterial();

    if (material == ToolMaterials.WOOD || material == ToolMaterials.STONE) {
      return 0;
    }

    if (material == ToolMaterials.IRON || material == ToolMaterials.GOLD) {
      return 1;
    }

    if (material == ToolMaterials.DIAMOND) {
      return 2;
    }

    if (material == ToolMaterials.NETHERITE) {
      return 3;
    }

    return byDurability(material.getDurability());
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
  private static boolean sweep(ServerWorld world, PlayerEntity player, Hand hand, BlockPos centre, int radius) {
    Set<BlockPos> taken = new HashSet<>();
    boolean harvested = false;

    for (BlockPos pos : BlockPos.iterate(centre.add(-radius, 0, -radius), centre.add(radius, 0, radius))) {
      harvested |= harvest(world, player, hand, pos.toImmutable(), taken);
    }

    return harvested;
  }

  private static boolean harvest(ServerWorld world, PlayerEntity player, Hand hand, BlockPos pos,
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

      return regrowing.onUse(state, world, pos, player, at(pos)) == ActionResult.SUCCESS;
    }

    CropAutoReplant.harvest(world, player, hand, pos, state, (CropBlock) state.getBlock());

    return true;
  }

  /** Ripe, and a crop this mod harvests by hand. The same two questions the auto replant asks. */
  private static boolean harvestable(BlockState state) {
    return state.getBlock() instanceof CropBlock crop
            && crop.isMature(state)
            && CropSeasons.isCrop(state);
  }

  /**
   * The block a harvest of this crop is really against - its root, for the plants that have one.
   *
   * <p>Everything else is one block, one plant, and stands for itself.
   */
  private static BlockPos plantOf(BlockView world, BlockState state, BlockPos pos) {
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
    return new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
  }
}
