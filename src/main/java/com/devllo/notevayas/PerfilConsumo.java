package com.devllo.notevayas;

/**
 * Como se consume cada item: cuanto duran los efectos, si suma amplificador, si aplica
 * nausea de entrada y si tiene onset retardado.
 *
 * Todas las duraciones estan en ticks (20 ticks = 1 segundo).
 */
public enum PerfilConsumo {
	/** Porro: 45s. */
	PORRO(45 * 20, 0, 0, 0, 1.0f),
	/** Blunt: 120s. */
	BLUNT(120 * 20, 0, 0, 0, 1.0f),
	/** Bong: 25s, +1 al amplificador, y 3s de nausea al inicio. */
	BONG(25 * 20, 1, 3 * 20, 0, 1.0f),
	/** Brownie: 90s, pero no pasa nada hasta que pasan 15s. */
	BROWNIE(90 * 20, 0, 0, 15 * 20, 1.0f),
	/** Space cake: 240s, +1 al amplificador, onset de 30s. */
	SPACE_CAKE(240 * 20, 1, 0, 30 * 20, 1.0f),
	/** Cogollo fresco sin curar: efectos al 30% y nausea garantizada de 10s. */
	COGOLLO_FRESCO(45 * 20, 0, 10 * 20, 0, 0.30f);

	private final int duracionBase;
	private final int bonusAmplificador;
	private final int nauseaInicial;
	private final int onset;
	private final float factorDuracion;

	PerfilConsumo(int duracionBase, int bonusAmplificador, int nauseaInicial, int onset,
			float factorDuracion) {
		this.duracionBase = duracionBase;
		this.bonusAmplificador = bonusAmplificador;
		this.nauseaInicial = nauseaInicial;
		this.onset = onset;
		this.factorDuracion = factorDuracion;
	}

	public int duracionBase() {
		return this.duracionBase;
	}

	public int bonusAmplificador() {
		return this.bonusAmplificador;
	}

	/** Nausea aplicada al momento de consumir, independiente de los efectos reales. */
	public int nauseaInicial() {
		return this.nauseaInicial;
	}

	/** Ticks de espera antes de que arranquen los efectos. 0 = inmediato. */
	public int onset() {
		return this.onset;
	}

	public float factorDuracion() {
		return this.factorDuracion;
	}

	public boolean tieneOnsetRetardado() {
		return this.onset > 0;
	}
}
