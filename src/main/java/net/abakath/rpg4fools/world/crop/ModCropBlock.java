package net.abakath.rpg4fools.world.crop;

import com.mojang.serialization.MapCodec;
import net.abakath.rpg4fools.init.ModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/**
 * A farmland crop of this mod's own.
 *
 * <p>Carries no field naming its seed. createCodec only admits a factory taking Settings, so a seed
 * passed to the constructor would have nowhere to come from when a saved chunk is read back. The
 * roster is asked instead, which also keeps one source of truth for what belongs to what.
 */
public class ModCropBlock extends CropBlock {
  public static final MapCodec<ModCropBlock> CODEC = simpleCodec(ModCropBlock::new);

  public ModCropBlock(Properties settings) {
    super(settings);
  }

  @Override
  public MapCodec<? extends CropBlock> codec() {
    return CODEC;
  }

  @Override
  protected ItemLike getBaseSeedId() {
    return ModItems.seedFor(this);
  }
}
