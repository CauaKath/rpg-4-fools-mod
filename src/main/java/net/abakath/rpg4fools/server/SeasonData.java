package net.abakath.rpg4fools.server;

import net.abakath.rpg4fools.RPG4Fools;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.abakath.rpg4fools.enums.SubSeason;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import java.util.Objects;

public class SeasonData extends SavedData {
  private static final String KEY = "season";
  private static final SubSeason DEFAULT_SUB_SEASON = SubSeason.EARLY_SPRING;

  /**
   * Never null. A brand new world sits at time 0, where DayChangingHandler bails out before it can
   * assign a value, so the field has to carry a sane default of its own.
   */
  private SubSeason subSeason = DEFAULT_SUB_SEASON;

  /**
   * Stored as an ordinal rather than a name, as it was under the NBT serialisation this replaces.
   * An out of range value decays to the default instead of failing the whole read, so a save
   * written by a build with a different SubSeason list still loads.
   */
  private static final Codec<SeasonData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.INT.optionalFieldOf(KEY, DEFAULT_SUB_SEASON.ordinal()).forGetter(data -> data.subSeason.ordinal())
  ).apply(instance, SeasonData::fromOrdinal));

  private static final SavedDataType<SeasonData> TYPE = new SavedDataType<>(
          Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, "season"),
          SeasonData::new,
          CODEC,
          // Not null. SavedDataStorage calls update on this without checking, so a null one throws
          // the moment a saved file actually exists - which is every load after the first save.
          // No vanilla type describes a mod's own data, so this is the one with the least to do:
          // its schema is a bare long array, and the fixers only run at all when the stored data
          // version is older than the current one.
          DataFixTypes.SAVED_DATA_FORCED_CHUNKS
  );

  private static SeasonData fromOrdinal(int ordinal) {
    SeasonData seasonData = new SeasonData();
    SubSeason[] values = SubSeason.values();

    seasonData.subSeason = ordinal >= 0 && ordinal < values.length ? values[ordinal] : DEFAULT_SUB_SEASON;

    return seasonData;
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
    SavedDataStorage storage = Objects.requireNonNull(server.getLevel(Level.OVERWORLD)).getDataStorage();

    return storage.computeIfAbsent(TYPE);
  }
}
