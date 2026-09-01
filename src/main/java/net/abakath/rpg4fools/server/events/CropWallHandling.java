package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModItems;
import net.abakath.rpg4fools.world.CropDefinition;
import net.abakath.rpg4fools.world.CropItems;
import net.abakath.rpg4fools.world.CropWalls;
import net.abakath.rpg4fools.world.WallArm;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

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
  private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    if (!(world instanceof ServerWorld serverWorld)) {
      return ActionResult.PASS;
    }

    ItemStack stack = player.getStackInHand(hand);
    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);

    return stack.isOf(ModItems.CROP_WALL)
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
  private static ActionResult wall(ServerWorld world, PlayerEntity player, ItemStack stack,
                                   BlockPos pos, BlockState state) {
    CropDefinition definition = ModBlocks.definitionFor(state.getBlock());

    if (definition == null || !definition.walled()) {
      return ActionResult.PASS;
    }

    // The same roster entry answers for the walled block too, and a panel used on one of those means
    // the player is building, not converting.
    if (!state.isOf(ModBlocks.blockFor(definition))) {
      return ActionResult.PASS;
    }

    BlockState planted = root(world, pos, definition, facing(player))
            .with(CropBlock.AGE, state.get(CropBlock.AGE));

    if (!planted.canPlaceAt(world, pos)) {
      return ActionResult.PASS;
    }

    replace(world, player, stack, pos, planted, SoundEvents.BLOCK_WOOD_PLACE);
    return ActionResult.SUCCESS;
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
  private static ActionResult sow(ServerWorld world, PlayerEntity player, ItemStack stack,
                                  BlockPos pos, BlockState state) {
    // Aiming a seed at a panel mostly means hitting the farmland behind it, so the ground a panel
    // stands in counts as the panel. Farmland with nothing above it is left to vanilla, which plants
    // the crop the ordinary way.
    BlockPos target = state.isOf(Blocks.FARMLAND) ? pos.up() : pos;

    if (!CropWalls.isWall(world.getBlockState(target))) {
      return ActionResult.PASS;
    }

    if (!world.getBlockState(target.down()).isOf(Blocks.FARMLAND)) {
      return ActionResult.PASS;
    }

    Optional<BlockState> planted = CropItems.plantedBy(stack);

    if (planted.isEmpty()) {
      return ActionResult.PASS;
    }

    CropDefinition definition = ModBlocks.definitionFor(planted.get().getBlock());

    if (definition == null || !definition.walled()) {
      return ActionResult.PASS;
    }

    BlockState sown = root(world, target, definition, facing(player));

    if (touchesPlant(world, target, sown.get(CropWalls.AXIS))) {
      return ActionResult.PASS;
    }

    replace(world, player, stack, target, sown, SoundEvents.ITEM_CROP_PLANT);
    return ActionResult.SUCCESS;
  }

  /**
   * The root cell of a fresh plant: middle column, bottom row, on the axis the wall suggests.
   *
   * <p>Carries the panel's joins from the start. A cell works those out on neighbour update as well,
   * but an update reaches the blocks around a change and never the change itself - so without this a
   * newly sown cell would draw no arms at all until something else disturbed the wall.
   */
  private static BlockState root(ServerWorld world, BlockPos pos, CropDefinition definition, Direction.Axis fallback) {
    return CropWalls.joins(world, pos, ModBlocks.walledFor(definition).getDefaultState()
            .with(CropWalls.AXIS, CropWalls.axisAt(world, pos, fallback))
            .with(CropWalls.ARM, WallArm.CENTER)
            .with(CropWalls.ROW, 0));
  }

  /**
   * Whether a plant is already growing against this cell, in the plane a new one would spread along.
   *
   * <p>Only that plane. A wall that turns a corner can have a plant on each arm without either being
   * able to reach the other, and refusing the second one would be refusing a placement that is not
   * actually crowded.
   */
  private static boolean touchesPlant(ServerWorld world, BlockPos pos, Direction.Axis axis) {
    Direction along = CropWalls.positive(axis);

    return CropWalls.isCrop(world.getBlockState(pos.up()))
            || CropWalls.isCrop(world.getBlockState(pos.down()))
            || CropWalls.isCrop(world.getBlockState(pos.offset(along)))
            || CropWalls.isCrop(world.getBlockState(pos.offset(along.getOpposite())));
  }

  /**
   * The axis of the wall the player is standing in front of.
   *
   * <p>Used only where the wall itself cannot say - a lone panel, a corner, a junction. Looking north
   * at a wall means the wall runs east to west, so the axis wanted is the one across the line of
   * sight rather than along it.
   */
  private static Direction.Axis facing(PlayerEntity player) {
    return player.getHorizontalFacing().rotateYClockwise().getAxis();
  }

  private static void replace(ServerWorld world, PlayerEntity player, ItemStack stack, BlockPos pos,
                              BlockState placed, SoundEvent sound) {
    world.setBlockState(pos, placed, Block.NOTIFY_ALL);

    world.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(player, placed));
    world.playSound(null, pos, sound, SoundCategory.BLOCKS,
            1.0F, 0.8F + world.random.nextFloat() * 0.4F);

    if (!player.getAbilities().creativeMode) {
      stack.decrement(1);
    }
  }
}
