package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModItems;
import net.abakath.rpg4fools.world.CropDefinition;
import net.abakath.rpg4fools.world.CropItems;
import net.abakath.rpg4fools.world.CropSticks;
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
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

/**
 * The two placements vanilla has no way to express.
 *
 * <p>Placing a stick on bare farmland is ordinary block placement and the block's own rules cover it.
 * Everything else about a column goes through here, because a post two pixels wide is not something
 * anyone should have to aim at:
 *
 * <ul>
 *   <li>a stick used anywhere on a column goes on top of that column, the way scaffolding stacks,
 *       rather than needing the player to sneak and hit the top face
 *   <li>a stick used on a crop standing in the ground puts a trellis around it
 *   <li>a seed used on an empty stick sows it, and so does a seed used on the farmland underneath,
 *       which is what most clicks at a thin post actually hit
 * </ul>
 *
 * <p>The last three all put something into a block that is already occupied, which vanilla answers by
 * placing the held block in the space above - a crop hanging in the air, or a stick balanced on a
 * plant.
 *
 * <p>Registered after {@link SeasonPlantingGate}, which is what keeps a seed from being sown into a
 * stick out of season: the gate refuses the interaction first and this is never reached.
 *
 * <p>Ownership is not recorded for a crop sown here, unlike one planted in the ground.
 * {@link net.abakath.rpg4fools.world.CropTransition} sends a sticked crop back to a bare stick when
 * its season ends and never reaches the rule that resows an untended field, so there is nothing for
 * ownership to decide.
 */
public class CropStickHandling {
  public static void register() {
    UseBlockCallback.EVENT.register(CropStickHandling::onUseBlock);
  }

  /**
   * Runs on the server only, for the reason the replanting hook does.
   *
   * <p>Passing on the client lets vanilla send the use packet, and the client's own prediction comes
   * to nothing either way: it would place the held block one space up, and both blocks refuse to
   * stand there.
   */
  private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    if (!(world instanceof ServerWorld serverWorld)) {
      return ActionResult.PASS;
    }

    ItemStack stack = player.getStackInHand(hand);
    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);

    return stack.isOf(ModItems.CROP_STICK)
            ? stick(serverWorld, player, stack, pos, state)
            : sow(serverWorld, player, stack, pos, state);
  }

  /**
   * Puts a stick around a crop already growing in the ground, keeping how far along it is.
   *
   * <p>The plant is not disturbed by being given a trellis. Sending it back to seed would make
   * sticking a field something done before sowing or not at all, and the whole appeal is deciding a
   * plant is worth the sticks once it is already in front of you.
   */
  private static ActionResult stick(ServerWorld world, PlayerEntity player, ItemStack stack,
                                    BlockPos pos, BlockState state) {
    // Anywhere on a column means the top of it. Which face was hit says nothing useful about intent
    // when the block is a post two pixels wide, and there is only one place another stick can go.
    if (CropSticks.isColumn(state)) {
      return stack(world, player, stack, pos);
    }

    CropDefinition definition = ModBlocks.definitionFor(state.getBlock());

    if (definition == null || !definition.sticked()) {
      return ActionResult.PASS;
    }

    // Only the crop standing in the ground. The same roster entry answers for the sticked block
    // too, and a stick used on one of those means the player is stacking, not converting.
    if (!state.isOf(ModBlocks.blockFor(definition))) {
      return ActionResult.PASS;
    }

    Block sticked = ModBlocks.stickedFor(definition);
    BlockState placed = sticked.getDefaultState()
            .with(CropBlock.AGE, state.get(CropBlock.AGE))
            .with(CropSticks.CAPPED, CropSticks.capped(world, pos));

    if (!placed.canPlaceAt(world, pos)) {
      return ActionResult.PASS;
    }

    replace(world, player, stack, pos, placed, SoundEvents.BLOCK_WOOD_PLACE);
    return ActionResult.SUCCESS;
  }

  /**
   * Adds a stick to the top of the column the player clicked, if it has room for one.
   *
   * <p>Passes rather than refusing when the column is full. The block's own placement rules turn the
   * placement away from there, which is the same answer arrived at by the usual route.
   */
  private static ActionResult stack(ServerWorld world, PlayerEntity player, ItemStack stack, BlockPos pos) {
    BlockPos above = CropSticks.columnTop(world, pos).up();

    if (!world.getBlockState(above).isAir()) {
      return ActionResult.PASS;
    }

    BlockState placed = ModBlocks.CROP_STICK.getDefaultState()
            .with(CropSticks.CAPPED, CropSticks.capped(world, above));

    if (!placed.canPlaceAt(world, above)) {
      return ActionResult.PASS;
    }

    replace(world, player, stack, above, placed, SoundEvents.BLOCK_WOOD_PLACE);
    return ActionResult.SUCCESS;
  }

  /**
   * Sows a seed into an empty stick, at age zero, as sowing it into farmland would.
   *
   * <p>Refused where the stick already touches a plant. A crop on a trellis is one plant that climbs
   * its own sticks, and a seed sown directly above or below an existing one would graft a second
   * plant into the middle of it - two sets of roots, two ages, one column. The stick a player wants
   * filled is reached by letting the plant grow into it.
   */
  private static ActionResult sow(ServerWorld world, PlayerEntity player, ItemStack stack,
                                  BlockPos pos, BlockState state) {
    // Aiming a seed at a stick mostly means hitting the farmland behind it, so the ground a stick
    // stands in counts as the stick. Farmland with nothing on it is left to vanilla, which plants
    // the crop the ordinary way.
    BlockPos target = state.isOf(Blocks.FARMLAND) ? pos.up() : pos;

    if (!CropSticks.isEmpty(world.getBlockState(target))) {
      return ActionResult.PASS;
    }

    if (CropSticks.isSticked(world.getBlockState(target.down()))
            || CropSticks.isSticked(world.getBlockState(target.up()))) {
      return ActionResult.PASS;
    }

    Optional<BlockState> planted = CropItems.plantedBy(stack);

    if (planted.isEmpty()) {
      return ActionResult.PASS;
    }

    CropDefinition definition = ModBlocks.definitionFor(planted.get().getBlock());

    if (definition == null || !definition.sticked()) {
      return ActionResult.PASS;
    }

    replace(world, player, stack, target, ModBlocks.stickedFor(definition).getDefaultState()
                    .with(CropSticks.CAPPED, world.getBlockState(target).get(CropSticks.CAPPED)),
            SoundEvents.ITEM_CROP_PLANT);
    return ActionResult.SUCCESS;
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
