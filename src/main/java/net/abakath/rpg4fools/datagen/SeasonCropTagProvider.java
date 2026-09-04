package net.abakath.rpg4fools.datagen;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlockTags;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModCrops;
import net.abakath.rpg4fools.world.CropDefinition;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Generates the crop and per-season block tags.
 *
 * <p>One table drives both #rpg4fools:crops and the four grows_in_* tags, so a crop cannot end up
 * in one without the other. Adding a crop means one line here.
 *
 * <p>Assignments follow the real growing calendar loosely: spring and autumn take the root
 * vegetables, summer takes the gourds, and winter takes nothing. An empty winter tag is
 * intentional, not an oversight.
 *
 * <p>Run ./gradlew runDatagen after editing. Output lands in src/main/generated and is committed.
 */
public class SeasonCropTagProvider extends FabricTagProvider.BlockTagProvider {
  private static final Map<Block, Set<Season>> CROP_SEASONS = createCropSeasons();

  public SeasonCropTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void addTags(HolderLookup.Provider wrapperLookup) {
    FabricTagBuilder crops = getOrCreateTagBuilder(ModBlockTags.CROPS);

    // Built up front rather than on demand so a season nobody grows in still gets a tag file.
    // Without it the tag would be missing rather than empty, which reads as "not loaded" instead
    // of "nothing grows then".
    Map<Season, FabricTagBuilder> seasonBuilders = new EnumMap<>(Season.class);
    for (Season season : Season.values()) {
      seasonBuilders.put(season, getOrCreateTagBuilder(ModBlockTags.forSeason(season)));
    }

    CROP_SEASONS.forEach((block, seasons) -> {
      crops.add(block);
      seasons.forEach(season -> seasonBuilders.get(season).add(block));
    });
  }

  private static Map<Block, Set<Season>> createCropSeasons() {
    // Insertion-ordered so the generated files stay stable across runs and diffs stay readable.
    Map<Block, Set<Season>> seasons = new LinkedHashMap<>();

    put(seasons, Blocks.WHEAT, Season.SPRING, Season.SUMMER, Season.AUTUMN);
    put(seasons, Blocks.CARROTS, Season.SPRING, Season.AUTUMN);
    put(seasons, Blocks.POTATOES, Season.SPRING, Season.SUMMER, Season.AUTUMN);
    put(seasons, Blocks.BEETROOTS, Season.SPRING, Season.AUTUMN);
    put(seasons, Blocks.TORCHFLOWER_CROP, Season.SPRING, Season.SUMMER);
    put(seasons, Blocks.PITCHER_CROP, Season.SPRING, Season.SUMMER);
    put(seasons, Blocks.MELON_STEM, Season.SUMMER);
    put(seasons, Blocks.PUMPKIN_STEM, Season.SUMMER, Season.AUTUMN);
    put(seasons, Blocks.SWEET_BERRY_BUSH, Season.SUMMER, Season.AUTUMN);

    // An attached stem is the same plant past pollination, so it keeps its stem's seasons. It no
    // longer grows itself, but a consumer asking "is this in season" should get the same answer for
    // both halves of one melon patch.
    put(seasons, Blocks.ATTACHED_MELON_STEM, Season.SUMMER);
    put(seasons, Blocks.ATTACHED_PUMPKIN_STEM, Season.SUMMER, Season.AUTUMN);

    // The dormant bush keeps the live bush's seasons. The hook reads them to decide when to revive
    // it, and an untagged block reads as growing in every season, which would revive it in winter.
    put(seasons, ModBlocks.DORMANT_SWEET_BERRY_BUSH, Season.SUMMER, Season.AUTUMN);

    // The mod's own crops, from the same table that registers them. A crop cannot end up tagged
    // without being registered, or registered without a season.
    for (CropDefinition definition : ModCrops.ALL) {
      seasons.put(ModBlocks.blockFor(definition), definition.seasons());

      // The sticked form keeps the plain crop's seasons. It is the same plant with a trellis around
      // it, and the season hook has to keep looking at it: that hook is what returns the column to
      // bare sticks when summer ends. The empty stick itself is left untagged, having nothing to
      // grow and no season to be out of.
      if (definition.sticked()) {
        seasons.put(ModBlocks.stickedFor(definition), definition.seasons());
      }

      // The walled form, for the same reason: the hook is what strips a wall back to bare panels
      // when the season ends, and it only looks at what the crops tag names. The panel itself stays
      // untagged, having nothing to grow and no season to be out of.
      if (definition.walled()) {
        seasons.put(ModBlocks.walledFor(definition), definition.seasons());
      }

      if (definition.kind() == CropDefinition.Kind.BUSH) {
        seasons.put(ModBlocks.dormantFor(definition), definition.seasons());
      }
    }

    return seasons;
  }

  private static void put(Map<Block, Set<Season>> seasons, Block block, Season... grownIn) {
    seasons.put(block, EnumSet.copyOf(List.of(grownIn)));
  }
}
