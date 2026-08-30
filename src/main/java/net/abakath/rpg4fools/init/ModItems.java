package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.CropDefinition;
import net.minecraft.block.Block;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Items the mod adds.
 *
 * <p>A farmland crop has two: a seed that plants it and produce that feeds you. A bush has one,
 * because a berry is both, exactly as sweet berries are.
 *
 * <p>The crop stick is the one item here that plants nothing. It is a block the player places, and
 * what grows on it comes later.
 *
 * <p>Seeds and berries are AliasedBlockItem so they plant on use while reading as an item rather
 * than a block. That also means {@link net.abakath.rpg4fools.world.CropItems} resolves them through
 * its BlockItem branch with no entry of its own.
 */
public final class ModItems {
  /**
   * The trellis, as a thing to carry.
   *
   * <p>A plain BlockItem, unlike everything else here: placing one on farmland or on top of another
   * is ordinary block placement, and the block's own placement rules are what keep a column three
   * high and rooted. Only the two placements vanilla cannot express - a stick onto a crop already in
   * the ground, and a seed into a stick already standing - are handled elsewhere, in
   * {@link net.abakath.rpg4fools.server.events.CropStickHandling}.
   */
  public static final Item CROP_STICK = register("crop_stick", new BlockItem(ModBlocks.CROP_STICK, new Item.Settings()));

  private static final Map<CropDefinition, Item> SEEDS = new LinkedHashMap<>();
  private static final Map<CropDefinition, Item> PRODUCE = new LinkedHashMap<>();

  static {
    for (CropDefinition definition : ModCrops.ALL) {
      Block block = ModBlocks.blockFor(definition);

      if (definition.kind() == CropDefinition.Kind.FARMLAND) {
        SEEDS.put(definition, register(definition.seedName(), new AliasedBlockItem(block, new Item.Settings())));
        PRODUCE.put(definition, register(definition.produceName(), new Item(new Item.Settings().food(food(definition)))));
        continue;
      }

      Item berry = register(definition.produceName(),
              new AliasedBlockItem(block, new Item.Settings().food(food(definition))));

      SEEDS.put(definition, berry);
      PRODUCE.put(definition, berry);
    }
  }

  private ModItems() {
  }

  public static Item seedItem(CropDefinition definition) {
    return SEEDS.get(definition);
  }

  public static Item produceItem(CropDefinition definition) {
    return PRODUCE.get(definition);
  }

  /**
   * The seed that plants this block.
   *
   * <p>Falls back to wheat seeds rather than null: this answers CropBlock.getSeedsItem, which
   * vanilla calls for pick block, and a null there would crash the client on a middle click.
   */
  public static Item seedFor(Block block) {
    CropDefinition definition = ModBlocks.definitionFor(block);
    return definition == null ? Items.WHEAT_SEEDS : SEEDS.get(definition);
  }

  /** The berry this bush gives. Falls back to sweet berries for the same reason. */
  public static Item berryFor(Block block) {
    CropDefinition definition = ModBlocks.definitionFor(block);
    return definition == null ? Items.SWEET_BERRIES : PRODUCE.get(definition);
  }

  /**
   * Forces this class to load, which is what performs the registrations above.
   *
   * <p>Called from the mod initialiser after the blocks, since every item here is built from one.
   */
  public static void registerItems() {
  }

  private static FoodComponent food(CropDefinition definition) {
    return new FoodComponent.Builder()
            .nutrition(definition.nutrition())
            .saturationModifier(definition.saturation())
            .build();
  }

  private static Item register(String name, Item item) {
    return Registry.register(Registries.ITEM, new Identifier(RPG4Fools.MOD_ID, name), item);
  }
}
