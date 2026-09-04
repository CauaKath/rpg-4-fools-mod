package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.CropItems;
import net.abakath.rpg4fools.world.CropSeasons;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Adds a crop's growing seasons to its tooltip, for the crop and for its seed alike.
 *
 * <p>Hidden behind Shift. A season list is reference material a player wants once per crop, not
 * every time they mouse over their inventory, so the default tooltip stays as short as vanilla's.
 *
 * <p>Each season is written in its own colour, which means colour cannot also mean "now". The
 * current season is bolded instead.
 */
@Environment(EnvType.CLIENT)
public final class CropSeasonTooltip {
  private CropSeasonTooltip() {
  }

  public static void register() {
    ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> append(stack, lines));
  }

  private static void append(ItemStack stack, List<Component> lines) {
    Optional<BlockState> crop = CropItems.cropFor(stack);

    if (crop.isEmpty()) {
      return;
    }

    if (!Screen.hasShiftDown()) {
      lines.add(Component.translatable("tooltip.rpg4fools.hold_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
      return;
    }

    lines.add(Component.translatable("tooltip.rpg4fools.seasons", seasonList(crop.get())).withStyle(ChatFormatting.GRAY));
  }

  private static MutableComponent seasonList(BlockState crop) {
    EnumSet<Season> seasons = CropSeasons.getSeasons(crop);
    Season current = currentSeason();

    MutableComponent list = Component.empty();
    boolean first = true;

    for (Season season : seasons) {
      if (!first) {
        list.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
      }

      MutableComponent name = Component.literal(season.getName()).withStyle(season.getColor());
      if (season == current) {
        name.withStyle(ChatFormatting.BOLD);
      }

      list.append(name);
      first = false;
    }

    return list;
  }

  /**
   * The season the player is standing in, or null outside a world.
   *
   * <p>ClientSeasonState carries a default season so rendering always has something to work with.
   * That default is a lie in a tooltip, where bolding a season would claim it is the current one,
   * so the world is checked rather than trusting the cached value on its own.
   */
  private static Season currentSeason() {
    if (Minecraft.getInstance().level == null) {
      return null;
    }

    return ClientSeasonState.getSubSeason().getSeason();
  }
}
