package com.devllo.notevayas;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
	// Cogollos: llevan el component de calidad por defecto en REGULAR, asi que
	// cualquier stack (incluso los del creative o /give) tiene calidad valida.
	public static final Map<Cepa, Item> SEMILLAS = new EnumMap<>(Cepa.class);
	public static final Map<Cepa, Item> COGOLLOS_FRESCOS = new EnumMap<>(Cepa.class);
	public static final Map<Cepa, Item> COGOLLOS = new EnumMap<>(Cepa.class);

	public static final Item PAPEL = registrar("papel", Item::new, new Item.Properties());
	public static final Item MOLEDOR = registrar("moledor", Item::new,
			new Item.Properties().durability(64));
	// Los consumibles se fuman/comen; canAlwaysEat = true para poder usarlos con el
	// hambre llena (fumar no alimenta).
	public static final Item PORRO = registrar("porro",
			props -> new ConsumibleItem(PerfilConsumo.PORRO, props),
			new Item.Properties().food(new FoodProperties(0, 0.0f, true)));
	public static final Item BLUNT = registrar("blunt",
			props -> new ConsumibleItem(PerfilConsumo.BLUNT, props),
			new Item.Properties().food(new FoodProperties(0, 0.0f, true)));
	public static final Item BROWNIE = registrar("brownie",
			props -> new ConsumibleItem(PerfilConsumo.BROWNIE, props),
			new Item.Properties().food(new FoodProperties(4, 0.3f, true)));
	public static final Item SPACE_CAKE = registrar("space_cake",
			props -> new ConsumibleItem(PerfilConsumo.SPACE_CAKE, props),
			new Item.Properties().food(new FoodProperties(6, 0.4f, true)));

	public static final Item BONG = registrar("bong",
			props -> new BlockItem(ModBlocks.BONG, props), new Item.Properties());

	public static final Item LAMPARA_CULTIVO = registrar("lampara_cultivo",
			props -> new BlockItem(ModBlocks.LAMPARA_CULTIVO, props), new Item.Properties());
	public static final Item MACETA_HIDROPONICA = registrar("maceta_hidroponica",
			props -> new BlockItem(ModBlocks.MACETA_HIDROPONICA, props), new Item.Properties());
	public static final Item SECADERO = registrar("secadero",
			props -> new BlockItem(ModBlocks.SECADERO, props), new Item.Properties());

	static {
		for (Cepa cepa : Cepa.values()) {
			// La semilla es el BlockItem del cultivo. useItemDescriptionPrefix() hace que
			// use la clave de lang "item.notevayas.semilla_x" en vez de la del bloque,
			// igual que wheat_seeds en vanilla.
			SEMILLAS.put(cepa, registrar(cepa.semillaId(),
					props -> new BlockItem(ModBlocks.CULTIVOS.get(cepa), props),
					new Item.Properties().useItemDescriptionPrefix()));

			// El fresco se puede comer directo (con penalizacion); el seco no, va a
			// crafteo o al bong.
			COGOLLOS_FRESCOS.put(cepa, registrar(cepa.cogolloFrescoId(),
					props -> new CogolloItem(cepa, true, props),
					new Item.Properties()
							.component(ModComponents.CALIDAD, new Calidad(Calidad.REGULAR))
							.food(new FoodProperties(1, 0.0f, true))));

			COGOLLOS.put(cepa, registrar(cepa.cogolloId(),
					props -> new CogolloItem(cepa, false, props),
					new Item.Properties().component(ModComponents.CALIDAD, new Calidad(Calidad.REGULAR))));
		}
	}

	private ModItems() {
	}

	/**
	 * En 26.2 Item.Properties.setId(ResourceKey) es obligatorio: el item necesita
	 * conocer su propia id antes de construirse.
	 */
	private static Item registrar(String nombre, Function<Item.Properties, Item> fabrica,
			Item.Properties properties) {
		Identifier id = NoTeVayas.id(nombre);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, key, fabrica.apply(properties.setId(key)));
	}

	public static void register() {
		// Fuerza la carga de la clase: el trabajo real lo hacen los inicializadores estaticos.
	}
}
