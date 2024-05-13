package net.abakath.rpg4fools;

import net.abakath.rpg4fools.events.ModEvents;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RPG4Fools implements ModInitializer {
	public static final String MOD_ID = "rpg4fools";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEvents.registerEvents();
		LOGGER.info("Hello Fabric world!");
	}
}