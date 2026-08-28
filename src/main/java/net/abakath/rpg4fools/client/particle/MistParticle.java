package net.abakath.rpg4fools.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/**
 * A single puff of mist.
 *
 * <p>The volume comes from many large, almost transparent puffs overlapping, not from any one of
 * them being visible. That is the difference between reading as thick air and reading as dust
 * floating around, so the defaults here are deliberately big and faint.
 *
 * <p>Drifts slowly, ignores gravity and does not collide, so it behaves like suspended air rather
 * than like falling matter.
 */
@Environment(EnvType.CLIENT)
public class MistParticle extends SpriteBillboardParticle {
  private static final float DEFAULT_ALPHA = 0.10f;
  private static final float DEFAULT_SCALE = 3.0f;
  private static final int MIN_AGE = 60;
  private static final int AGE_SPREAD = 80;

  /** Ticks spent fading in at the start and fading out at the end, so puffs never pop. */
  private static final int FADE_TICKS = 20;

  private float peakAlpha = DEFAULT_ALPHA;

  protected MistParticle(ClientWorld world,
                         double x,
                         double y,
                         double z,
                         double velocityX,
                         double velocityY,
                         double velocityZ,
                         SpriteProvider spriteProvider) {
    super(world, x, y, z, 0.0, 0.0, 0.0);

    this.setSprite(spriteProvider.getSprite(this.random));

    this.velocityX = velocityX;
    this.velocityY = velocityY;
    this.velocityZ = velocityZ;

    this.scale = DEFAULT_SCALE;
    this.maxAge = MIN_AGE + this.random.nextInt(AGE_SPREAD);
    this.gravityStrength = 0.0f;
    this.collidesWithWorld = false;
    this.velocityMultiplier = 1.0f;
    this.alpha = 0.0f;
  }

  /** Sets the colour of this puff. Called at spawn time from the biome atmosphere. */
  public void setMistColor(int rgb) {
    this.setColor(
            ((rgb >> 16) & 0xFF) / 255.0f,
            ((rgb >> 8) & 0xFF) / 255.0f,
            (rgb & 0xFF) / 255.0f
    );
  }

  /** Sets the opacity this puff fades up to. */
  public void setPeakAlpha(float peakAlpha) {
    this.peakAlpha = peakAlpha;
  }

  public void setMistScale(float scale) {
    this.scale = scale;
  }

  @Override
  public void tick() {
    super.tick();

    int ticksFromEdge = Math.min(this.age, this.maxAge - this.age);
    float fade = Math.min(1.0f, (float) ticksFromEdge / FADE_TICKS);

    this.alpha = this.peakAlpha * fade;
  }

  @Override
  public ParticleTextureSheet getType() {
    return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
  }

  @Environment(EnvType.CLIENT)
  public static class Factory implements ParticleFactory<SimpleParticleType> {
    private final SpriteProvider spriteProvider;

    public Factory(SpriteProvider spriteProvider) {
      this.spriteProvider = spriteProvider;
    }

    @Override
    public MistParticle createParticle(SimpleParticleType type,
                                       ClientWorld world,
                                       double x,
                                       double y,
                                       double z,
                                       double velocityX,
                                       double velocityY,
                                       double velocityZ) {
      return new MistParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
    }
  }
}
