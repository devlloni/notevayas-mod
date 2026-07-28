package com.devllo.notevayas;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Cogollo (fresco o seco) de una cepa concreta.
 *
 * La linea de calidad del tooltip la aporta el propio component {@link Calidad},
 * que implementa TooltipProvider; aca no hace falta ningun override.
 */
public class CogolloItem extends Item {
	private static final Calidad POR_DEFECTO = new Calidad(Calidad.REGULAR);

	private final Cepa cepa;
	private final boolean fresco;

	public CogolloItem(Cepa cepa, boolean fresco, Properties properties) {
		super(properties);
		this.cepa = cepa;
		this.fresco = fresco;
	}

	public Cepa cepa() {
		return this.cepa;
	}

	public boolean esFresco() {
		return this.fresco;
	}

	/** Calidad de un stack, cayendo a REGULAR si todavia no tiene el component. */
	public static Calidad calidadDe(ItemStack stack) {
		return stack.getOrDefault(ModComponents.CALIDAD, POR_DEFECTO);
	}

	/**
	 * Comerse un cogollo fresco sin curar da los efectos al 30% y nausea garantizada.
	 * El cogollo seco no es comestible: se usa para craftear o en el bong.
	 */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entidad) {
		Calidad calidad = calidadDe(stack);
		ItemStack resultado = super.finishUsingItem(stack, level, entidad);

		if (this.fresco && !level.isClientSide()) {
			EfectosCepa.consumir(entidad, this.cepa, calidad, PerfilConsumo.COGOLLO_FRESCO);
		}
		return resultado;
	}
}
