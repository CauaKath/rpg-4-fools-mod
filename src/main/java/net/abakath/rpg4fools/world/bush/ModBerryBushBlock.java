package net.abakath.rpg4fools.world.bush;

import com.mojang.serialization.MapCodec;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A berry bush of this mod's own.
 *
 * <p>Extends the vanilla bush so growth, shape and sounds come free. Only the two things that name
 * sweet berries are replaced: what picking gives, and what pick block hands over.
 *
 * <p>Thorns are per plant. Vanilla hurts anything walking through a bush, which reads right for a
 * bramble and wrong for strawberries, so the roster decides and a bush without thorns drops the
 * collision behaviour entirely.
 */
public class ModBerryBushBlock extends SweetBerryBushBlock {
  public static final MapCodec<SweetBerryBushBlock> CODEC = simpleCodec(ModBerryBushBlock::new);

  public ModBerryBushBlock(Properties settings) {
    super(settings);
  }

  @Override
  public MapCodec<SweetBerryBushBlock> codec() {
    return CODEC;
  }

  @Override
  protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
    return new ItemStack(ModItems.berryFor(this));
  }

  @Override
  protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity,
                              InsideBlockEffectApplier effects, boolean flag) {
    if (ModBlocks.isThorny(this)) {
      super.entityInside(state, world, pos, entity, effects, flag);
    }
  }

  @Override
  public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    int age = state.getValue(AGE);

    // Nothing worth picking yet. The parent handles bone meal and the empty click.
    if (age <= 1) {
      return super.useWithoutItem(state, world, pos, player, hit);
    }

    int picked = 1 + world.getRandom().nextInt(2) + (age == MAX_AGE ? 1 : 0);
    popResource(world, pos, new ItemStack(ModItems.berryFor(this), picked));
    world.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
            1.0F, 0.8F + world.getRandom().nextFloat() * 0.4F);

    // Back to leafy rather than bare, the way vanilla leaves a picked bush.
    BlockState picking = state.setValue(AGE, 1);
    world.setBlock(pos, picking, Block.UPDATE_CLIENTS);
    world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picking));

    return InteractionResult.SUCCESS;
  }
}
