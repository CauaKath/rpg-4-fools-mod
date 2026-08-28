package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.CropItems;
import net.abakath.rpg4fools.world.CropSeasons;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Refuses to plant a crop outside the seasons it grows in.
 *
 * <p>Registered on both sides rather than server only. The client predicts placement, so a
 * server-only refusal would show the crop appearing and then snapping away.
 */
public class SeasonPlantingGate {
  public static void register() {
    UseBlockCallback.EVENT.register(SeasonPlantingGate::onUseBlock);
  }

  private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    ItemStack stack = player.getStackInHand(hand);
    Season season = CurrentSeason.season();

    Optional<BlockState> planted = CropItems.plantedBy(stack);
    if (planted.isPresent()) {
      BlockState crop = planted.get();

      return CropSeasons.isInSeason(crop, season)
              ? ActionResult.PASS
              : refuse(player, world, crop, season, "message.rpg4fools.cannot_plant");
    }

    // Bone meal is the other way to make a crop grow, so leaving it alone would make out of season
    // wheat merely slower to force rather than impossible.
    if (stack.isOf(Items.BONE_MEAL)) {
      BlockState target = world.getBlockState(hit.getBlockPos());

      if (CropSeasons.isCrop(target) && !CropSeasons.isInSeason(target, season)) {
        return refuse(player, world, target, season, "message.rpg4fools.cannot_grow");
      }
    }

    return ActionResult.PASS;
  }

  /**
   * Cancels the interaction, and says why on the action bar.
   *
   * <p>The message is sent from the server side only. Both sides run this handler, and the client
   * copy would otherwise print the same line a second time.
   */
  private static ActionResult refuse(PlayerEntity player, World world, BlockState crop, Season season, String key) {
    if (!world.isClient()) {
      player.sendMessage(
              Text.translatable(
                      key,
                      crop.getBlock().getName(),
                      Text.literal(season.getName()).formatted(season.getColor())
              ),
              true
      );
    }

    return ActionResult.FAIL;
  }
}
