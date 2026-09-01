package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.init.ModCompostItems;
import net.abakath.rpg4fools.server.CompostedChunks;
import net.abakath.rpg4fools.server.PendingCompost;
import net.abakath.rpg4fools.world.Compost;
import net.abakath.rpg4fools.world.CropSticks;
import net.abakath.rpg4fools.world.CropWalls;
import net.abakath.rpg4fools.world.WalledCropBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Everything the player does with compost, in one place.
 *
 * <p>Four interactions, and they are one handler because they are one loop: a catalyst labels a
 * composter, a full labelled composter hands over compost instead of bone meal, compost goes into
 * farmland, and a shovel scrapes it back out. Splitting them across four callbacks would mean four
 * chances for two of them to disagree about which one owns a click on a composter.
 *
 * <p>No mixin. Every one of these is a use on a block, and UseBlockCallback runs before the block's
 * own onUse, so the composter can be made to hand over something else without touching it.
 *
 * <p>Collecting is checked before labelling. A player holding a catalyst who clicks a composter that
 * is already full and already labelled meant to empty it; re-labelling it there would eat a catalyst
 * and change nothing.
 *
 * <p>Registered before {@link CropAutoReplant}, which harvests any ripe crop that is right clicked.
 * Without that order a field could never be composted once it ripened, because the click would be
 * taken as a harvest. This handler only claims a click when the player is holding compost or a
 * shovel,
 * so nothing else changes hands.
 */
public final class CompostHandling {
  /** Vanilla's full composter, the level at which it hands over what it made. */
  private static final int FULL = 8;

  /** How far compost reaches from where it was used. One means the 3x3 around it. */
  private static final int REACH = 1;

  private CompostHandling() {
  }

  public static void register() {
    UseBlockCallback.EVENT.register(CompostHandling::onUseBlock);
  }

