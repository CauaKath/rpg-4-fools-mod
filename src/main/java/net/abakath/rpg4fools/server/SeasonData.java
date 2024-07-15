package net.abakath.rpg4fools.server;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.enums.Season;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.Objects;

public class SeasonData extends PersistentState {
  public final String KEY = "season";
  public Season season;

  private static final Type<SeasonData> type = new Type<>(
          SeasonData::new,
          SeasonData::createFromNbt,
          null
  );

  public static SeasonData createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    SeasonData seasonData = new SeasonData();

    seasonData.season = Season.values()[nbt.getInt("season")];

    return seasonData;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    nbt.putInt(KEY, season.ordinal());
    return nbt;
  }

  public static SeasonData getServerState(MinecraftServer server) {
    PersistentStateManager persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();

    SeasonData seasonData = persistentStateManager.getOrCreate(type, RPG4Fools.MOD_ID);

    seasonData.markDirty();

    return seasonData;
  }
}
