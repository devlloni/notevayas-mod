package com.devllo.notevayas;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Data components propios del mod.
 *
 * Al ser parte de la identidad del ItemStack (ver ItemStack.isSameItemSameComponents),
 * dos cogollos con distinta calidad no se apilan entre si sin que haya que hacer nada mas.
 */
public final class ModComponents {
	public static final DataComponentType<Calidad> CALIDAD = DataComponentType.<Calidad>builder()
			.persistent(Calidad.CODEC)
			.networkSynchronized(Calidad.STREAM_CODEC)
			.build();

	/**
	 * Cepa de origen. Los cogollos ya la saben por su Item, pero los consumibles
	 * (porro, blunt, brownie, space cake) son un unico item cada uno y necesitan
	 * recordar de que cepa salieron para saber que efectos aplicar.
	 */
	public static final DataComponentType<Cepa> CEPA = DataComponentType.<Cepa>builder()
			.persistent(Cepa.CODEC)
			.networkSynchronized(Cepa.STREAM_CODEC)
			.build();

	private ModComponents() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, NoTeVayas.id("calidad"), CALIDAD);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, NoTeVayas.id("cepa"), CEPA);
	}
}
