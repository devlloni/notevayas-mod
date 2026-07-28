package com.devllo.notevayas;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	/** En 26.2 BlockEntityType se construye directo, ya no hay BlockEntityType.Builder. */
	public static final BlockEntityType<SecaderoBlockEntity> SECADERO = new BlockEntityType<>(
			SecaderoBlockEntity::new, Set.of(ModBlocks.SECADERO));

	private ModBlockEntities() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, NoTeVayas.id("secadero"), SECADERO);
	}
}
