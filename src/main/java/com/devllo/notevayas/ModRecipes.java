package com.devllo.notevayas;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipes {
	/**
	 * La receta no tiene parametros propios (todas las combinaciones estan en la clase),
	 * asi que el serializer usa codecs de unidad.
	 */
	// En 26.2 RecipeSerializer es un record de (MapCodec, StreamCodec), no una interfaz
	// que se implemente.
	public static final RecipeSerializer<RecetaConsumible> CONSUMIBLE = new RecipeSerializer<>(
			MapCodec.unit(RecetaConsumible::new),
			StreamCodec.unit(new RecetaConsumible()));

	private ModRecipes() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, NoTeVayas.id("consumible"), CONSUMIBLE);
	}
}
