package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModItems;
import net.abakath.rpg4fools.server.PlantedCrops;
import net.abakath.rpg4fools.world.CropDefinition;
import net.abakath.rpg4fools.world.CropItems;
import net.abakath.rpg4fools.world.CropSticks;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
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
  private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    if (!(world instanceof ServerLevel serverWorld)) {
      return InteractionResult.PASS;
    }

    ItemStack stack = player.getItemInHand(hand);
    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);

    return stack.is(ModItems.CROP_STICK)
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
  private static InteractionResult stick(ServerLevel world, Player player, ItemStack stack,
                                    BlockPos pos, BlockState state) {
    // Anywhere on a column means the top of it. Which face was hit says nothing useful about intent
    // when the block is a post two pixels wide, and there is only one place another stick can go.
    if (CropSticks.isColumn(state)) {
      return stack(world, player, stack, pos);
    }

    CropDefinition definition = ModBlocks.definitionFor(state.getBlock());

    if (definition == null || !definition.sticked()) {
      return InteractionResult.PASS;
    }

    // Only the crop standing in the ground. The same roster entry answers for the sticked block
    // too, and a stick used on one of those means the player is stacking, not converting.
    if (!state.is(ModBlocks.blockFor(definition))) {
      return InteractionResult.PASS;
    }

    Block sticked = ModBlocks.stickedFor(definition);
    BlockState placed = sticked.defaultBlockState()
            .setValue(CropBlock.AGE, state.getValue(CropBlock.AGE))
            .setValue(CropSticks.CAPPED, CropSticks.capped(world, pos));

    if (!placed.canSurvive(world, pos)) {
      return InteractionResult.PASS;
    }

    replace(world, player, stack, pos, placed, SoundEvents.WOOD_PLACE);
    return InteractionResult.SUCCESS;
  }

  /**
   * Adds a stick to the top of the column the player clicked, if it has room for one.
   *
   * <p>Passes rather than refusing when the column is full. The block's own placement rules turn the
   * placement away from there, which is the same answer arrived at by the usual route.
   */
  private static InteractionResult stack(ServerLevel world, Player player, ItemStack stack, BlockPos pos) {
    BlockPos above = CropSticks.columnTop(world, pos).above();

    if (!world.getBlockState(above).isAir()) {
      return InteractionResult.PASS;
    }

    BlockState placed = CropSticks.stick(world, above, false);

    if (!placed.canSurvive(world, above)) {
      return InteractionResult.PASS;
    }

    replace(world, player, stack, above, placed, SoundEvents.WOOD_PLACE);
    return InteractionResult.SUCCESS;
  }

  /**
   * Sows a seed into an empty stick, at age zero, as sowing it into farmland would.
   *
   * <p>Refused where the stick already touches a plant. A crop on a trellis is one plant that climbs
   * its own sticks, and a seed sown directly above or below an existing one would graft a second
   * plant into the middle of it - two sets of roots, two ages, one column. The stick a player wants
   * filled is reached by letting the plant grow into it.
   */
  private static InteractionResult sow(ServerLevel world, Player player, ItemStack stack,
                                  BlockPos pos, BlockState state) {
    // Aiming a seed at a stick mostly means hitting the farmland behind it, so the ground a stick
    // stands in counts as the stick. Farmland with nothing on it is left to vanilla, which plants
    // the crop the ordinary way.
    BlockPos target = state.is(Blocks.FARMLAND) ? pos.above() : pos;

    if (!CropSticks.isEmpty(world.getBlockState(target))) {
      return InteractionResult.PASS;
    }

    if (CropSticks.isSticked(world.getBlockState(target.below()))
            || CropSticks.isSticked(world.getBlockState(target.above()))) {
      return InteractionResult.PASS;
    }

    Optional<BlockState> planted = CropItems.plantedBy(stack);

    if (planted.isEmpty()) {
      return InteractionResult.PASS;
    }

    CropDefinition definition = ModBlocks.definitionFor(planted.get().getBlock());

    if (definition == null || !definition.sticked()) {
      return InteractionResult.PASS;
    }

    replace(world, player, stack, target, ModBlocks.stickedFor(definition).defaultBlockState()
                    .setValue(CropSticks.CAPPED, world.getBlockState(target).getValue(CropSticks.CAPPED)),
            SoundEvents.CROP_PLANTED);
    return InteractionResult.SUCCESS;
  }

  private static void replace(ServerLevel world, Player player, ItemStack stack, BlockPos pos,
                              BlockState placed, SoundEvent sound) {
    world.setBlock(pos, placed, Block.UPDATE_ALL);

    // The plot is the player's now, however it started. Sticks are work put into it, and a field that
    // resowed itself over them would be taking that away. Recorded at the foot of the column, which
    // is the position the season rules ask about.
    PlantedCrops.get(world).remember(CropSticks.base(world, pos));

    world.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placed));
    world.playSound(null, pos, sound, SoundSource.BLOCKS,
            1.0F, 0.8F + world.random.nextFloat() * 0.4F);

    if (!player.getAbilities().instabuild) {
      stack.shrink(1);
    }
  }
}
