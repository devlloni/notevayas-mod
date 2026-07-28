package com.devllo.notevayas;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Inyecta el canamo silvestre en los biomas donde aparece.
 *
 * El feature en si esta definido en JSON (data/notevayas/worldgen/), aca solo se dice en
 * que biomas va: los JSON de worldgen no pueden agregarse a biomas vanilla sin pisar el
 * bioma entero.
 */
public final class ModWorldgen {
	public static final ResourceKey<PlacedFeature> CANAMO_SILVESTRE = ResourceKey.create(
			Registries.PLACED_FEATURE, NoTeVayas.id("canamo_silvestre"));

	private ModWorldgen() {
	}

	public static void register() {
		BiomeModifications.addFeature(
				BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.SAVANNA, Biomes.FOREST, Biomes.TAIGA),
				GenerationStep.Decoration.VEGETAL_DECORATION,
				CANAMO_SILVESTRE);
	}
}