  /**
   * Server only, like the auto replant next door and for the same reason: everything here grants an
   * interaction rather than refusing one, and the client has nothing it needs to predict.
   */
  private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    if (!(world instanceof ServerLevel serverWorld)) {
      return InteractionResult.PASS;
    }

    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);
    ItemStack stack = player.getItemInHand(hand);

    if (state.is(Blocks.COMPOSTER)) {
      return useComposter(serverWorld, player, pos, state, stack);
    }

    BlockPos soil = soilUnder(world, pos, state);

    if (soil != null) {
      return useSoil(serverWorld, player, soil, state, stack);
    }

    return InteractionResult.PASS;
  }

  /**
   * The bed that whatever was clicked is growing in, or null if it is not growing in one.
   *
   * <p>Clicking the plant has to work as well as clicking the soil. A player looking at a field sees
   * crops, not farmland, and telling them to aim at the one square inch of dirt still showing between
   * two stems is asking them to fight the game.
   *
   * <p>The two supported forms need asking rather than assuming. A cell of a wall plant sits over
   * whatever happens to be behind it, and a section of a sticked plant sits over another stick, so
   * for both the bed is under the root rather than under the block that was clicked.
   */
  private static BlockPos soilUnder(Level world, BlockPos pos, BlockState state) {
    if (state.is(Blocks.FARMLAND)) {
      return pos;
    }

    BlockPos below;

    if (state.getBlock() instanceof WalledCropBlock) {
      below = CropWalls.root(state, pos).below();
    } else if (CropSticks.isColumn(state)) {
      below = CropSticks.base(world, pos).below();
    } else {
      below = pos.below();
    }

    return world.getBlockState(below).is(Blocks.FARMLAND) ? below : null;
  }

  private static InteractionResult useComposter(ServerLevel world, Player player, BlockPos pos,
                                           BlockState state, ItemStack stack) {
    PendingCompost pending = PendingCompost.get(world);
    Compost labelled = pending.marked(world, pos);

    if (labelled != Compost.NONE && state.getValue(ComposterBlock.LEVEL) == FULL) {
      return collect(world, player, pos, state, pending, labelled);
    }

    Compost catalyst = ModCompostItems.catalyst(stack);

    if (catalyst == null) {
      return InteractionResult.PASS;
    }

    // One label per batch. A second catalyst would only overwrite the first, and eating it to do
    // nothing visible is worse than refusing the click.
    if (labelled != Compost.NONE) {
      return refuse(world, pos);
    }

    pending.mark(pos, catalyst);

    if (!player.getAbilities().instabuild) {
      stack.shrink(1);
    }

    world.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 1.0F);
    world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            6, 0.3, 0.1, 0.3, 0.0);

    return InteractionResult.SUCCESS;
  }

  /**
   * Hands over the compost the batch was labelled for, and empties the composter as vanilla would.
   *
   * <p>Written out rather than left to the composter, because the whole point is that what comes out
   * is not bone meal. Everything else about it - the level going to zero, the sound - is kept the
   * same so an emptied composter behaves the way the player already expects.
   */
  private static InteractionResult collect(ServerLevel world, Player player, BlockPos pos,
                                      BlockState state, PendingCompost pending, Compost kind) {
    pending.clear(pos);

    Block.popResource(world, pos, new ItemStack(ModCompostItems.compostFor(kind)));

    BlockState emptied = state.setValue(ComposterBlock.LEVEL, 0);
    world.setBlock(pos, emptied, Block.UPDATE_CLIENTS);
    world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, emptied));
    world.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

    return InteractionResult.SUCCESS;
  }

  private static InteractionResult useSoil(ServerLevel world, Player player, BlockPos soil,
                                      BlockState clicked, ItemStack stack) {
    if (stack.getItem() instanceof ShovelItem) {
      // A ripe crop is a harvest first. Scraping the bed underneath one would take the harvest away
      // from every tool in the game except the shovel, which is the wrong trade for a feature about
      // soil.
      if (clicked.getBlock() instanceof CropBlock crop && crop.isMaxAge(clicked)) {
        return InteractionResult.PASS;
      }

      return clear(world, player, soil, world.getBlockState(soil));
    }

    Compost kind = ModCompostItems.compost(stack);

    if (kind == null) {
      return InteractionResult.PASS;
    }

    return apply(world, player, soil, stack, kind);
  }

  /**
   * Works compost into every bare piece of farmland in reach.
   *
   * <p>Soil that already carries something is skipped rather than overwritten, so a player topping
   * up the edge of a treated plot does not quietly replace the middle of it. A click that finds
   * nothing bare at all is refused and costs nothing; a click that finds some is spent, because
   * refusing a partial application would leave a plot impossible to finish.
   */
  private static InteractionResult apply(ServerLevel world, Player player, BlockPos centre,
                                    ItemStack stack, Compost kind) {
    int treated = 0;

    for (int x = -REACH; x <= REACH; x++) {
      for (int z = -REACH; z <= REACH; z++) {
        BlockPos pos = centre.offset(x, 0, z);
        BlockState state = world.getBlockState(pos);

        if (!state.is(Blocks.FARMLAND) || Compost.composted(state)) {
          continue;
        }

        world.setBlock(pos, state.setValue(Compost.PROPERTY, kind), Block.UPDATE_CLIENTS);
        CompostedChunks.get(world).remember(new ChunkPos(pos));

        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                4, 0.3, 0.1, 0.3, 0.0);

        treated++;
      }
    }

    if (treated == 0) {
      return refuse(world, centre);
    }

    if (!player.getAbilities().instabuild) {
      stack.shrink(1);
    }

    world.playSound(null, centre, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 0.9F);

    return InteractionResult.SUCCESS;
  }

  /**
   * Scrapes compost back out of one block of soil, giving nothing back.
   *
   * <p>The only way to swap one compost for another, since applying over an existing one is refused.
   * A shovel on farmland does nothing in vanilla - the block is not in the map that turns dirt and
   * grass into a path - so this costs no interaction anybody was using, and a ripe crop is turned
   * away upstream so it costs no harvest either.
   *
   * <p>The shovel rather than the hoe, which is the tool the soil would suggest. Right clicking
   * farmland with a hoe is being kept free for something else, and scraping is what a shovel does
   * anyway.
   */
  private static InteractionResult clear(ServerLevel world, Player player, BlockPos pos, BlockState state) {
    if (!Compost.composted(state)) {
      return InteractionResult.PASS;
    }

    BlockState bare = state.setValue(Compost.PROPERTY, Compost.NONE);
    world.setBlock(pos, bare, Block.UPDATE_CLIENTS);
    world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, bare));
    world.playSound(null, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);

    return InteractionResult.SUCCESS;
  }

  /** A click that was understood and had nothing to do. Says so, and spends nothing. */
  private static InteractionResult refuse(ServerLevel world, BlockPos pos) {
    world.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);

    return InteractionResult.FAIL;
  }
}
