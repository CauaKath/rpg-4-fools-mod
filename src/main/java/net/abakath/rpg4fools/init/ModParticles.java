package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Particle types used by the mod.
 *
 * <p>Registered from the common entrypoint on purpose. Particle types live in a synced registry, so
 * registering them only on the client would leave the client and a dedicated server with different
 * registry contents. The client side factory that actually draws the particle is registered
 * separately, in the client entrypoint.
 */
public class ModParticles {
  /**
   * A large, very faint billboard used to build volumetric looking mist. The colour is set per
   * instance at spawn time, so one type covers green swamp air and white freezing air alike.
   */
  public static final SimpleParticleType MIST = FabricParticleTypes.simple();

  public static void registerParticles() {
    Registry.register(Registries.PARTICLE_TYPE, new Identifier(RPG4Fools.MOD_ID, "mist"), MIST);
  }
}
