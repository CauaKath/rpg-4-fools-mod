package net.abakath.rpg4fools.world;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * What has been worked into a patch of farmland.
 *
 * <p>A property on vanilla farmland rather than a block of this mod's own. Every check in the game
 * that asks whether soil is farmland asks it as {@code isOf(Blocks.FARMLAND)} - vanilla crop and
 * stem planting, villager farming, hydration, trampling - and a separate block would have failed
 * all of them at once. Composted soil is still farmland; it just has something in it.
 *
 * <p>{@link #NONE} is first so that plain farmland stays plain. FarmlandBlock builds its default
 * state from the state manager's default, which takes the first value of every property, so the
 * mixin that adds this one needs to do nothing else.
 *
 * <p>Three kinds, one axis each, and never more than one at a time: rich feeds the harvest, warm
 * hurries the growing, creeping hurries the spreading. Applying over an existing compost is
 * refused rather than allowed to overwrite, so the only way to swap is to scrape the soil with a
 * shovel
 * and start again - which is what makes the choice of kind a decision rather than a keystroke.
 */
public enum Compost implements StringIdentifiable {
  NONE("none"),
  RICH("rich"),
  WARM("warm"),
  CREEPING("creeping");

  /**
   * Added to vanilla farmland by {@link net.abakath.rpg4fools.mixin.FarmlandCompostMixin}.
   *
   * <p>Farmland goes from 8 states to 32. Cheap, and the alternative - a side table keyed by
   * position - could not be drawn, which is the one thing composted soil has to be.
   */
  public static final EnumProperty<Compost> PROPERTY = EnumProperty.of("compost", Compost.class);

  private final String name;

  Compost(String name) {
    this.name = name;
  }

  @Override
  public String asString() {
    return name;
  }

  /**
   * What this block is composted with, for any block at all.
   *
   * <p>Guarded on the property rather than on the block, so anything that is not farmland answers
   * NONE instead of throwing. Callers ask about whatever happens to be under a plant, and under a
   * plant is not always soil.
   */
  public static Compost on(BlockState state) {
    return state.contains(PROPERTY) ? state.get(PROPERTY) : NONE;
  }

  public static Compost at(BlockView world, BlockPos soil) {
    return on(world.getBlockState(soil));
  }

  /** Whether this state is farmland carrying something. What the expiry scan looks for. */
  public static boolean composted(BlockState state) {
    return on(state) != NONE;
  }
}
