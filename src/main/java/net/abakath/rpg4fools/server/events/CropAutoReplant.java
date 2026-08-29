package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.world.CropHarvest;
import net.abakath.rpg4fools.world.CropSeasons;
import net.abakath.rpg4fools.world.RegrowingCropBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;

/**
 * Harvests a ripe crop on right click and sows it again in the same tick.
 *
 * <p>Saves the break-and-replant dance without changing what a field yields: the drops come from
 * the crop's own loot table, so tools and Fortune still decide the numbers, and one seed is taken
 * back out of them to pay for the replant. Whether a crop can be picked this way is not a property
 * of the crop, so nothing is added to {@link net.abakath.rpg4fools.world.CropDefinition} for it;
 * the crops tag decides, the same authority {@link CropSeasons} and {@link SeasonPlantingGate}
 * answer to.
 *
 * <p>Only CropBlock qualifies. That is the shape this can put back - one age property counted from
 * zero - and it happens to exclude exactly the plants that would be wrong to reset: melon and
 * pumpkin stems, which are not what the player is harvesting, and berry bushes, which pick
 * themselves in {@link net.abakath.rpg4fools.world.ModBerryBushBlock}. Regrowing crops are a
 * CropBlock and so have to be turned away by name.
 *
 * <p>Seasons are left alone deliberately. A crop is only mature while it is in season, since
 * {@link SeasonChangeSweep} kills the field the moment the season turns, so an out of season check
 * here could never fire.
 */
public class CropAutoReplant {
  public static void register() {
    UseBlockCallback.EVENT.register(CropAutoReplant::onUseBlock);
  }

  /**
   * Runs on the server only, unlike the planting gate next door.
   *
   * <p>The gate refuses an interaction, and a refusal has to reach the client before it predicts
   * the placement. This grants one instead, and the client has nothing to predict from: the loot
   * roll is the server's. Passing on the client lets vanilla send the use packet as usual, and the
   * server copy of this handler is what answers it.
   */
  private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    if (!(world instanceof ServerWorld serverWorld)) {
      return ActionResult.PASS;
    }

    // Sneaking is how a player says they meant the vanilla interaction - placing a block into the
    // crop's space, most of all. Harvesting through that would make a tilled row impossible to
    // build on.
    if (player.isSneaking()) {
      return ActionResult.PASS;
    }

    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);

    if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMature(state)) {
      return ActionResult.PASS;
    }

    // A crop that fruits again is picked, not torn up, and it does that itself in onUse - the same
    // division of labour the berry bushes already keep. Replanting one would throw away the plant
    // this feature exists to leave standing.
    if (crop instanceof RegrowingCropBlock) {
      return ActionResult.PASS;
    }

    if (!CropSeasons.isCrop(state)) {
      return ActionResult.PASS;
    }

    harvest(serverWorld, player, hand, pos, state, crop);

    return ActionResult.SUCCESS;
  }

  private static void harvest(ServerWorld world, PlayerEntity player, Hand hand, BlockPos pos,
                              BlockState state, CropBlock crop) {
    ItemStack tool = player.getStackInHand(hand);
    List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, tool);

    takeSeed(drops, crop.getPickStack(world, pos, state));

    CropHarvest.give(player, drops);

    BlockState sown = crop.withAge(0);
    world.setBlockState(pos, sown, Block.NOTIFY_LISTENERS);
    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, sown));
    world.playSound(null, pos, SoundEvents.ITEM_CROP_PLANT, SoundCategory.BLOCKS,
            1.0F, 0.8F + world.random.nextFloat() * 0.4F);
  }

  /**
   * Removes one seed from the drops, the cost of sowing the crop again.
   *
   * <p>Nothing is taken when the roll produced no seed. Wheat is the case that matters: its table
   * can give none at all, and refusing the harvest over that would leave the player clicking a ripe
   * crop that does nothing. A free replant costs one seed's worth of nothing, since the seed the
   * player would have replanted by hand is the one that never dropped.
   */
  private static void takeSeed(List<ItemStack> drops, ItemStack seed) {
    for (int i = 0; i < drops.size(); i++) {
      ItemStack drop = drops.get(i);

      if (!drop.isOf(seed.getItem())) {
        continue;
      }

      drop.decrement(1);

      if (drop.isEmpty()) {
        drops.remove(i);
      }

      return;
    }
  }
}
