package net.abakath.rpg4fools.server.debug;

import com.mojang.brigadier.context.CommandContext;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.abakath.rpg4fools.world.SeasonSnow;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Reports everything the snow line decision is made from, at the caller's position.
 *
 * <p>Temporary. Exists because "no snow here, snow there" is not something the code can be read to
 * explain: the decision is uniform per biome, so two neighbouring columns disagreeing means either
 * they are different biomes or something is not behaving as written. This prints enough to tell
 * those apart in one command instead of another round of guessing.
 *
 * <p>Both the caller's own position and the top of the column are reported. Weather is decided at
 * the heightmap top rather than at the player's feet, and vanilla's own answer is height adjusted,
 * so a difference between the two rows is itself informative.
 */
public final class SnowDebugCommand {
  private SnowDebugCommand() {
  }

  public static void register() {
    CommandRegistrationCallback.EVENT.register((dispatcher, registries, selection) ->
            dispatcher.register(Commands.literal("rpg4fools")
                    .then(Commands.literal("debug")
                            .then(Commands.literal("snow")
                                    .executes(SnowDebugCommand::report)))));
  }

  private static int report(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    ServerLevel world = source.getLevel();

    BlockPos feet = BlockPos.containing(source.getPosition());
    BlockPos top = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, feet);

    say(source, "--- rpg4fools snow debug");
    say(source, "season      " + CurrentSeason.get()
            + "   snowLine=" + SeasonSnow.snowLineTemperature()
            + "   isSnowSeason=" + SeasonSnow.isSnowSeason());
    say(source, "seaLevel    " + world.getSeaLevel());

    describe(source, world, "feet", feet);
    describe(source, world, "top ", top);

    return 1;
  }

  private static void describe(CommandSourceStack source, ServerLevel world, String label, BlockPos pos) {
    Holder<Biome> entry = world.getBiome(pos);
    Biome biome = entry.value();
    int seaLevel = world.getSeaLevel();

    float temperature = biome.getBaseTemperature();
    boolean qualifies = SeasonSnow.isSnowSeason() && temperature <= SeasonSnow.snowLineTemperature();

    say(source, label + " " + pos.toShortString()
            + "  " + entry.unwrapKey().map(key -> key.identifier().toString()).orElse("<unregistered>"));
    say(source, "     temp=" + temperature
            + " hasPrecipitation=" + biome.hasPrecipitation()
            + " shouldThaw=" + SeasonSnow.shouldThaw(temperature));
    say(source, "     warmEnoughToRain=" + biome.warmEnoughToRain(pos, seaLevel)
            + " coldEnoughToSnow=" + biome.coldEnoughToSnow(pos, seaLevel)
            + " precipitation=" + biome.getPrecipitationAt(pos, seaLevel));
    say(source, "     shouldSnow=" + biome.shouldSnow(world, pos)
            + " shouldFreeze=" + biome.shouldFreeze(world, pos)
            + " expected=" + (biome.hasPrecipitation() ? (qualifies ? "SNOW" : "as vanilla") : "NONE"));
  }

  private static void say(CommandSourceStack source, String line) {
    source.sendSuccess(() -> Component.literal(line), false);
  }
}
