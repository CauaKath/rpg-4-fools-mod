package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

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
  public static final MapCodec<RegrowingCropBlock> CODEC = simpleCodec(RegrowingCropBlock::new);

  public RegrowingCropBlock(Properties settings) {
    super(settings);
  }

  @Override
  public MapCodec<? extends CropBlock> codec() {
    return CODEC;
  }

  @Override
  public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    // Nothing ripe to take. The parent handles bone meal and the empty click.
    if (!isMaxAge(state)) {
      return super.useWithoutItem(state, world, pos, player, hit);
    }

    if (!(world instanceof ServerLevel serverWorld)) {
      return InteractionResult.SUCCESS;
    }

    CropHarvest.drop(serverWorld, pos, pos.below(), new ItemStack(produce(), 1 + serverWorld.getRandom().nextInt(3)));

    BlockState picked = pick(state);
    serverWorld.setBlock(pos, picked, Block.UPDATE_CLIENTS);
    serverWorld.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
    serverWorld.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS,
            1.0F, 0.8F + serverWorld.getRandom().nextFloat() * 0.4F);

    return InteractionResult.SUCCESS;
  }

  /**
   * Built from the current state rather than the default one, so anything else the crop is carrying
   * survives being picked. withAge would not: it starts from the default state and would drop it.
   */
  private BlockState pick(BlockState state) {
    return state.setValue(getAgeProperty(), ModBlocks.definitionFor(this).regrowAge());
  }

  private Item produce() {
    return ModItems.produceItem(ModBlocks.definitionFor(this));
  }
}
