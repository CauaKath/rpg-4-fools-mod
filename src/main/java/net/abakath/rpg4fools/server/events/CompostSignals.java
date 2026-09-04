package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.server.PendingCompost;
import net.abakath.rpg4fools.world.Compost;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * What a labelled composter looks like.
 *
 * <p>The label is the one thing about this feature the world cannot show on its own. A composter
 * with a catalyst in it is the same block at the same fill level as one without, so a player who
 * walked away mid batch has no way to remember which of their four composters is which - and finding
 * out costs them the batch, since collecting is what clears the label.
 *
 * <p>Coloured dust, in the colour of the compost the batch will produce. The three colours are the
 * ones already flecked through the compost sprites, so the composter and the item it is going to
 * hand over agree without the player being told they do.
 *
 * <p>A sweep on an interval rather than a block tick. A composter is a vanilla block with no ticking
 * of its own and no block entity to give it one, and the set of labelled ones is small enough that
 * walking it every second costs nothing.
 */
public class CompostSignals implements ServerTickEvents.EndTick {
  /** Ticks between puffs. Often enough to look continuous, rare enough to be free. */
  private static final int INTERVAL = 20;

  private static final Map<Compost, Vector3f> COLOURS = new EnumMap<>(Compost.class);

  static {
    COLOURS.put(Compost.RICH, colour(186, 162, 94));
    COLOURS.put(Compost.WARM, colour(200, 196, 186));
    COLOURS.put(Compost.CREEPING, colour(108, 140, 66));
  }

  @Override
  public void onEndTick(MinecraftServer server) {
    if (server.getTickCount() % INTERVAL != 0) {
      return;
    }

    for (ServerLevel world : server.getAllLevels()) {
      PendingCompost.get(world).forEach(world, (pos, kind) -> {
        Vector3f colour = COLOURS.get(kind);

        if (colour == null) {
          return;
        }

        world.sendParticles(new DustParticleOptions(colour, 1.0F),
                pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                2, 0.2, 0.05, 0.2, 0.0);
      });
    }
  }

  private static Vector3f colour(int red, int green, int blue) {
    return new Vector3f(red / 255.0F, green / 255.0F, blue / 255.0F);
  }
}
