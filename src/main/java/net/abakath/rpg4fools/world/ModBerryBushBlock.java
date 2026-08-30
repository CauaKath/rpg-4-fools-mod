package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

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
  public static final MapCodec<SweetBerryBushBlock> CODEC = createCodec(ModBerryBushBlock::new);

  public ModBerryBushBlock(Settings settings) {
    super(settings);
  }

  @Override
  public MapCodec<SweetBerryBushBlock> getCodec() {
    return CODEC;
  }

  @Override
  public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
    return new ItemStack(ModItems.berryFor(this));
  }

  @Override
  public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
    if (ModBlocks.isThorny(this)) {
      super.onEntityCollision(state, world, pos, entity);
    }
  }

  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    int age = state.get(AGE);

    // Nothing worth picking yet. The parent handles bone meal and the empty click.
    if (age <= 1) {
      return super.onUse(state, world, pos, player, hit);
    }

    int picked = 1 + world.random.nextInt(2) + (age == MAX_AGE ? 1 : 0);
    dropStack(world, pos, new ItemStack(ModItems.berryFor(this), picked));
    world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS,
            1.0F, 0.8F + world.random.nextFloat() * 0.4F);

    // Back to leafy rather than bare, the way vanilla leaves a picked bush.
    BlockState picking = state.with(AGE, 1);
    world.setBlockState(pos, picking, Block.NOTIFY_LISTENERS);
    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, picking));

    return ActionResult.success(world.isClient);
  }
}
