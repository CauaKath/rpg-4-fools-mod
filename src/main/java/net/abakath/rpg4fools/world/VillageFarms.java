package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The village farm a block stands in, if it stands in one at all.
 *
 * <p>This is what lets a season change read as villagers replanting rather than as a farm rotting.
 * A crop the season killed is put back as something that grows now, and the lanes it goes back in
 * are the lanes the farm was built with: the village piece still knows its own rotation, so the
 * template column a block sits in can be recovered long after the chunk was generated.
 *
 * <p>Nothing here decides whether a replant should happen. {@link CropTransition} owns that, the
 * same way it owns every other answer about what a season does to a crop.
 */
public final class VillageFarms {
  /**
   * How wide a lane is, in template columns.
   *
   * <p>Three is exact for the plains farms: two columns of crops with a water channel or a path on
   * every third one. The other biomes' farms are irregular patches rather than lanes, and there a
   * three wide strip is simply a tidy way to divide them.
   */
  public static final int LANE_WIDTH = 3;

  private VillageFarms() {
  }

  /** The village lane this position belongs to, or empty if it is not inside a village piece. */
  public static Optional<Lane> laneAt(ServerWorld world, BlockPos pos) {
    Map<Structure, StructureStart> starts = world.getChunk(pos).getStructureStarts();

    for (Map.Entry<Structure, StructureStart> entry : starts.entrySet()) {
      if (!isVillage(world, entry.getKey())) {
        continue;
      }

      StructureStart start = entry.getValue();

      if (!start.hasChildren() || !start.getBoundingBox().contains(pos)) {
        continue;
      }

      for (StructurePiece piece : start.getChildren()) {
        if (piece instanceof PoolStructurePiece pool && piece.getBoundingBox().contains(pos)) {
          return Optional.of(new Lane(pool.getPos(), laneOf(pos, pool.getPos(), pool.getRotation())));
        }
      }
    }

    return Optional.empty();
  }

  /**
   * Which lane of a piece a position stands in, counted in the template's own coordinates.
   *
   * <p>A village piece is rotated as it is placed, so world coordinates say nothing about which way
   * the lanes run. Undoing the rotation puts the block back where the template author left it,
   * where a lane is always a column of x. Jigsaw structures are never mirrored, so only the
   * rotation has to be undone.
   */
  public static int laneOf(BlockPos placed, BlockPos origin, BlockRotation rotation) {
    BlockPos local = StructureTemplate.transformAround(
            placed.subtract(origin), BlockMirror.NONE, opposite(rotation), BlockPos.ORIGIN);

    return Math.floorDiv(local.getX(), LANE_WIDTH);
  }

  /**
   * What a villager would sow right now.
   *
   * <p>Read from the crops tag rather than a list of its own, so a datapack that retags a crop is
   * obeyed and the mod's own crops need no special mention. Narrowed to CropBlock, which drops the
   * stems and the flower crops: neither is something a farm plot is replanted with.
   *
   * <p>Empty in winter, which is what leaves a village with the dead crops everyone else gets.
   */
  public static List<Block> inSeason(Season season) {
    List<Block> crops = new ArrayList<>();

    Optional<RegistryEntryList.Named<Block>> tagged = Registries.BLOCK.getEntryList(ModBlockTags.CROPS);

    if (tagged.isEmpty()) {
      return crops;
    }

    for (RegistryEntry<Block> entry : tagged.get()) {
      Block block = entry.value();
      BlockState state = block.getDefaultState();

      if (block instanceof CropBlock && CropSeasons.isInSeason(state, season)) {
        crops.add(block);
      }
    }

    return crops;
  }

  /**
   * The crop this lane is sown with this season.
   *
   * <p>Seeded from the piece, the lane and the season, so every block of a lane reaches the same
   * answer and reaching it twice changes nothing. That matters: the sweep settles what is loaded
   * and the random tick hook settles the rest, and the two must not disagree about a farm one of
   * them has already replanted.
   */
  public static Block sownIn(Lane lane, Season season, List<Block> pool) {
    BlockPos origin = lane.origin();
    Random random = Random.create(
            MathHelper.hashCode(origin.getX() + lane.index(), origin.getY() + season.ordinal(), origin.getZ()));

    return pool.get(random.nextInt(pool.size()));
  }

  /**
   * Structures are a dynamic registry, so the world's own manager is the only place to ask. The
   * village tag is what decides rather than the five village structure ids, so a datapack's own
   * village is treated as one too.
   */
  private static boolean isVillage(ServerWorld world, Structure structure) {
    Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

    return registry.getKey(structure)
            .map(key -> registry.entryOf(key).isIn(StructureTags.VILLAGE))
            .orElse(false);
  }

  private static BlockRotation opposite(BlockRotation rotation) {
    return switch (rotation) {
      case CLOCKWISE_90 -> BlockRotation.COUNTERCLOCKWISE_90;
      case COUNTERCLOCKWISE_90 -> BlockRotation.CLOCKWISE_90;
      default -> rotation;
    };
  }

  /** One lane of one village piece: enough to name it, and to sow it the same way twice. */
  public record Lane(BlockPos origin, int index) {
  }
}
