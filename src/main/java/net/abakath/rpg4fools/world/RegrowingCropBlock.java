package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * A farmland crop that fruits more than once.
 *
 * <p>Picking a ripe plant takes the produce and leaves the plant where it is, set back to the age
 * its roster entry names, so the fruit ripens again while the roots stay put. Sowing from seed is
 * the long wait; every harvest after the first is the short one.
 *
 * <p>Seeds are not part of a pick. The plant was not sacrificed to get the produce, so there is
 * nothing to hand back - a plant that paid out seeds on every pick would seed a farm off one
 * tomato. Breaking it still rolls the loot table, which is where seeds come from.
 *
 * <p>The age the plant returns to is drawn as the bare adult, so a picked plant reads as done
 * fruiting rather than half grown - the difference between a crop worth waiting on and a crop
 * worth tearing up. The two ages above it carry flowers, so the whole cycle is legible from the
 * sprite alone and nothing has to be tracked on the blockstate.
 */
public class RegrowingCropBlock extends ModCropBlock {
  public static final MapCodec<RegrowingCropBlock> CODEC = createCodec(RegrowingCropBlock::new);

  public RegrowingCropBlock(Settings settings) {
    super(settings);
  }

  @Override
  public MapCodec<? extends CropBlock> getCodec() {
    return CODEC;
  }

  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    // Nothing ripe to take. The parent handles bone meal and the empty click.
    if (!isMature(state)) {
      return super.onUse(state, world, pos, player, hit);
    }

    if (!(world instanceof ServerWorld serverWorld)) {
      return ActionResult.SUCCESS;
    }

    CropHarvest.drop(serverWorld, pos, pos.down(), new ItemStack(produce(), 1 + serverWorld.random.nextInt(3)));

    BlockState picked = pick(state);
    serverWorld.setBlockState(pos, picked, Block.NOTIFY_LISTENERS);
    serverWorld.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, picked));
    serverWorld.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS,
            1.0F, 0.8F + serverWorld.random.nextFloat() * 0.4F);

    return ActionResult.SUCCESS;
  }

  /**
   * Built from the current state rather than the default one, so anything else the crop is carrying
   * survives being picked. withAge would not: it starts from the default state and would drop it.
   */
  private BlockState pick(BlockState state) {
    return state.with(getAgeProperty(), ModBlocks.definitionFor(this).regrowAge());
  }

  private Item produce() {
    return ModItems.produceItem(ModBlocks.definitionFor(this));
  }
}
