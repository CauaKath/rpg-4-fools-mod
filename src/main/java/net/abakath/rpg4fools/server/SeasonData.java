package net.abakath.rpg4fools.server;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.enums.SubSeason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.Objects;

public class SeasonData extends PersistentState {
  private static final String KEY = "season";
  private static final SubSeason DEFAULT_SUB_SEASON = SubSeason.EARLY_SPRING;

  /**
   * Never null. A brand new world sits at time 0, where DayChangingHandler bails out before it can
   * assign a value, so the field has to carry a sane default of its own.
   */
  private SubSeason subSeason = DEFAULT_SUB_SEASON;

  private static final Type<SeasonData> type = new Type<>(
          SeasonData::new,
          SeasonData::createFromNbt,
          null
  );

  public static SeasonData createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    SeasonData seasonData = new SeasonData();

    int ordinal = nbt.getInt(KEY);
    SubSeason[] values = SubSeason.values();

    seasonData.subSeason = ordinal >= 0 && ordinal < values.length ? values[ordinal] : DEFAULT_SUB_SEASON;

    return seasonData;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
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
    this.markDirty();
  }

  public static SeasonData getServerState(MinecraftServer server) {
    PersistentStateManager persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();

    return persistentStateManager.getOrCreate(type, RPG4Fools.MOD_ID);
  }
}
