package net.abakath.rpg4fools.server.events.trellis;

import net.abakath.rpg4fools.server.events.season.SeasonPlantingGate;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModItems;
import net.abakath.rpg4fools.world.crop.CropDefinition;
import net.abakath.rpg4fools.world.crop.CropItems;
import net.abakath.rpg4fools.world.trellis.CropWalls;
import net.abakath.rpg4fools.world.trellis.WallArm;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import java.util.Optional;

/**
 * The two placements vanilla has no way to express, for walls.
 *
 * <p>Putting a panel up is ordinary block placement and the block's own rules cover it, which for a
 * wall means no rules at all. What is left is the pair of interactions that put something into a
 * block already occupied, and which vanilla answers by placing the held block in the space in front
 * instead - a cucumber hanging in the air, or a panel floating off the end of a wall:
 *
 * <ul>
 *   <li>a panel used on a cucumber standing in the ground grows the wall around it, keeping how far
 *       along the plant is
 *   <li>a seed used on a bare panel sows it, and so does a seed used on the farmland underneath,
 *       which is what a click aimed at a two pixel post mostly hits
 * </ul>
 *
 * <p>Registered after {@link SeasonPlantingGate}, which is what keeps a seed out of a panel outside
 * its season: the gate refuses the interaction first and this is never reached.
 *
 * <p>Which way the plant will spread is settled here, once, from the wall the player is standing in
 * front of - see {@link CropWalls#axisAt}. The plant keeps that answer for life.
 */
public class CropWallHandling {
  public static void register() {
    UseBlockCallback.EVENT.register(CropWallHandling::onUseBlock);
  }

  /**
   * Runs on the server only, for the reason the stick handler does.
   *
   * <p>Passing on the client lets vanilla send the use packet, and the client's own prediction comes
   * to nothing either way: it would place the held block one space over, where the seed cannot stand
   * and the panel is simply not what was asked for.
   */
  private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    if (!(world instanceof ServerLevel serverWorld)) {
      return InteractionResult.PASS;
    }

