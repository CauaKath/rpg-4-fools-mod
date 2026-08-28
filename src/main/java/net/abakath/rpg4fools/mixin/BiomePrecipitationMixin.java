package net.abakath.rpg4fools.mixin;

import net.abakath.rpg4fools.world.SeasonSnow;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns rain into snow in the cold months, for biomes cold enough to qualify.
 *
 * <p>This one method is where vanilla decides rain against snow for weather rendering, for whether
 * a chunk tick lays down a snow layer, and for whether standing water freezes. Overriding it here
 * means all three follow the season together rather than being wired up separately.
 *
 * <p>Only a RAIN result is changed. A biome that returns NONE has no precipitation at all, which is
 * what keeps desert, savanna and badlands dry without naming them, and a biome already returning
 * SNOW is left alone so snowy and taiga behave as they always did.
 *
 * <p>Deliberately in the common mixin config: the server needs this to accumulate snow, and the
 * client needs it to draw snow instead of rain.
 */
@Mixin(Biome.class)
public abstract class BiomePrecipitationMixin {
  @Inject(method = "getPrecipitation", at = @At("RETURN"), cancellable = true)
  private void rpg4fools$applyWinterSnowLine(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
    Biome.Precipitation vanilla = cir.getReturnValue();

    // NONE means the biome has no precipitation at all. That is what keeps desert, savanna and
    // badlands dry without naming any of them, so it is never overridden in either direction.
    if (vanilla == Biome.Precipitation.NONE) {
      return;
    }

    float temperature = ((Biome) (Object) this).getTemperature();

    if (vanilla == Biome.Precipitation.RAIN) {
      if (SeasonSnow.isSnowSeason() && temperature <= SeasonSnow.snowLineTemperature()) {
        cir.setReturnValue(Biome.Precipitation.SNOW);
      }

      return;
    }

    // The inverse case. Without it a mild snowy biome would keep snowing right through the
    // midsummer thaw, so the melt pass would be clearing ground that the next snowfall covers
    // straight back up.
    if (SeasonSnow.shouldThaw(temperature)) {
      cir.setReturnValue(Biome.Precipitation.RAIN);
    }
  }
}
