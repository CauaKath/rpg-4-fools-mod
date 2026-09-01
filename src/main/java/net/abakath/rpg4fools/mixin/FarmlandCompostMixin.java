package net.abakath.rpg4fools.mixin;

import net.abakath.rpg4fools.world.Compost;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.state.StateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives vanilla farmland somewhere to carry compost.
 *
 * <p>A property on the vanilla block rather than three blocks of this mod's own. Planting, villager
 * farming, hydration and trampling all test soil with {@code isOf(Blocks.FARMLAND)}, and a new block
 * would have to buy every one of those back with a mixin of its own. This buys all of them with one.
 *
 * <p>Nothing needs to set the default. FarmlandBlock's constructor builds its default state from the
 * state manager's, which takes the first value of every property, and {@link Compost#NONE} is first.
 *
 * <p>Trampling costs nothing to handle either: farmland that is jumped on becomes dirt, and dirt has
 * no compost on it to worry about.
 */
@Mixin(FarmlandBlock.class)
public class FarmlandCompostMixin {
  @Inject(method = "appendProperties", at = @At("TAIL"))
  private void rpg4fools$addCompost(StateManager.Builder<Block, BlockState> builder, CallbackInfo info) {
    builder.add(Compost.PROPERTY);
  }
}
