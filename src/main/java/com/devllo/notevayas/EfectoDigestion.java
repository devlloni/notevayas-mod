package com.devllo.notevayas;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Onset retardado de brownie y space cake: mientras dura no hace nada, y al expirar
 * dispara los efectos reales.
 *
 * No hay hook de "efecto expirado" en MobEffect, asi que se usa
 * shouldApplyEffectTickThisTick para pedir un unico tick cuando queda 1 de duracion
 * (MobEffectInstance.tick lo consulta con la duracion restante antes de decrementarla).
 * Asi se evita cualquier scheduler global.
 *
 * MobEffectInstance no admite datos propios, asi que cepa/calidad/perfil viajan
 * codificados en el amplificador.
 */
public class EfectoDigestion extends MobEffect {
	private static final int FACTOR_CEPA = 8;
	private static final int FACTOR_CALIDAD = 2;

	public EfectoDigestion(MobEffectCategory categoria, int color) {
		super(categoria, color);
	}

	/** amplificador = cepa*8 + calidad*2 + (space cake ? 1 : 0). Maximo 31. */
	public static int codificar(Cepa cepa, Calidad calidad, PerfilConsumo perfil) {
		return cepa.ordinal() * FACTOR_CEPA
				+ calidad.nivel() * FACTOR_CALIDAD
				+ (perfil == PerfilConsumo.SPACE_CAKE ? 1 : 0);
	}

	private static Cepa cepaDe(int amplificador) {
		return Cepa.values()[(amplificador / FACTOR_CEPA) % Cepa.values().length];
	}

	private static Calidad calidadDe(int amplificador) {
		return new Calidad((amplificador % FACTOR_CEPA) / FACTOR_CALIDAD);
	}

	private static PerfilConsumo perfilDe(int amplificador) {
		return (amplificador % FACTOR_CALIDAD) == 1 ? PerfilConsumo.SPACE_CAKE : PerfilConsumo.BROWNIE;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duracionRestante, int amplificador) {
		return duracionRestante == 1;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entidad, int amplificador) {
		EfectosCepa.aplicarReales(entidad, cepaDe(amplificador), calidadDe(amplificador),
				perfilDe(amplificador));
		return true;
	}
}
