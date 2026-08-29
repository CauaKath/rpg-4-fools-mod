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
 * A berry bush waiting out a season it cannot fruit in.
 *
 * <p>Unlike every other crop a bush does not die, so this is a state it comes back from rather than
 * an ending. {@link DormantBushes} pairs each live bush with its dormant form and the season hook
 * swaps the two in both directions.
 *
 * <p>Extends the mod's bush to keep its shape, its slowdown and whether it has thorns: a dormant
 * bramble is still a bramble to walk through. Only the parts that amount to producing berries are
 * turned off.
 */
public class DormantBerryBushBlock extends ModBerryBushBlock {
  /**
   * Typed to the vanilla bush, because that is what SweetBerryBushBlock.getCodec returns and the
   * return type is invariant. The factory still builds dormant bushes.
   */
  public static final MapCodec<SweetBerryBushBlock> CODEC = createCodec(DormantBerryBushBlock::new);

  public DormantBerryBushBlock(Settings settings) {
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
   * same tick and is the only thing that can bring the bush back. A block that stopped ticking would
   * stay dormant forever.
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

  /** Nothing to pick. The live bush would hand over berries and reset the age. */
  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    return ActionResult.PASS;
  }
}
