package net.abakath.rpg4fools.mixin.client;

import net.abakath.rpg4fools.client.FogTransition;
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
 * <p>One hook for what used to be three. Fog colour, fog distance and sky colour are all
 * environment attributes now, resolved through this one probe, so grading them here reaches every
 * consumer at once instead of chasing the renderer that reads each one.
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

    if (attribute == EnvironmentAttributes.FOG_COLOR) {
      cir.setReturnValue(atmosphere().applyToFogColor((Integer) vanilla));
      return;
    }

    if (attribute == EnvironmentAttributes.SKY_COLOR) {
      cir.setReturnValue(atmosphere().applyToSkyColor((Integer) vanilla));
      return;
    }

    if (attribute == EnvironmentAttributes.FOG_START_DISTANCE) {
      cir.setReturnValue(easedStart((Float) vanilla, partialTick));
      return;
    }

    if (attribute == EnvironmentAttributes.FOG_END_DISTANCE) {
      cir.setReturnValue(easedEnd((Float) vanilla));
    }
  }

  /**
   * Advances the easing and returns the smoothed fog start.
   *
   * <p>The biome blend moves in steps as samples cross a border, and with a swamp at 24 blocks
   * against a forest at 146 a single step is a visible jump. Note this runs even at zero presence,
   * so leaving a biome eases back to vanilla instead of snapping.
   *
   * <p>The end distance has to be fetched rather than remembered, because the start is derived as a
   * ratio of where the fog ends and the two arrive as separate questions now.
   */
  private float easedStart(float vanillaStart, float partialTick) {
    float vanillaEnd = vanillaFogEnd(partialTick);
    ResolvedAtmosphere atmosphere = atmosphere();

    return FogTransition.update(
            atmosphere.fogStart(vanillaStart, vanillaEnd),
            atmosphere.fogEnd(vanillaEnd),
            System.nanoTime()
    );
  }

  /**
   * The eased end that the start's easing pass already advanced.
   *
   * <p>Vanilla asks for the start before the end, so by the time this runs the pair has moved
   * together this frame. On the very first frame, before any easing exists, the target is used
   * directly.
   */
  private float easedEnd(float vanillaEnd) {
    float eased = FogTransition.getEnd();

    return Float.isNaN(eased) ? atmosphere().fogEnd(vanillaEnd) : eased;
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
