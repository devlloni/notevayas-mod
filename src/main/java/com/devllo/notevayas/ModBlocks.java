package com.devllo.notevayas;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * DECISION: 4 bloques separados (uno por cepa) en vez de un unico bloque "cultivo"
 * con una property de cepa.
 *
 * Motivos, en orden de peso:
 *  1. Las semillas son BlockItem. Con 4 bloques cada semilla planta el suyo y no hace
 *     falta tocar getStateForPlacement; con un bloque unico habria que escribir
 *     colocacion custom para setear la cepa.
 *  2. Loot tables: 4 tablas triviales contra una sola con 4 ramas de
 *     block_state_property por cada drop.
 *  3. Las reglas de crecimiento dependen de la cepa. Como campo final del bloque salen
 *     gratis; como property habria que leerlas del BlockState en cada random tick.
 *
 * El costo es el mismo en ambos casos: 4 cepas x 8 edades = 32 blockstates igual.
 * Las 8 texturas de etapa son compartidas, asi que los 4 bloques apuntan a los mismos
 * 8 modelos.
 */
public final class ModBlocks {
	public static final Map<Cepa, Block> CULTIVOS = new EnumMap<>(Cepa.class);

	public static final Block LAMPARA_CULTIVO = registrar("lampara_cultivo",
			LamparaCultivoBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(0.5f)
					.sound(SoundType.LANTERN)
					.lightLevel(state -> state.getValue(LamparaCultivoBlock.LIT)
							? LamparaCultivoBlock.LUZ_PRENDIDA
							: 0));

	public static final Block CANAMO_SILVESTRE = registrar("canamo_silvestre",
			CanamoSilvestreBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.PLANT)
					.noCollision()
					.instabreak()
					.sound(SoundType.GRASS)
					.pushReaction(PushReaction.DESTROY));

	public static final Block BONG = registrar("bong",
			BongBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_LIGHT_BLUE)
					.strength(0.5f)
					.sound(SoundType.GLASS)
					.noOcclusion());

	public static final Block SECADERO = registrar("secadero",
			SecaderoBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.WOOD)
					.strength(1.0f)
					.sound(SoundType.WOOD));

	public static final Block MACETA_HIDROPONICA = registrar("maceta_hidroponica",
			MacetaHidroponicaBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.TERRACOTTA_ORANGE)
					.strength(1.5f)
					.sound(SoundType.COPPER));

	static {
		for (Cepa cepa : Cepa.values()) {
			CULTIVOS.put(cepa, registrar(cepa.cultivoId(),
					props -> new CultivoBlock(cepa, props),
					propiedadesDeCultivo()));
		}
	}

	private ModBlocks() {
	}

	/**
	 * Las mismas properties que usa el trigo vanilla, pero escritas a mano en vez de
	 * con ofFullCopy(Blocks.WHEAT): ofFullCopy tambien copia los campos "drops" y
	 * "descriptionId". Son DependantName (se derivan de la id del bloque nuevo, asi que
	 * en la practica funcionarian), pero no quiero que la loot table de estos cultivos
	 * dependa de detalles internos del trigo.
	 */
	private static BlockBehaviour.Properties propiedadesDeCultivo() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.PLANT)
				.noCollision()
				.randomTicks()
				.instabreak()
				.sound(SoundType.CROP)
				.pushReaction(PushReaction.DESTROY);
	}

	/** Igual que los items, en 26.2 el bloque necesita su ResourceKey antes de construirse. */
	private static Block registrar(String nombre, Function<BlockBehaviour.Properties, Block> fabrica,
			BlockBehaviour.Properties properties) {
		Identifier id = NoTeVayas.id(nombre);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
		return Registry.register(BuiltInRegistries.BLOCK, key, fabrica.apply(properties.setId(key)));
	}

	public static void register() {
		// Fuerza la carga de la clase.
	}
}
