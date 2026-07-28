package com.devllo.notevayas;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoTeVayas implements ModInitializer {
	public static final String MOD_ID = "notevayas";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModSounds.register();
		ModComponents.register();
		ModBlocks.register();
		ModItems.register();
		ModBlockEntities.register();
		ModEffects.register();
		ModRecipes.register();
		ModCreativeTab.register();
		ModWorldgen.register();

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> NoTeVayasCommand.register(dispatcher));

		LOGGER.info("NoTeVayas listo.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
