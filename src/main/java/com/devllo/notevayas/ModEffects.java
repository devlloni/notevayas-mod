package com.devllo.notevayas;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
	/** Cuanto mas rapido baja el hambre con munchies (1.5 = 50% mas rapido). */
	public static final float MULTIPLICADOR_HAMBRE = 1.5f;
	/** Saturacion extra que da cualquier comida con munchies (0.5 = +50%). */
	public static final float BONUS_SATURACION = 0.5f;

	// registerForHolder devuelve el Holder que necesita MobEffectInstance.
	public static final Holder<MobEffect> MUNCHIES = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, NoTeVayas.id("munchies"),
			new EfectoMunchies(MobEffectCategory.NEUTRAL, 0x7CB342));

	public static final Holder<MobEffect> DIGESTION = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, NoTeVayas.id("digestion"),
			new EfectoDigestion(MobEffectCategory.NEUTRAL, 0x8D6E63));

	private ModEffects() {
	}

	public static void register() {
		// Fuerza la carga de la clase.
	}
}
