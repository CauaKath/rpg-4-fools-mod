package net.abakath.rpg4fools.server.events.crop;

import net.abakath.rpg4fools.init.ModItems;
import net.abakath.rpg4fools.server.PlantedCrops;
import net.abakath.rpg4fools.world.crop.CropItems;
import net.abakath.rpg4fools.world.season.CropSeasons;
import net.abakath.rpg4fools.world.trellis.CropSticks;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Remembers which crops a player planted, so the seasons can leave those alone.
 *
 * <p>Fabric has no event for a block being placed, so a planting is caught by watching for the use
 * that would cause one and looking at the spot afterwards. The check runs on the server's own task
 * queue, which is reached after the placement it is asking about has already happened; if the
 * placement failed, or the player was holding a seed and clicked something else entirely, there is
 * simply no crop there and nothing is recorded.
 *
 * <p>A crop stick counts as a planting too. Sticks are work a player put into a plot, and a field
 * that resowed itself over a trellis somebody built would be taking that away. The position recorded
 * is the foot of the column rather than the block the stick landed in, because the foot is the spot
 * the season rules ask about.
 */
public final class CropOwnership {
  private CropOwnership() {
  }

  public static void register() {
    UseBlockCallback.EVENT.register(CropOwnership::onUseBlock);

    // Breaking it hands the plot back. Harvesting does not: the replanting hook sows the same spot
    // again, and a field a player keeps cutting is still a field a player planted.
    PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
      if (world instanceof ServerLevel server && (CropSeasons.isCrop(state) || CropSticks.isColumn(state))) {
        PlantedCrops.get(server).forget(pos);
      }
    });
  }

  private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    if (!(world instanceof ServerLevel server)) {
      return InteractionResult.PASS;
    }

    ItemStack stack = player.getItemInHand(hand);

    if (CropItems.plantedBy(stack).isEmpty() && !stack.is(ModItems.CROP_STICK)) {
      return InteractionResult.PASS;
    }

    // Both the block clicked and the one a placement would land in. A seed goes into the space the
    // player pointed past; a stick can go either there or into the column already standing in front
    // of them, and which one it was is not worth working out twice.
    BlockPos clicked = hit.getBlockPos();
    BlockPos offset = clicked.relative(hit.getDirection());

    server.getServer().execute(() -> {
      claim(server, clicked);
      claim(server, offset);
    });

    return InteractionResult.PASS;
  }

  /** Records the foot of whatever grew or was built here, if anything did. */
  public static void claim(ServerLevel world, BlockPos pos) {
    BlockState state = world.getBlockState(pos);

    if (CropSticks.isColumn(state)) {
      PlantedCrops.get(world).remember(CropSticks.base(world, pos));
      return;
    }

    if (CropSeasons.isCrop(state)) {
      PlantedCrops.get(world).remember(pos);
    }
  }
}
