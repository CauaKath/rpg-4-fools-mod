package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.abakath.rpg4fools.init.ModItems;
import net.minecraft.block.CropBlock;
import net.minecraft.item.ItemConvertible;

/**
 * A farmland crop of this mod's own.
 *
 * <p>Carries no field naming its seed. createCodec only admits a factory taking Settings, so a seed
 * passed to the constructor would have nowhere to come from when a saved chunk is read back. The
 * roster is asked instead, which also keeps one source of truth for what belongs to what.
 */
public class ModCropBlock extends CropBlock {
  public static final MapCodec<ModCropBlock> CODEC = createCodec(ModCropBlock::new);

  public ModCropBlock(Settings settings) {
    super(settings);
  }

  @Override
  public MapCodec<? extends CropBlock> getCodec() {
    return CODEC;
  }

  @Override
  protected ItemConvertible getSeedsItem() {
    return ModItems.seedFor(this);
  }
}
