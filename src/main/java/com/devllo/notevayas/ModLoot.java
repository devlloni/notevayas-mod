package com.devllo.notevayas;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * Segunda via para conseguir semillas: cortando pasto, igual que las de trigo.
 *
 * El canamo silvestre generado en el mundo es la via principal, pero solo aparece en
 * chunks NUEVOS: en un mundo ya explorado no sirve de nada. Esto se aplica al romper el
 * pasto en cualquier lado, asi que arranca a funcionar en el mundo que ya tenias.
 *
 * Se agrega un pool aparte en vez de tocar el de vanilla: el pool original sigue tirando
 * sus semillas de trigo como siempre y este tira la de canamo por su cuenta.
 */
public final class ModLoot {
	/** Bastante mas bajo que el 12.5% de las semillas de trigo: no queremos inundar. */
	private static final float PROBABILIDAD = 0.03f;

	private ModLoot() {
	}

	/** Las loot tables de los pastos que pueden dar semilla. */
	private static Set<ResourceKey<LootTable>> pastos() {
		return Stream.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN)
				.map(Block::getLootTable)
				.flatMap(Optional::stream)
				.collect(Collectors.toUnmodifiableSet());
	}

	public static void register() {
		Set<ResourceKey<LootTable>> pastos = pastos();

		LootTableEvents.MODIFY.register((clave, tabla, origen, registros) -> {
			if (!pastos.contains(clave)) {
				return;
			}

			// Mismos pesos que la loot table del canamo silvestre: Purpura Nocturna es la rara.
			tabla.withPool(LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1.0f))
					.when(LootItemRandomChanceCondition.randomChance(PROBABILIDAD))
					.add(LootItem.lootTableItem(ModItems.SEMILLAS.get(Cepa.PIEDRA_ROJA)).setWeight(2))
					.add(LootItem.lootTableItem(ModItems.SEMILLAS.get(Cepa.VERDE_SOL)).setWeight(2))
					.add(LootItem.lootTableItem(ModItems.SEMILLAS.get(Cepa.MEDIA_LUNA)).setWeight(2))
					.add(LootItem.lootTableItem(ModItems.SEMILLAS.get(Cepa.PURPURA_NOCTURNA)).setWeight(1)));
		});
	}
}
