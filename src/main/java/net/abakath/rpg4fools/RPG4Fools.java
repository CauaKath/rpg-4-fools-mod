package net.abakath.rpg4fools;

import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModEvents;
import net.abakath.rpg4fools.init.ModItemGroups;
import net.abakath.rpg4fools.init.ModItems;
import net.abakath.rpg4fools.init.ModMessages;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RPG4Fools implements ModInitializer {
	public static final String MOD_ID = "rpg4fools";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.registerBlocks();
		ModItems.registerItems();
		ModItemGroups.registerItemGroups();
		ModEvents.registerEvents();
		ModMessages.registerS2CPackets();
	}
}