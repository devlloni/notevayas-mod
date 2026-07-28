package com.devllo.notevayas;

import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Las 4 cepas del mod. El id se usa como sufijo de todos los items/bloques
 * derivados y como nombre de textura, asi que tiene que coincidir exactamente
 * con los archivos de assets.
 */
public enum Cepa implements StringRepresentable {
	PIEDRA_ROJA("piedra_roja"),
	VERDE_SOL("verde_sol"),
	MEDIA_LUNA("media_luna"),
	PURPURA_NOCTURNA("purpura_nocturna");

	public static final StringRepresentable.EnumCodec<Cepa> CODEC =
			StringRepresentable.fromEnum(Cepa::values);

	public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, Cepa> STREAM_CODEC =
			net.minecraft.network.codec.ByteBufCodecs.idMapper(i -> values()[i], Cepa::ordinal);

	private final String id;

	Cepa(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	@Override
	public String getSerializedName() {
		return this.id;
	}

	public String cultivoId() {
		return "cultivo_" + this.id;
	}

	/**
	 * Biomas donde la cepa se da bien: dan +1 de calidad al cosechar, y en el caso de
	 * Verde Sol tambien el 1.5x de velocidad de crecimiento.
	 *
	 * OJO: los biomas de Verde Sol son los que pediste. Los de las otras tres cepas los
	 * invente yo (tematicos, no estaban en la spec) porque el calculo de calidad los
	 * necesita. Cambialos a gusto.
	 */
	public Set<ResourceKey<Biome>> biomasFavorables() {
		return switch (this) {
			case VERDE_SOL -> Set.of(Biomes.SAVANNA, Biomes.PLAINS, Biomes.DESERT);
			case PIEDRA_ROJA -> Set.of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WINDSWEPT_HILLS);
			case MEDIA_LUNA -> Set.of(Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.MEADOW);
			case PURPURA_NOCTURNA -> Set.of(Biomes.DARK_FOREST, Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
		};
	}

	public String semillaId() {
		return "semilla_" + this.id;
	}

	public String cogolloFrescoId() {
		return "cogollo_fresco_" + this.id;
	}

	public String cogolloId() {
		return "cogollo_" + this.id;
	}
}
