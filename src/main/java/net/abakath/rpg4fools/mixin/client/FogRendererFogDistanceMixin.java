package net.abakath.rpg4fools.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.abakath.rpg4fools.client.FogTransition;
import net.abakath.rpg4fools.client.ResolvedAtmosphere;
import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Closes the fog in around the player according to the biome they are standing in.
 *
 * <p>This is the same fog vanilla draws on the horizon, only brought near. Because vanilla computes
 * it from the camera position, the effect is felt only from inside a biome: standing outside a
 * swamp looking in, its fog is not yours.
 *
 * <p>Distances are set in blocks rather than scaled from the view distance. A scale factor means a
 * swamp feels completely different at render distance 8 and 32, and it cannot express "you can see
 * about 24 blocks in here" at all.
 *
 * <p>Runs at TAIL so vanilla has already set its own values, and only for FOG_TERRAIN. FOG_SKY uses
 * the same method, and overriding that too would wash the horizon out.
 */
@Mixin(FogRenderer.class)
public class FogRendererFogDistanceMixin {
  @Inject(method = "setupFog", at = @At("TAIL"))
  private static void rpg4fools$applyBiomeFog(Camera camera,
                                              FogRenderer.FogMode fogType,
                                              float viewDistance,
                                              boolean thickFog,
                                              float tickDelta,
                                              CallbackInfo ci) {
    if (fogType != FogRenderer.FogMode.FOG_TERRAIN) {
      return;
    }

    if (camera == null || camera.getFluidInCamera() != FogType.NONE) {
      return;
    }

    Minecraft client = Minecraft.getInstance();
    ClientLevel world = client.level;

    if (world == null || !world.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    ResolvedAtmosphere atmosphere = SeasonAtmosphere.resolve(world, BlockPos.containing(camera.getPosition()));

    // Eased rather than applied straight. The biome blend moves in steps as samples cross a border,
    // and with a swamp at 24 blocks against a forest at 146 a single step is a visible jump. Note
    // this runs even at zero presence, so leaving a biome eases back to vanilla instead of snapping.
    float vanillaStart = RenderSystem.getShaderFogStart();
    float vanillaEnd = RenderSystem.getShaderFogEnd();

    float start = FogTransition.update(
            atmosphere.fogStart(vanillaStart, vanillaEnd),
            atmosphere.fogEnd(vanillaEnd),
            System.nanoTime()
    );

    RenderSystem.setShaderFogStart(start);
    RenderSystem.setShaderFogEnd(FogTransition.getEnd());
  }
}
