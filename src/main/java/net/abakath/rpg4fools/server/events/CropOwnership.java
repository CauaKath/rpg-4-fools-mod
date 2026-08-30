package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.server.PlantedCrops;
import net.abakath.rpg4fools.world.CropItems;
import net.abakath.rpg4fools.world.CropSeasons;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Remembers which crops a player planted, so the seasons can leave those alone.
 *
 * <p>Fabric has no event for a block being placed, so a planting is caught by watching for the use
 * that would cause one and looking at the spot afterwards. The check runs on the server's own task
 * queue, which is reached after the placement it is asking about has already happened; if the
 * placement failed, or the player was holding a seed and clicked something else entirely, there is
 * simply no crop there and nothing is recorded.
 */
public final class CropOwnership {
  private CropOwnership() {
  }

  public static void register() {
    UseBlockCallback.EVENT.register(CropOwnership::onUseBlock);

    // Breaking it hands the plot back. Harvesting does not: the replanting hook sows the same spot
    // again, and a field a player keeps cutting is still a field a player planted.
    PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
      if (world instanceof ServerWorld server && CropSeasons.isCrop(state)) {
        PlantedCrops.get(server).forget(pos);
      }
    });
  }

  private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    if (!(world instanceof ServerWorld server)) {
      return ActionResult.PASS;
    }

    ItemStack stack = player.getStackInHand(hand);

    if (CropItems.plantedBy(stack).isEmpty()) {
      return ActionResult.PASS;
    }

    BlockPos sown = hit.getBlockPos().offset(hit.getSide());

    server.getServer().execute(() -> {
      if (CropSeasons.isCrop(server.getBlockState(sown))) {
        PlantedCrops.get(server).remember(sown);
      }
    });

    return ActionResult.PASS;
  }
}
