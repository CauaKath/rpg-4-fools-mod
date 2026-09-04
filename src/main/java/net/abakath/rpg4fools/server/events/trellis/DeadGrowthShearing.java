package net.abakath.rpg4fools.server.events.trellis;

import net.abakath.rpg4fools.world.trellis.CropSticks;
import net.abakath.rpg4fools.world.trellis.CropWalls;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shears, taken to what a season left on a trellis.
 *
 * <p>Dead growth on a stick or a panel used to be clearable only by knocking the trellis down and
 * putting it back up, or by waiting for something to be sown over it. Neither is a way to tidy a
 * garden. A snip takes the debris off and leaves the timber exactly where it stood.
 *
 * <p>Nothing drops. The debris is worth nothing wherever it sits - a dead crop in a field is broken
 * for no reward either - and this is about the trellis being reusable, not about being paid for the
 * loss.
 *
 * <p>One snip clears the whole plant's worth of debris rather than one block of it, because one
 * plant is what died. A column is at most three blocks so it is simply walked; a wall is not, and
 * what makes it harder is that a dead panel keeps {@link CropWalls#ARM} and {@link CropWalls#ROW} but
 * not the axis - that property belongs to the plant - so the box the debris came from cannot be
 * worked back out. Contiguous debris is cleared instead: clean panels and living cells stop it, so
 * an ordinary patch goes in one snip, and a run of vines that all died side by side goes together,
 * which is the answer a player clearing a fence wants anyway.
 */
public final class DeadGrowthShearing {
  /**
   * How many panels one snip may clear.
   *
   * <p>Only a wall can reach it - a column is capped at {@link CropSticks#MAX_HEIGHT}. A whole dead
   * fence is one contiguous patch and there is no upper bound on how long a player may have built it,
   * so this is what keeps a single click from writing thousands of blocks in one tick. Hitting it
   * leaves the rest standing, and the next snip carries on from there.
   */
  private static final int MAX_PANELS = 64;

  /** Held once rather than cloned per panel, which is what {@link Direction#values} does. */
  private static final Direction[] NEIGHBOURS = Direction.values();

  private DeadGrowthShearing() {
  }

  public static void register() {
    UseBlockCallback.EVENT.register(DeadGrowthShearing::onUseBlock);
  }

  private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    if (!(world instanceof ServerLevel serverWorld)) {
      return InteractionResult.PASS;
    }

    ItemStack stack = player.getItemInHand(hand);

    if (!stack.is(Items.SHEARS)) {
      return InteractionResult.PASS;
    }

    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);

    List<BlockPos> debris = debrisFrom(serverWorld, pos, state);

    if (debris.isEmpty()) {
      return InteractionResult.PASS;
    }

    // The trellis is being changed, so the same permission an ordinary placement needs applies.
    if (!player.mayBuild()) {
      return InteractionResult.PASS;
    }

    for (BlockPos at : debris) {
      clear(serverWorld, at);
    }

    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());

    serverWorld.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F,
            0.8F + serverWorld.getRandom().nextFloat() * 0.4F);
    serverWorld.gameEvent(player, GameEvent.SHEAR, pos);
    player.awardStat(Stats.ITEM_USED.get(Items.SHEARS));

    return InteractionResult.SUCCESS;
  }

  /**
   * Every block one snip at this position clears, or nothing if there is no debris to take.
   *
   * <p>Worked out before anything is written, so a snip that turns out to have nothing to do can
   * pass the click on untouched rather than having spent durability on it.
   */
  private static List<BlockPos> debrisFrom(ServerLevel world, BlockPos pos, BlockState state) {
    if (CropSticks.isEmpty(state) && state.getValue(CropSticks.DEAD)) {
      return column(world, pos);
    }

    if (CropWalls.isWall(state) && state.getValue(CropWalls.DEAD)) {
      return patch(world, pos);
    }

    return List.of();
  }

  /**
   * The dead sticks in this position's column, from the ground up.
   *
   * <p>Walked from the foot rather than from the click, so a snip aimed at the top of a column takes
   * the debris below it too. Living sections are left where they are: a plant growing up a column
   * whose upper sticks are still wearing last season's remains is clearing them itself.
   */
  private static List<BlockPos> column(ServerLevel world, BlockPos pos) {
    BlockPos base = CropSticks.base(world, pos);
    List<BlockPos> dead = new ArrayList<>();

    for (int above = 0; above < CropSticks.MAX_HEIGHT; above++) {
      BlockPos at = base.above(above);
      BlockState state = world.getBlockState(at);

      if (!CropSticks.isColumn(state)) {
        break;
      }

      if (CropSticks.isEmpty(state) && state.getValue(CropSticks.DEAD)) {
        dead.add(at);
      }
    }

    return dead;
  }

  /**
   * The dead panels reachable from this one without crossing anything else.
   *
   * <p>Breadth first from the click, so the cap - when a patch is big enough to hit it - takes the
   * debris nearest the player's aim first.
   *
   * <p>Vertical neighbours count as well as the four the wall joins along. A plant covers a box three
   * rows tall, so the debris it leaves is only one patch if up and down are part of what touching
   * means.
   */
  private static List<BlockPos> patch(ServerLevel world, BlockPos pos) {
    List<BlockPos> dead = new ArrayList<>();
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> queue = new ArrayDeque<>();

    seen.add(pos);
    queue.add(pos);

    while (!queue.isEmpty() && dead.size() < MAX_PANELS) {
      BlockPos at = queue.removeFirst();
      dead.add(at);

      for (Direction direction : NEIGHBOURS) {
        BlockPos neighbour = at.relative(direction);

        if (!seen.add(neighbour)) {
          continue;
        }

        BlockState state = world.getBlockState(neighbour);

        if (CropWalls.isWall(state) && state.getValue(CropWalls.DEAD)) {
          queue.add(neighbour);
        }
      }
    }

    return dead;
  }

  /**
   * Takes the debris off one block, whichever of the two it is.
   *
   * <p>A state change and not a break. The stick or panel never leaves the world, so there is no
   * loot to drop and nothing for the neighbours to react to beyond the sprite.
   */
  private static void clear(ServerLevel world, BlockPos pos) {
    BlockState state = world.getBlockState(pos);

    if (CropSticks.isEmpty(state)) {
      world.setBlock(pos, CropSticks.cleared(state), Block.UPDATE_ALL);
      return;
    }

    if (CropWalls.isWall(state)) {
      world.setBlock(pos, CropWalls.cleared(state), Block.UPDATE_ALL);
    }
  }
}
