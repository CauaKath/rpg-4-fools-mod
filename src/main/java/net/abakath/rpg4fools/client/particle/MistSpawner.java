package net.abakath.rpg4fools.client.particle;

import net.abakath.rpg4fools.client.ResolvedAtmosphere;
import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.abakath.rpg4fools.init.ModParticles;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Fills the air around the player with mist.
 *
 * <p>Keeps a target number of puffs alive rather than spawning at a fixed rate, so the density
 * reads the same whether the player is standing still or sprinting. The target follows the
 * atmosphere at the camera, which is what makes walking from plains into a swamp thicken the air.
 */
@Environment(EnvType.CLIENT)
public final class MistSpawner {
  /** Puffs alive at full density. The real cost here is overdraw, so this is the number to tune. */
  private static final int MAX_PARTICLES = 220;

  /**
   * Average puff lifetime in ticks, matching MistParticle. Population settles at spawn rate times
   * lifetime, so this is what turns a target population into a per tick rate.
   */
  private static final int AVERAGE_LIFETIME = 100;

  /** Ceiling on spawns per tick, so a sudden density jump eases in instead of popping. */
  private static final int MAX_SPAWNS_PER_TICK = 8;

  /** Horizontal reach of the spawn volume, in blocks. */
  private static final int SPAWN_RADIUS = 16;

  /** Vertical reach of the spawn volume, in blocks. */
  private static final int SPAWN_HEIGHT = 8;

  /** Below this density nothing spawns at all, so clear biomes cost nothing. */
  private static final float MIN_DENSITY = 0.02f;

  private static final float BASE_ALPHA = 0.10f;
  private static final float BASE_SCALE = 3.0f;

  private MistSpawner() {
  }

  public static void register() {
    ClientTickEvents.END_CLIENT_TICK.register(MistSpawner::onClientTick);
  }

  private static void onClientTick(MinecraftClient client) {
    ClientWorld world = client.world;
    PlayerEntity player = client.player;

    if (world == null || player == null || client.isPaused()) {
      return;
    }

    if (!world.getRegistryKey().equals(World.OVERWORLD)) {
      return;
    }

    float quality = particleQuality(client);
    if (quality <= 0.0f) {
      return;
    }

    BlockPos cameraPos = player.getBlockPos();
    ResolvedAtmosphere atmosphere = SeasonAtmosphere.resolve(world, cameraPos);

    float density = atmosphere.mistDensity();
    if (density < MIN_DENSITY) {
      return;
    }

    // Population settles at rate times lifetime, so aiming for a target population is just a
    // division. No need to track individual puffs, and it self corrects when some expire early
    // after the player teleports or the density drops.
    float target = MAX_PARTICLES * density * quality;
    float ratePerTick = target / AVERAGE_LIFETIME;

    int spawns = Math.min(MAX_SPAWNS_PER_TICK, (int) ratePerTick);

    // Spawn the fractional remainder probabilistically, so a rate below one per tick still works.
    if (world.getRandom().nextFloat() < ratePerTick - (int) ratePerTick) {
      spawns++;
    }

    for (int i = 0; i < spawns; i++) {
      spawnPuff(client, world, cameraPos, atmosphere, density);
    }
  }

  private static void spawnPuff(MinecraftClient client,
                                ClientWorld world,
                                BlockPos cameraPos,
                                ResolvedAtmosphere atmosphere,
                                float density) {
    Random random = world.getRandom();

    double x = cameraPos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0 * SPAWN_RADIUS;
    double y = cameraPos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0 * SPAWN_HEIGHT;
    double z = cameraPos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0 * SPAWN_RADIUS;

    BlockPos spawnPos = BlockPos.ofFloored(x, y, z);

    if (!world.getBlockState(spawnPos).isAir()) {
      return;
    }

    double driftX = (random.nextDouble() - 0.5) * 0.006;
    double driftY = (random.nextDouble() - 0.5) * 0.002;
    double driftZ = (random.nextDouble() - 0.5) * 0.006;

    Particle particle = client.particleManager.addParticle(
            ModParticles.MIST, x, y, z, driftX, driftY, driftZ
    );

    if (!(particle instanceof MistParticle mist)) {
      return;
    }

    mist.setMistColor(atmosphere.tintColor());
    mist.setPeakAlpha(BASE_ALPHA * (0.6f + 0.4f * density));
    mist.setMistScale(BASE_SCALE * (0.7f + 0.6f * (float) random.nextDouble()));
  }

  /**
   * Scales density by the game's own particle setting, so a player on a weak machine already has
   * the control without the mod inventing a config system.
   */
  private static float particleQuality(MinecraftClient client) {
    ParticlesMode mode = client.options.getParticles().getValue();

    return switch (mode) {
      case ALL -> 1.0f;
      case DECREASED -> 0.5f;
      case MINIMAL -> 0.0f;
    };
  }
}
