package com.devllo.notevayas;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Item que al consumirse aplica los efectos de su cepa con un perfil concreto.
 *
 * La cepa y la calidad viajan en components: las recetas de la Fase 6 las copian desde
 * el cogollo usado.
 */
public class ConsumibleItem extends Item {
	/** Si el stack no trae cepa (por ejemplo dado con /give), se asume esta. */
	private static final Cepa CEPA_POR_DEFECTO = Cepa.MEDIA_LUNA;

	private final PerfilConsumo perfil;

	public ConsumibleItem(PerfilConsumo perfil, Properties properties) {
		super(properties);
		this.perfil = perfil;
	}

	public PerfilConsumo perfil() {
		return this.perfil;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entidad) {
		Cepa cepa = stack.getOrDefault(ModComponents.CEPA, CEPA_POR_DEFECTO);
		Calidad calidad = stack.getOrDefault(ModComponents.CALIDAD, new Calidad(Calidad.REGULAR));

		// super consume el item y aplica la comida; los efectos van solo del lado servidor.
		ItemStack resultado = super.finishUsingItem(stack, level, entidad);

		if (!level.isClientSide()) {
			EfectosCepa.consumir(entidad, cepa, calidad, this.perfil);
		}
		return resultado;
	}
}
