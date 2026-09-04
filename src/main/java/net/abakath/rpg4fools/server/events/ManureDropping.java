package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.init.ModCompostItems;
import net.abakath.rpg4fools.init.ModEntityTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Manure, left behind by animals that graze.
 *
 * <p>The one catalyst that is waited for rather than made, which is what stops the compost pipeline
 * from being three trips to the same furnace. A pen that is kept is a pen that pays.
 *
 * <p>Deliberately slow. This is a passive drop on every animal a player owns, and the failure mode
 * of getting it wrong is a floor of item entities rather than anything subtle, so the roll is long
 * and the interval is long with it. Roughly one drop per animal every few minutes.
 *
 * <p>A sweep on an interval rather than a hook on the animal. Nothing in Fabric offers an entity
 * tick, and mixing into one would mean this mod's code running on every entity in the world on
 * every tick to do something that only has to happen twice a minute.
 */
public class ManureDropping implements ServerTickEvents.EndTick {
  /** Ticks between sweeps. Half a minute, which is far below the rate any single animal drops at. */
  private static final int INTERVAL = 600;

  /** One sweep in twelve for a given animal, so an animal averages a drop every six minutes. */
  private static final int CHANCE = 12;

  @Override
  public void onEndTick(MinecraftServer server) {
    if (server.getTickCount() % INTERVAL != 0) {
      return;
    }

    for (ServerLevel world : server.getAllLevels()) {
      sweep(world);
    }
  }

  private static void sweep(ServerLevel world) {
    for (Entity entity : world.getAllEntities()) {
      if (!(entity instanceof Animal animal)) {
        continue;
      }

      // Babies eat and do not graze. Waiting for an animal to grow up before it pays is also what
      // stops a pen of newborns from being a faster source than a pen of adults.
      if (animal.isBaby() || !animal.getType().builtInRegistryHolder().is(ModEntityTags.MANURE_PRODUCERS)) {
        continue;
      }

      if (world.getRandom().nextInt(CHANCE) != 0) {
        continue;
      }

      BlockPos pos = animal.blockPosition();

      Block.popResource(world, pos, new ItemStack(ModCompostItems.MANURE));
    }
  }
}
