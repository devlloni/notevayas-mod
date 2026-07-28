package com.devllo.notevayas;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Munchies: acompana a cualquier consumible. No hace nada por si mismo; la mecanica
 * (hambre 50% mas rapida, comida con 50% mas de saturacion) la aplican PlayerMixin y
 * ConsumableMixin consultando si el jugador tiene este efecto.
 *
 * Existe como subclase solo porque el constructor de MobEffect es protected.
 */
public class EfectoMunchies extends MobEffect {
	public EfectoMunchies(MobEffectCategory categoria, int color) {
		super(categoria, color);
	}
}
