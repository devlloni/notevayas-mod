package com.devllo.notevayas;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Efectos que aplica cada cepa al consumirse.
 *
 * Duracion final = duracion del perfil x multiplicador de calidad x penalizacion de la
 * cepa (Media Luna dura 40% menos) x factor del perfil (el cogollo fresco va al 30%).
 * Amplificador final = base del efecto + bonus de calidad Elite + bonus del perfil.
 */
public final class EfectosCepa {
	/** Media Luna no tiene contraefecto, pero dura 40% menos. */
	private static final float PENALIZACION_MEDIA_LUNA = 0.60f;

	/** Nausea fija de Verde Sol, no escala con calidad ni perfil. */
	private static final int NAUSEA_VERDE_SOL = 5 * 20;

	private EfectosCepa() {
	}

	/** Un efecto de la cepa: el efecto vanilla y su amplificador base. */
	private record Efecto(Holder<MobEffect> efecto, int amplificadorBase, boolean beneficioso) {
		static Efecto bueno(Holder<MobEffect> efecto, int amplificadorBase) {
			return new Efecto(efecto, amplificadorBase, true);
		}

		static Efecto contra(Holder<MobEffect> efecto, int amplificadorBase) {
			return new Efecto(efecto, amplificadorBase, false);
		}
	}

	private static List<Efecto> efectosDe(Cepa cepa) {
		return switch (cepa) {
			case PIEDRA_ROJA -> List.of(
					Efecto.bueno(MobEffects.RESISTANCE, 1),
					Efecto.bueno(MobEffects.REGENERATION, 0),
					Efecto.bueno(MobEffects.ABSORPTION, 0),
					Efecto.contra(MobEffects.SLOWNESS, 0));
			case VERDE_SOL -> List.of(
					Efecto.bueno(MobEffects.SPEED, 1),
					Efecto.bueno(MobEffects.HASTE, 0),
					Efecto.bueno(MobEffects.JUMP_BOOST, 0));
			case MEDIA_LUNA -> List.of(
					Efecto.bueno(MobEffects.RESISTANCE, 0),
					Efecto.bueno(MobEffects.SPEED, 0),
					Efecto.bueno(MobEffects.REGENERATION, 0));
			case PURPURA_NOCTURNA -> List.of(
					Efecto.bueno(MobEffects.NIGHT_VISION, 0),
					Efecto.bueno(MobEffects.SLOW_FALLING, 0),
					Efecto.bueno(MobEffects.ABSORPTION, 1),
					Efecto.contra(MobEffects.WEAKNESS, 0));
		};
	}

	public static int duracionFinal(Cepa cepa, Calidad calidad, PerfilConsumo perfil) {
		float duracion = perfil.duracionBase()
				* calidad.multiplicadorDuracion()
				* perfil.factorDuracion();
		if (cepa == Cepa.MEDIA_LUNA) {
			duracion *= PENALIZACION_MEDIA_LUNA;
		}
		return Math.max(1, Math.round(duracion));
	}

	/**
	 * Punto de entrada al consumir. Si el perfil tiene onset retardado, en vez de los
	 * efectos reales aplica el efecto "digestion", que los dispara al expirar.
	 */
	public static void consumir(LivingEntity entidad, Cepa cepa, Calidad calidad, PerfilConsumo perfil) {
		if (perfil.nauseaInicial() > 0) {
			entidad.addEffect(new MobEffectInstance(MobEffects.NAUSEA, perfil.nauseaInicial(), 0));
		}

		if (perfil.tieneOnsetRetardado()) {
			entidad.addEffect(new MobEffectInstance(ModEffects.DIGESTION, perfil.onset(),
					EfectoDigestion.codificar(cepa, calidad, perfil)));
			return;
		}

		aplicarReales(entidad, cepa, calidad, perfil);
	}

	/** Los efectos de verdad. Lo llama consumir() o, si hubo onset, EfectoDigestion. */
	public static void aplicarReales(LivingEntity entidad, Cepa cepa, Calidad calidad,
			PerfilConsumo perfil) {
		int duracion = duracionFinal(cepa, calidad, perfil);

		for (Efecto efecto : efectosDe(cepa)) {
			// El bonus de calidad y de perfil solo sube los efectos buenos; los
			// contraefectos se quedan en su amplificador base.
			int amplificador = efecto.beneficioso()
					? efecto.amplificadorBase() + calidad.bonusAmplificador() + perfil.bonusAmplificador()
					: efecto.amplificadorBase();
			entidad.addEffect(new MobEffectInstance(efecto.efecto(), duracion, amplificador));
		}

		if (cepa == Cepa.VERDE_SOL) {
			entidad.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_VERDE_SOL, 0));
		}

		// Munchies acompana a cualquier consumible, con la misma duracion.
		entidad.addEffect(new MobEffectInstance(ModEffects.MUNCHIES, duracion, 0));
	}
}
