package net.abakath.rpg4fools.server;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.enums.SubSeason;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import java.util.Objects;

public class SeasonData extends SavedData {
  private static final String KEY = "season";
  private static final SubSeason DEFAULT_SUB_SEASON = SubSeason.EARLY_SPRING;

  /**
   * Never null. A brand new world sits at time 0, where DayChangingHandler bails out before it can
   * assign a value, so the field has to carry a sane default of its own.
   */
  private SubSeason subSeason = DEFAULT_SUB_SEASON;

  private static final Factory<SeasonData> type = new Factory<>(
          SeasonData::new,
          SeasonData::createFromNbt,
          null
  );

  public static SeasonData createFromNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
    SeasonData seasonData = new SeasonData();

    int ordinal = nbt.getInt(KEY);
    SubSeason[] values = SubSeason.values();

    seasonData.subSeason = ordinal >= 0 && ordinal < values.length ? values[ordinal] : DEFAULT_SUB_SEASON;

    return seasonData;
  }

  @Override
  public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registryLookup) {
    nbt.putInt(KEY, subSeason.ordinal());
    return nbt;
  }

  public SubSeason getSubSeason() {
    return subSeason;
  }

  /**
   * Only marks the state dirty when the value actually changes, so the world save is not forced to
   * rewrite this state on every tick.
   */
  public void setSubSeason(SubSeason subSeason) {
    if (subSeason == null || subSeason == this.subSeason) {
      return;
    }

    this.subSeason = subSeason;
    this.setDirty();
  }

  public static SeasonData getServerState(MinecraftServer server) {
    DimensionDataStorage persistentStateManager = Objects.requireNonNull(server.getLevel(Level.OVERWORLD)).getDataStorage();

    return persistentStateManager.computeIfAbsent(type, RPG4Fools.MOD_ID);
  }
}
