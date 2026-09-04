package net.abakath.rpg4fools.world.season;

import net.abakath.rpg4fools.server.events.crop.CropTagValidator;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.enums.SubSeason;
import net.abakath.rpg4fools.init.ModBlockTags;
import net.minecraft.world.level.block.state.BlockState;
import java.util.EnumSet;

/**
 * Answers which seasons a crop grows in.
 *
 * <p>This is only the lookup. Nothing here changes how a crop behaves; consumers decide what being
 * out of season costs.
 *
 * <p>A crop carrying no grows_in_* tag counts as growing in every season. The alternative, treating
 * an untagged crop as never in season, would silently break every modded crop this mod ships no
 * tags for. Being permissive means an untagged crop behaves exactly like vanilla, and
 * CropTagValidator logs the omission so it still gets noticed.
 *
 * <p>Deliberately uncached. BlockState.isIn is a set lookup against the block's registry entry,
 * which is already cheap, and a cache would have to be invalidated on every datapack reload for no
 * measurable gain.
 */
public class CropSeasons {
  /** Whether the mod considers this block a crop at all. */
  public static boolean isCrop(BlockState state) {
    return state.is(ModBlockTags.CROPS);
  }

  /**
   * Seasons this crop grows in. Never empty: an untagged crop reads as all four.
   *
   * <p>Allocates, so prefer isInSeason when a single season is all that is being asked.
   */
  public static EnumSet<Season> getSeasons(BlockState state) {
    EnumSet<Season> seasons = EnumSet.noneOf(Season.class);

    for (Season season : Season.values()) {
      if (state.is(ModBlockTags.forSeason(season))) {
        seasons.add(season);
      }
    }

    return seasons.isEmpty() ? EnumSet.allOf(Season.class) : seasons;
  }

  /**
   * Whether a winter leaves this crop standing rather than killing it.
   *
   * <p>Says nothing about growth. A crop that survives a winter is still out of season through it;
   * consumers decide what that costs, exactly as they do for the season tags.
   */
  public static boolean survivesWinter(BlockState state) {
    return state.is(ModBlockTags.SURVIVES_WINTER);
  }

  public static boolean isInSeason(BlockState state, Season season) {
    return state.is(ModBlockTags.forSeason(season)) || hasNoSeason(state);
  }

  /** Sub-seasons carry no tags of their own; they resolve to their parent season. */
  public static boolean isInSeason(BlockState state, SubSeason subSeason) {
    return isInSeason(state, subSeason.getSeason());
  }

  private static boolean hasNoSeason(BlockState state) {
    for (Season season : Season.values()) {
      if (state.is(ModBlockTags.forSeason(season))) {
        return false;
      }
    }

    return true;
  }
}
