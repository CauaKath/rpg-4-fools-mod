package net.abakath.rpg4fools.mixin;

import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

  @Inject(method = "saveWithoutId", at = @At("HEAD"))
  protected void injectWriteMethod(CompoundTag nbt, CallbackInfoReturnable info) {
    if(persistentData != null) {
      nbt.put("rpg4fools.abakath_data", persistentData);
    }
  }

  @Inject(method = "load", at = @At("HEAD"))
  protected void injectReadMethod(CompoundTag nbt, CallbackInfo info) {
    if (nbt.contains("rpg4fools.abakath_data", 10)) {
      persistentData = nbt.getCompound("rpg4fools.abakath_data");
    }
  }
}
