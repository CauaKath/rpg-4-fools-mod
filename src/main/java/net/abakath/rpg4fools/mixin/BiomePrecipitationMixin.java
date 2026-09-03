package net.abakath.rpg4fools.mixin;

import net.abakath.rpg4fools.world.SeasonSnow;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns rain into snow in the cold months, for biomes cold enough to qualify.
 *
 * <p>Hooked on warmEnoughToRain because that is the one place the three behaviours that ought to
 * agree all descend from:
 *
 * <pre>
 *   getPrecipitationAt -> coldEnoughToSnow -> warmEnoughToRain   what the weather draws
 *   shouldSnow         -> getPrecipitationAt -> ...              whether a chunk tick lays snow
 *   shouldFreeze       -> warmEnoughToRain                       whether standing water freezes
 * </pre>
 *
 * <p>Freezing is the reason this sits here rather than on getPrecipitationAt, which is the obvious
 * place and is wrong: shouldFreeze reaches warmEnoughToRain without passing through it, so a hook
 * up there gave snow that fell and settled over water that never froze.
 *
 * <p>Warm means rain and cold means snow, so the season flips the answer rather than replacing it.
 *
 * <p>A biome with no precipitation at all is left alone, which is what keeps desert, savanna and
 * badlands dry without naming them. That guard matters more here than it did on getPrecipitationAt,
 * which checks hasPrecipitation itself before it ever asks about temperature - shouldFreeze does
 * not, so without it a winter would ice over desert water.
 *
 * <p>Deliberately in the common mixin config: the server needs this to accumulate snow and to
 * freeze water, and the client needs it to draw snow instead of rain.
 */
@Mixin(Biome.class)
public abstract class BiomePrecipitationMixin {
  @Inject(method = "warmEnoughToRain", at = @At("RETURN"), cancellable = true)
  private void rpg4fools$applyWinterSnowLine(BlockPos pos, int seaLevel, CallbackInfoReturnable<Boolean> cir) {
    Biome biome = (Biome) (Object) this;

    if (!biome.hasPrecipitation()) {
      return;
    }

    float temperature = biome.getBaseTemperature();

    if (cir.getReturnValue()) {
      if (SeasonSnow.isSnowSeason() && temperature <= SeasonSnow.snowLineTemperature()) {
        cir.setReturnValue(false);
      }

      return;
    }

    // The inverse case. Without it a mild snowy biome would keep snowing right through the
    // midsummer thaw, so the melt pass would be clearing ground that the next snowfall covers
    // straight back up.
    if (SeasonSnow.shouldThaw(temperature)) {
      cir.setReturnValue(true);
    }
  }
}
