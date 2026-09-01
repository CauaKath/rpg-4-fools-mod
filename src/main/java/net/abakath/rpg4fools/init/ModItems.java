package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.CropDefinition;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Items the mod adds.
 *
 * <p>A farmland crop has two: a seed that plants it and produce that feeds you. A bush has one,
 * because a berry is both, exactly as sweet berries are.
 *
 * <p>The crop stick and the crop wall are the two items here that plant nothing. They are blocks the
 * player places, and what grows on them comes later.
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
  public static final Item CROP_STICK = register("crop_stick", props -> new BlockItem(ModBlocks.CROP_STICK, props));

  /**
   * The wall panel, as a thing to carry.
   *
   * <p>A plain BlockItem like the stick, and with less for its block to check: a panel goes wherever
   * the player points it. The two placements vanilla cannot express - a panel onto a cucumber already
   * in the ground, and a seed into a panel already standing - are handled in
   * {@link net.abakath.rpg4fools.server.events.CropWallHandling}.
   */
  public static final Item CROP_WALL = register("crop_wall", props -> new BlockItem(ModBlocks.CROP_WALL, props));

  private static final Map<CropDefinition, Item> SEEDS = new LinkedHashMap<>();
  private static final Map<CropDefinition, Item> PRODUCE = new LinkedHashMap<>();

  static {
    for (CropDefinition definition : ModCrops.ALL) {
      Block block = ModBlocks.blockFor(definition);

      if (definition.kind() == CropDefinition.Kind.FARMLAND) {
        SEEDS.put(definition, register(definition.seedName(), props -> new BlockItem(block, props.useItemDescriptionPrefix())));
        PRODUCE.put(definition, register(definition.produceName(), props -> new Item(props.food(food(definition)))));
        continue;
      }

      Item berry = register(definition.produceName(),
              props -> new BlockItem(block, props.food(food(definition)).useItemDescriptionPrefix()));

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

  private static FoodProperties food(CropDefinition definition) {
    return new FoodProperties.Builder()
            .nutrition(definition.nutrition())
            .saturationModifier(definition.saturation())
            .build();
  }

  /**
   * An item has to be told its own registry key before it is constructed, so the key is built here
   * and handed to the factory as part of the properties.
   */
  private static Item register(String name, Function<Item.Properties, Item> factory) {
    ResourceKey<Item> key =
            ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, name));

    return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
  }
}
