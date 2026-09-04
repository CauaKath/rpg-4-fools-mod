package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.CropItems;
import net.abakath.rpg4fools.world.CropSeasons;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

  private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    ItemStack stack = player.getItemInHand(hand);
    Season season = CurrentSeason.season();

    Optional<BlockState> planted = CropItems.plantedBy(stack);
    if (planted.isPresent()) {
      BlockState crop = planted.get();

      return CropSeasons.isInSeason(crop, season)
              ? InteractionResult.PASS
              : refuse(player, world, crop, season, "message.rpg4fools.cannot_plant");
    }

    // Bone meal is the other way to make a crop grow, so leaving it alone would make out of season
    // wheat merely slower to force rather than impossible.
    if (stack.is(Items.BONE_MEAL)) {
      BlockState target = world.getBlockState(hit.getBlockPos());

      if (CropSeasons.isCrop(target) && !CropSeasons.isInSeason(target, season)) {
        return refuse(player, world, target, season, "message.rpg4fools.cannot_grow");
      }
    }

    return InteractionResult.PASS;
  }

  /**
   * Cancels the interaction, and says why on the action bar.
   *
   * <p>Sent from whichever side is running, which in practice means the client. Fabric only
   * forwards a use to the server when the callback returns SUCCESS, so a FAIL from the client
   * copy of this handler ends the interaction there and the server copy is never invoked. Guarding
   * the message on the server side left it unreachable: the block was refused and nothing was said.
   *
   * <p>That also means it cannot print twice. Whichever side refuses first is the only side that
   * gets to run.
   */
  private static InteractionResult refuse(Player player, Level world, BlockState crop, Season season, String key) {
    player.sendOverlayMessage(
            Component.translatable(
                    key,
                    crop.getBlock().getName(),
                    Component.literal(season.getName()).withStyle(season.getColor())
            )
    );

    return InteractionResult.FAIL;
  }
}
