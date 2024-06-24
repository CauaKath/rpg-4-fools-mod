package net.abakath.rpg4fools.utils;

import net.minecraft.nbt.NbtCompound;

public interface IEntityDataSaver {
  NbtCompound getPersistentData();
}