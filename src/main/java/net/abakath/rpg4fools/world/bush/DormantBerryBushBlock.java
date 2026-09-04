package net.abakath.rpg4fools.world.bush;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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
  public static final MapCodec<SweetBerryBushBlock> CODEC = simpleCodec(DormantBerryBushBlock::new);

  public DormantBerryBushBlock(Properties settings) {
    super(settings);
  }

  @Override
  public MapCodec<SweetBerryBushBlock> codec() {
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
  public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
  }

  @Override
  public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
    return false;
  }

  @Override
  public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
    return false;
  }

  @Override
  public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
  }

  /** Nothing to pick. The live bush would hand over berries and reset the age. */
  @Override
  public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    return InteractionResult.PASS;
  }
}
