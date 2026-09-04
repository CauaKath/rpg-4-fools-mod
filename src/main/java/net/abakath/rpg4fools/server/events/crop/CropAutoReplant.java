package net.abakath.rpg4fools.server.events.crop;

import net.abakath.rpg4fools.server.events.season.SeasonChangeSweep;
import net.abakath.rpg4fools.server.events.season.SeasonPlantingGate;
import net.abakath.rpg4fools.world.bush.ModBerryBushBlock;
import net.abakath.rpg4fools.world.crop.CropDefinition;
import net.abakath.rpg4fools.world.crop.CropHarvest;
import net.abakath.rpg4fools.world.season.CropSeasons;
import net.abakath.rpg4fools.world.crop.RegrowingCropBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;

/**
 * Harvests a ripe crop on right click and sows it again in the same tick.
 *
 * <p>Saves the break-and-replant dance without changing what a field yields: the drops come from
 * the crop's own loot table and land at the plant's feet the way breaking it would, so tools and
 * Fortune still decide the numbers, and one seed is taken back out of them to pay for the
 * replant. Whether a crop can be picked this way is not a property
 * of the crop, so nothing is added to {@link net.abakath.rpg4fools.world.crop.CropDefinition} for it;
 * the crops tag decides, the same authority {@link CropSeasons} and {@link SeasonPlantingGate}
 * answer to.
 *
 * <p>Only CropBlock qualifies. That is the shape this can put back - one age property counted from
 * zero - and it happens to exclude exactly the plants that would be wrong to reset: melon and
 * pumpkin stems, which are not what the player is harvesting, and berry bushes, which pick
 * themselves in {@link net.abakath.rpg4fools.world.bush.ModBerryBushBlock}. Regrowing crops are a
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
  private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    if (!(world instanceof ServerLevel serverWorld)) {
      return InteractionResult.PASS;
    }

    // Sneaking is how a player says they meant the vanilla interaction - placing a block into the
    // crop's space, most of all. Harvesting through that would make a tilled row impossible to
    // build on.
    if (player.isShiftKeyDown()) {
      return InteractionResult.PASS;
    }

    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);

    if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
      return InteractionResult.PASS;
    }

    // A crop that fruits again is picked, not torn up, and it does that itself in onUse - the same
    // division of labour the berry bushes already keep. Replanting one would throw away the plant
    // this feature exists to leave standing.
    if (crop instanceof RegrowingCropBlock) {
      return InteractionResult.PASS;
    }

    if (!CropSeasons.isCrop(state)) {
      return InteractionResult.PASS;
    }

    harvest(serverWorld, player, hand, pos, state, crop);

    return InteractionResult.SUCCESS;
  }

  /**
   * Takes the crop and sows it again. Package visible because {@link HoeAreaHarvest} sweeps a square
   * of crops through this same method, so that a wider harvest cannot drift from a single one.
   */
  static void harvest(ServerLevel world, Player player, InteractionHand hand, BlockPos pos,
                      BlockState state, CropBlock crop) {
    ItemStack tool = player.getItemInHand(hand);
    List<ItemStack> drops = Block.getDrops(state, world, pos, null, player, tool);

    takeSeed(drops, state.getCloneItemStack(world, pos, false));

    CropHarvest.drop(world, pos, pos.below(), drops);

    BlockState sown = crop.getStateForAge(0);
    world.setBlock(pos, sown, Block.UPDATE_CLIENTS);
    world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, sown));
    world.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS,
            1.0F, 0.8F + world.getRandom().nextFloat() * 0.4F);
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

      if (!drop.is(seed.getItem())) {
        continue;
      }

      drop.shrink(1);

      if (drop.isEmpty()) {
        drops.remove(i);
      }

      return;
    }
  }
}
