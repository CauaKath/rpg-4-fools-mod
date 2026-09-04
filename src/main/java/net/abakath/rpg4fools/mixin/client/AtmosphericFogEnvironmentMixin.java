package net.abakath.rpg4fools.mixin.client;

import net.abakath.rpg4fools.client.FogEasing;
import net.abakath.rpg4fools.client.ResolvedAtmosphere;
import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grades the fog by the current season and the biome underfoot: its colour, and how near it closes.
 *
 * <p>Graded here rather than on the FOG_COLOR attribute the environment reads. That attribute is
 * the raw biome colour, and vanilla still has its sunrise and sunset work to do afterwards, which
 * washed most of a swamp's green back out. This is the finished colour, which is what the old hook
 * on the fog renderer graded.
 *
 * <p>The distances are graded here rather than on the FOG_START_DISTANCE and FOG_END_DISTANCE
 * attributes for a reason worth keeping. The start is derived as a ratio of where the fog ends, so
 * grading the attributes meant answering one question by asking the probe the other, behind a flag
 * to stop the hook recursing - and that ran per lookup rather than per frame. Here both values sit
 * on the FogData together and are written exactly once a frame.
 *
 * <p>Only the atmospheric environment is touched, so a camera in water, lava or powder snow keeps
 * its own fog: those are separate environments that never run this.
 */
@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {
  @Inject(method = "getBaseColor", at = @At("RETURN"), cancellable = true)
  private void rpg4fools$gradeFogColor(ClientLevel world,
                                       Camera camera,
                                       int viewDistance,
                                       float partialTick,
                                       CallbackInfoReturnable<Integer> cir) {
    if (world == null || camera == null || !world.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    cir.setReturnValue(
            SeasonAtmosphere.resolve(world, camera.blockPosition()).applyToFogColor(cir.getReturnValue()));
  }

  /**
   * Runs at TAIL, so these are vanilla's finished distances - its rain multiplier included - and
   * the grade is applied on top of them the same way it is applied on top of its fog colour.
   */
  @Inject(method = "setupFog", at = @At("TAIL"))
  private void rpg4fools$closeFogIn(FogData data,
                                    Camera camera,
                                    ClientLevel world,
                                    float viewDistance,
                                    DeltaTracker tickDelta,
                                    CallbackInfo ci) {
    if (world == null || camera == null || !world.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    ResolvedAtmosphere atmosphere = SeasonAtmosphere.resolve(world, camera.blockPosition());

    float vanillaStart = data.environmentalStart;
    float vanillaEnd = data.environmentalEnd;

    // Eased rather than applied straight. What the atmosphere reports moves in steps - the
    // aggregate is cached per four block cell, sky occlusion is counted in ninths - and this is the
    // one place that runs once a frame, which is what an easer needs to integrate correctly.
    FogEasing.advance(
            atmosphere.fogStart(vanillaStart, vanillaEnd),
            atmosphere.fogEnd(vanillaEnd),
            tickDelta.getRealtimeDeltaTicks()
    );

    data.environmentalStart = FogEasing.start();
    data.environmentalEnd = FogEasing.end();
  }
}
