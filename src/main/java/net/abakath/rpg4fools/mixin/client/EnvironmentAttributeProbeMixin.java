package net.abakath.rpg4fools.mixin.client;

import net.abakath.rpg4fools.client.ResolvedAtmosphere;
import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grades the fog and sky by the current season and the biome underfoot.
 *
 * <p>Sky colour and the fog distances are environment attributes now, resolved through this one
 * probe, so grading them here reaches every consumer at once instead of chasing the renderer that
 * reads each one. The fog colour is graded later, in {@link AtmosphericFogEnvironmentMixin}.
 *
 * <p>Nothing is eased here. The probe interpolates between ticks and blends biomes spatially by
 * itself, which is the stepping FogTransition used to smooth over on the old API - and easing on
 * top of that fought it, because getValue is asked many times a frame and by more than one probe.
 *
 * <p>Vanilla still computes every value, including its own time of day and weather handling. Only
 * the result is adjusted, so nothing about the existing behaviour is replaced.
 *
 * <p>Submersion needs no guard here. Water has its own WATER_FOG_ attributes, and lava and powder
 * snow are separate fog environments that never read these, so a camera under any of them is
 * already untouched.
 */
@Mixin(EnvironmentAttributeProbe.class)
public abstract class EnvironmentAttributeProbeMixin {
  @Shadow
  private Level level;

  @Shadow
  private Vec3 position;

  /**
   * Guards the one re-entrant lookup below. While set, this hook hands back vanilla's own value,
   * which is exactly what that lookup is asking for.
   */
  private static boolean rpg4fools$resolving;

  @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
  private void rpg4fools$gradeAtmosphere(EnvironmentAttribute<?> attribute,
                                         float partialTick,
                                         CallbackInfoReturnable<Object> cir) {
    if (rpg4fools$resolving || level == null || position == null) {
      return;
    }

    if (!level.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    Object vanilla = cir.getReturnValue();
    if (vanilla == null) {
      return;
    }

    if (attribute == EnvironmentAttributes.SKY_COLOR) {
      cir.setReturnValue(atmosphere().applyToSkyColor((Integer) vanilla));
      return;
    }

    if (attribute == EnvironmentAttributes.FOG_START_DISTANCE) {
      cir.setReturnValue(gradedStart((Float) vanilla, partialTick));
      return;
    }

    if (attribute == EnvironmentAttributes.FOG_END_DISTANCE) {
      cir.setReturnValue(atmosphere().fogEnd((Float) vanilla));
    }
  }

  /**
   * Where the fog begins.
   *
   * <p>The end distance has to be fetched rather than remembered, because the start is derived as a
   * ratio of where the fog ends and the two arrive as separate questions now.
   */
  private float gradedStart(float vanillaStart, float partialTick) {
    float vanillaEnd = vanillaFogEnd(partialTick);

    return atmosphere().fogStart(vanillaStart, vanillaEnd);
  }

  private float vanillaFogEnd(float partialTick) {
    EnvironmentAttributeProbe probe = (EnvironmentAttributeProbe) (Object) this;

    rpg4fools$resolving = true;
    try {
      return probe.getValue(EnvironmentAttributes.FOG_END_DISTANCE, partialTick);
    } finally {
      rpg4fools$resolving = false;
    }
  }

  private ResolvedAtmosphere atmosphere() {
    return SeasonAtmosphere.resolve(level, BlockPos.containing(position));
  }
}