    ItemStack stack = player.getItemInHand(hand);
    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);

    return stack.is(ModItems.CROP_WALL)
            ? wall(serverWorld, player, stack, pos, state)
            : sow(serverWorld, player, stack, pos, state);
  }

  /**
   * Builds the wall around a cucumber already growing in the ground, keeping how far along it is.
   *
   * <p>The plant is not disturbed by being given something to climb, exactly as it is not by being
   * given sticks. Sending it back to seed would make walling a plot something done before sowing or
   * not at all, and the appeal is deciding a plant is worth the wall once it is already in front of
   * you.
   *
   * <p>Only the crop standing in the ground. A panel used anywhere else - on more wall, on a cell of
   * a plant that is already spreading - is ordinary placement, and is passed to vanilla.
   */
  private static InteractionResult wall(ServerLevel world, Player player, ItemStack stack,
                                   BlockPos pos, BlockState state) {
    CropDefinition definition = ModBlocks.definitionFor(state.getBlock());

    if (definition == null || !definition.walled()) {
      return InteractionResult.PASS;
    }

    // The same roster entry answers for the walled block too, and a panel used on one of those means
    // the player is building, not converting.
    if (!state.is(ModBlocks.blockFor(definition))) {
      return InteractionResult.PASS;
    }

    BlockState planted = root(world, pos, definition, facing(player))
            .setValue(CropBlock.AGE, state.getValue(CropBlock.AGE));

    if (!planted.canSurvive(world, pos)) {
      return InteractionResult.PASS;
    }

    replace(world, player, stack, pos, planted, SoundEvents.WOOD_PLACE);
    return InteractionResult.SUCCESS;
  }

  /**
   * Sows a seed into a bare panel, at age zero, as sowing it into farmland would.
   *
   * <p>Refused where the panel has no farmland under it: the root is the one cell of a wall plant that
   * stands in soil, and a wall three storeys up is a wall, not a plot.
   *
   * <p>Refused, too, where the cell already touches a plant in the same plane. A crop on a wall is one
   * plant that spreads over its own panels, and a seed sown against an existing one would graft a
   * second set of roots into the middle of it. The cell a player wants covered is reached by letting
   * the plant spread into it.
   */
  private static InteractionResult sow(ServerLevel world, Player player, ItemStack stack,
                                  BlockPos pos, BlockState state) {
    // Aiming a seed at a panel mostly means hitting the farmland behind it, so the ground a panel
    // stands in counts as the panel. Farmland with nothing above it is left to vanilla, which plants
    // the crop the ordinary way.
    BlockPos target = state.is(Blocks.FARMLAND) ? pos.above() : pos;

    if (!CropWalls.isWall(world.getBlockState(target))) {
      return InteractionResult.PASS;
    }

    if (!world.getBlockState(target.below()).is(Blocks.FARMLAND)) {
      return InteractionResult.PASS;
    }

    Optional<BlockState> planted = CropItems.plantedBy(stack);

    if (planted.isEmpty()) {
      return InteractionResult.PASS;
    }

    CropDefinition definition = ModBlocks.definitionFor(planted.get().getBlock());

    if (definition == null || !definition.walled()) {
      return InteractionResult.PASS;
    }

    BlockState sown = root(world, target, definition, facing(player));

    if (touchesPlant(world, target, sown.getValue(CropWalls.AXIS))) {
      return InteractionResult.PASS;
    }

    replace(world, player, stack, target, sown, SoundEvents.CROP_PLANTED);
    return InteractionResult.SUCCESS;
  }

  /**
   * The root cell of a fresh plant: middle column, bottom row, on the axis the wall suggests.
   *
   * <p>Carries the panel's joins from the start. A cell works those out on neighbour update as well,
   * but an update reaches the blocks around a change and never the change itself - so without this a
   * newly sown cell would draw no arms at all until something else disturbed the wall.
   */
  private static BlockState root(ServerLevel world, BlockPos pos, CropDefinition definition, Direction.Axis fallback) {
    return CropWalls.joins(world, pos, ModBlocks.walledFor(definition).defaultBlockState()
            .setValue(CropWalls.AXIS, CropWalls.axisAt(world, pos, fallback))
            .setValue(CropWalls.ARM, WallArm.CENTER)
            .setValue(CropWalls.ROW, 0));
  }

  /**
   * Whether a plant is already growing against this cell, in the plane a new one would spread along.
   *
   * <p>Only that plane. A wall that turns a corner can have a plant on each arm without either being
   * able to reach the other, and refusing the second one would be refusing a placement that is not
   * actually crowded.
   */
  private static boolean touchesPlant(ServerLevel world, BlockPos pos, Direction.Axis axis) {
    Direction along = CropWalls.positive(axis);

    return CropWalls.isCrop(world.getBlockState(pos.above()))
            || CropWalls.isCrop(world.getBlockState(pos.below()))
            || CropWalls.isCrop(world.getBlockState(pos.relative(along)))
            || CropWalls.isCrop(world.getBlockState(pos.relative(along.getOpposite())));
  }

  /**
   * The axis of the wall the player is standing in front of.
   *
   * <p>Used only where the wall itself cannot say - a lone panel, a corner, a junction. Looking north
   * at a wall means the wall runs east to west, so the axis wanted is the one across the line of
   * sight rather than along it.
   */
  private static Direction.Axis facing(Player player) {
    return player.getDirection().getClockWise().getAxis();
  }

  private static void replace(ServerLevel world, Player player, ItemStack stack, BlockPos pos,
                              BlockState placed, SoundEvent sound) {
    world.setBlock(pos, placed, Block.UPDATE_ALL);

    world.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placed));
    world.playSound(null, pos, sound, SoundSource.BLOCKS,
            1.0F, 0.8F + world.getRandom().nextFloat() * 0.4F);

    if (!player.getAbilities().instabuild) {
      stack.shrink(1);
    }
  }
}
