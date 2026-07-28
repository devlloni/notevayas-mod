package com.devllo.notevayas;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Pestana propia del creativo con todo el mod, incluido el bong y las semillas.
 */
public final class ModCreativeTab {
	public static final ResourceKey<CreativeModeTab> KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, NoTeVayas.id("general"));

	private ModCreativeTab() {
	}

	public static void register() {
		// En 26.2 builder() pide fila y columna; Fabric reubica las pestanas de mods.
		CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("itemGroup.notevayas.general"))
				.icon(() -> new ItemStack(ModItems.COGOLLOS.get(Cepa.PIEDRA_ROJA)))
				.displayItems((parametros, salida) -> {
					for (Cepa cepa : Cepa.values()) {
						salida.accept(ModItems.SEMILLAS.get(cepa));
					}
					for (Cepa cepa : Cepa.values()) {
						salida.accept(ModItems.COGOLLOS_FRESCOS.get(cepa));
					}
					for (Cepa cepa : Cepa.values()) {
						salida.accept(ModItems.COGOLLOS.get(cepa));
					}

					salida.accept(ModItems.PAPEL);
					salida.accept(ModItems.MOLEDOR);
					salida.accept(ModItems.PORRO);
					salida.accept(ModItems.BLUNT);
					salida.accept(ModItems.BROWNIE);
					salida.accept(ModItems.SPACE_CAKE);

					salida.accept(ModItems.BONG);
					salida.accept(ModItems.SECADERO);
					salida.accept(ModItems.LAMPARA_CULTIVO);
					salida.accept(ModItems.MACETA_HIDROPONICA);
				})
				.build();

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, tab);
	}
}
