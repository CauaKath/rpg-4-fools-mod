package net.abakath.rpg4fools.mixin;

import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ModEntityDataSaverMixin implements IEntityDataSaver {
  @Unique
  private CompoundTag persistentData;

  @Override
  public CompoundTag getPersistentData() {
    if(this.persistentData == null) {
      this.persistentData = new CompoundTag();
    }
    return persistentData;
  }

  // Entity data is written through ValueOutput rather than a raw CompoundTag now, so the blob goes
  // in under a codec instead of being put directly.
  @Inject(method = "saveWithoutId", at = @At("HEAD"))
  protected void injectWriteMethod(ValueOutput output, CallbackInfo info) {
    if(persistentData != null) {
      output.store("rpg4fools.abakath_data", CompoundTag.CODEC, persistentData);
    }
  }

  @Inject(method = "load", at = @At("HEAD"))
  protected void injectReadMethod(ValueInput input, CallbackInfo info) {
    input.read("rpg4fools.abakath_data", CompoundTag.CODEC).ifPresent(data -> persistentData = data);
  }
}
