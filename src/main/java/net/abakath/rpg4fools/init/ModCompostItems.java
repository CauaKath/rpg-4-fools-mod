package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.Compost;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The compost pipeline, as items.
 *
 * <p>Two families and one arrow between them. A catalyst is what a batch of compost is labelled
 * with before it is made; a compost is what comes out and goes into the ground. Each catalyst maps
 * to exactly one kind, which is what lets the composter and the soil agree without either of them
 * knowing about the other.
 *
 * <p>Kept out of {@link ModItems}, which is the crop roster's own file and derives every item in it
 * from a {@link net.abakath.rpg4fools.world.CropDefinition}. Nothing here comes from a crop.
 *
 * <p>The three catalysts are gathered three different ways on purpose - ash is burnt, bark is
 * stripped, manure is waited for - so the pipeline is not three trips to the same furnace.
 */
public final class ModCompostItems {
  /** Burnt garden waste. Smelted from leaves, which vanilla has no recipe for; logs already smelt to charcoal. */
  public static final Item ASH = register("ash");

  /**
   * Bark, one per wood.
   *
   * <p>Knocked off a log by an axe. The strip still happens; the bark is what was going to be
   * wasted.
   *
   * <p>Eight items rather than one because bark is the thing in this mod a player already knows the
   * colour of - oak and birch and dark oak are eight sprites they have looked at ten thousand times,
   * and collapsing them into a single brown item throws that away for nothing. They are
   * interchangeable everywhere it matters: any of them labels a composter batch as creeping.
   *
   * <p>Only the eight overworld logs. Bamboo is a block rather than a log, and crimson and warped are
   * fungus stems; all three strip in game and none of them has bark.
   */
  private static final Map<Block, Item> BARKS = new LinkedHashMap<>();

  private static final List<Item> BARK_ITEMS = new ArrayList<>();

  /** Left behind by animals, slowly. The only catalyst that is waited for rather than made. */
  public static final Item MANURE = register("manure");

  public static final Item RICH_COMPOST = register("rich_compost");
  public static final Item WARM_COMPOST = register("warm_compost");
  public static final Item CREEPING_COMPOST = register("creeping_compost");

  /**
   * Which compost a catalyst labels a batch with.
   *
   * <p>Manure feeds, so it makes the compost that feeds the harvest. Ash is what is left of a fire,
   * so it makes the one that hurries growth. Bark is what a climbing plant would find to climb, so
   * it makes the one that hurries spreading.
   */
  private static final Map<Item, Compost> CATALYSTS = new LinkedHashMap<>();

  /** The other direction, for putting a made compost back into the ground. */
  private static final Map<Item, Compost> COMPOSTS = new LinkedHashMap<>();

  private static final Map<Compost, Item> BY_KIND = new LinkedHashMap<>();

  static {
    bark("oak", Blocks.OAK_LOG, Blocks.OAK_WOOD);
    bark("spruce", Blocks.SPRUCE_LOG, Blocks.SPRUCE_WOOD);
    bark("birch", Blocks.BIRCH_LOG, Blocks.BIRCH_WOOD);
    bark("jungle", Blocks.JUNGLE_LOG, Blocks.JUNGLE_WOOD);
    bark("acacia", Blocks.ACACIA_LOG, Blocks.ACACIA_WOOD);
    bark("dark_oak", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_WOOD);
    bark("mangrove", Blocks.MANGROVE_LOG, Blocks.MANGROVE_WOOD);
    bark("cherry", Blocks.CHERRY_LOG, Blocks.CHERRY_WOOD);

    CATALYSTS.put(MANURE, Compost.RICH);
    CATALYSTS.put(ASH, Compost.WARM);

    for (Item item : BARK_ITEMS) {
      CATALYSTS.put(item, Compost.CREEPING);
    }

    COMPOSTS.put(RICH_COMPOST, Compost.RICH);
    COMPOSTS.put(WARM_COMPOST, Compost.WARM);
    COMPOSTS.put(CREEPING_COMPOST, Compost.CREEPING);

    COMPOSTS.forEach((item, kind) -> BY_KIND.put(kind, item));
  }

  private ModCompostItems() {
  }

  /**
   * Registers one wood's bark and points its logs at it.
   *
   * <p>Both the log and the all-bark wood variant, since an axe strips either and a player who built
   * with wood blocks should not find the interaction quietly stops working.
   */
  private static void bark(String wood, Block... logs) {
    Item item = register(wood + "_bark");

    BARK_ITEMS.add(item);

    for (Block log : logs) {
      BARKS.put(log, item);
    }
  }

  /** The bark this log gives up, or null for anything an axe has no bark for. */
  public static Item barkFor(Block log) {
    return BARKS.get(log);
  }

  /** The kind this stack would label a composter with, or null if it is not a catalyst. */
  public static Compost catalyst(ItemStack stack) {
    return CATALYSTS.get(stack.getItem());
  }

  /** The kind this stack would work into soil, or null if it is not a compost. */
  public static Compost compost(ItemStack stack) {
    return COMPOSTS.get(stack.getItem());
  }

  /** What a finished composter hands over. */
  public static Item compostFor(Compost kind) {
    return BY_KIND.get(kind);
  }

  /** In the order they are gathered, then the order they come out, for the creative tab. */
  public static Iterable<Item> all() {
    List<Item> items = new ArrayList<>();

    items.add(ASH);
    items.addAll(BARK_ITEMS);
    items.add(MANURE);
    items.add(RICH_COMPOST);
    items.add(WARM_COMPOST);
    items.add(CREEPING_COMPOST);

    return items;
  }

  /** Forces this class to load, which is what performs the registrations above. */
  public static void registerItems() {
  }

  private static Item register(String name) {
    return Registry.register(Registries.ITEM, new Identifier(RPG4Fools.MOD_ID, name), new Item(new Item.Settings()));
  }
}
