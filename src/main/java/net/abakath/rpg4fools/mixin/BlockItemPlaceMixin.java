package net.abakath.rpg4fools.mixin;

import net.abakath.rpg4fools.server.events.CropOwnership;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Claims a plot the moment a player places a crop or a trellis.
 *
 * <p>{@link CropOwnership} catches a planting by watching for the use that would cause one and
 * looking at the spot afterwards, which is what it had to do when nothing reported a placement. It
 * works, but only because the deferred look happens to land after the placement: the server runs a
 * deferred task inline rather than queueing it whenever it is not already inside one, and which of
 * those holds is not something this mod controls.
 *
 * <p>This is the same claim made where there is nothing to get wrong. place has already put the
 * block down when this runs, and the context names the position it went to, so no ordering is
 * assumed and no second lookup is needed.
 *
 * <p>Additive rather than a replacement. The callback still covers the two placements that are not
 * a plain block placement at all - a stick onto a crop already in the ground, and a seed into a
 * stick already standing - and claiming a plot twice costs nothing, since the record is a set.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {
  @Inject(method = "place", at = @At("RETURN"))
  private void rpg4fools$claimPlacement(BlockPlaceContext context,
                                        CallbackInfoReturnable<InteractionResult> cir) {
    if (!cir.getReturnValue().consumesAction()) {
      return;
    }

    if (context.getPlayer() == null || !(context.getLevel() instanceof ServerLevel world)) {
      return;
    }

    CropOwnership.claim(world, context.getClickedPos());
  }
}
