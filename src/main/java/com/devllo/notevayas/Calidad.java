package com.devllo.notevayas;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * Calidad de un cogollo, valor del data component {@link ModComponents#CALIDAD}.
 *
 * Se serializa como un entero pelado 0-3 (ver CODEC), pero se modela como record
 * para poder implementar TooltipProvider: asi la linea de calidad del tooltip la
 * pone el propio component y no hace falta pisar Item.appendHoverText, que en 26.2
 * esta deprecado.
 */
public record Calidad(int nivel) implements TooltipProvider {
	public static final int PRENSADO = 0;
	public static final int REGULAR = 1;
	public static final int PREMIUM = 2;
	public static final int ELITE = 3;

	public static final int MIN = PRENSADO;
	public static final int MAX = ELITE;

	public static final Codec<Calidad> CODEC =
			Codec.intRange(MIN, MAX).xmap(Calidad::new, Calidad::nivel);

	public static final StreamCodec<io.netty.buffer.ByteBuf, Calidad> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(Calidad::new, Calidad::nivel);

	private static final String[] LANG_KEYS = {
			"calidad.notevayas.prensado",
			"calidad.notevayas.regular",
			"calidad.notevayas.premium",
			"calidad.notevayas.elite"
	};

	private static final ChatFormatting[] COLORES = {
			ChatFormatting.GRAY,
			ChatFormatting.WHITE,
			ChatFormatting.GREEN,
			ChatFormatting.GOLD
	};

	/** Clampea en construccion: el calculo de la Fase 3 suma y resta niveles libremente. */
	public Calidad {
		nivel = Math.clamp(nivel, MIN, MAX);
	}

	public static int clamp(int nivel) {
		return Math.clamp(nivel, MIN, MAX);
	}

	public String langKey() {
		return LANG_KEYS[this.nivel];
	}

	public ChatFormatting color() {
		return COLORES[this.nivel];
	}

	/** Multiplicador de duracion de efectos (Fase 5). */
	public float multiplicadorDuracion() {
		return switch (this.nivel) {
			case PRENSADO -> 0.5f;
			case PREMIUM -> 1.5f;
			case ELITE -> 2.0f;
			default -> 1.0f;
		};
	}

	/** La calidad Elite suma +1 al amplificador de los efectos (Fase 5). */
	public int bonusAmplificador() {
		return this.nivel == ELITE ? 1 : 0;
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> adder,
			TooltipFlag flag, DataComponentGetter getter) {
		adder.accept(Component.translatable(langKey()).withStyle(color()));
	}
}
