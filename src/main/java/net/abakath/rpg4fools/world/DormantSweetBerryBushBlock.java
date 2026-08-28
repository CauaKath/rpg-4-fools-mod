package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * A sweet berry bush waiting out a season it cannot fruit in.
 *
 * <p>Unlike every other crop a bush does not die, so this is a state it comes back from rather than
 * an ending. The season hook swaps the two blocks in both directions.
 *
 * <p>Extends the vanilla bush to keep its shape, its slowdown and its thorns: a dormant bush is
 * still a thicket to walk through. Only the parts that amount to producing berries are turned off.
 */
public class DormantSweetBerryBushBlock extends SweetBerryBushBlock {
  /**
   * Typed to the parent block, because that is what SweetBerryBushBlock.getCodec returns and the
   * return type is invariant. The factory still builds dormant bushes; only the declared type is
   * the parent's.
   */
  public static final MapCodec<SweetBerryBushBlock> CODEC = createCodec(DormantSweetBerryBushBlock::new);

  public DormantSweetBerryBushBlock(Settings settings) {
    super(settings);
  }

  @Override
  public MapCodec<SweetBerryBushBlock> getCodec() {
    return CODEC;
  }

  /**
   * Never grows on its own.
   *
   * <p>The block still random ticks, and deliberately so: the season hook runs at the head of the
   * same tick and is the only thing that can bring the bush back. A block that stopped ticking
   * would stay dormant forever.
   */
  @Override
  public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
  }

  @Override
  public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
    return false;
  }

  @Override
  public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
    return false;
  }

  @Override
  public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
  }

  /** Nothing to pick. Vanilla would hand over berries and reset the age. */
  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    return ActionResult.PASS;
  }
}
