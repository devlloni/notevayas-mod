package com.devllo.notevayas;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Cultivo de una cepa. Hay 4 bloques separados (uno por cepa); ver ModBlocks.
 */
public class CultivoBlock extends CropBlock {
	public static final MapCodec<CultivoBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Cepa.CODEC.fieldOf("cepa").forGetter(CultivoBlock::cepa),
					propertiesCodec()
			).apply(instance, CultivoBlock::new));

	/**
	 * Ciclo de vida completo: 11 etapas (0-10).
	 * 0-1 brote, 2 plántula, 3-4 vegetativo, 5-6 vegetativo maduro,
	 * 7 floración, 8 maduración temprana, 9 perfecta, 10 super maduro.
	 */
	public static final int MAX_AGE = 10;
	public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);

	/**
	 * Se guarda si llovio en algun momento del crecimiento, para el +1 de calidad.
	 *
	 * Va como property del BlockState y no como BlockEntity: es un unico bit, y un
	 * BlockEntity por planta seria caro en una plantacion grande. Sube el bloque de
	 * 11 a 22 estados, que es despreciable.
	 */
	public static final BooleanProperty LLOVIDO = BooleanProperty.create("llovido");

	private static final int LUZ_MINIMA_DIURNA = 9;
	private static final int LUZ_MAXIMA_NOCTURNA = 7;

	/** Cuantos bloques hacia arriba se busca una lampara prendida. */
	private static final int ALCANCE_LAMPARA = 3;

	private static final float BONUS_BIOMA = 1.5f;
	private static final float BONUS_LLUVIA = 1.25f;
	private static final float BONUS_LAMPARA = 2.0f;

	/** Chance de perder 1 nivel de calidad si crecio a la intemperie y sin ayuda. */
	private static final float PENALIZACION_SIN_ASISTENCIA = 0.30f;

	private final Cepa cepa;

	public CultivoBlock(Cepa cepa, BlockBehaviour.Properties properties) {
		super(properties);
		this.cepa = cepa;
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(this.getAgeProperty(), 0)
				.setValue(LLOVIDO, Boolean.FALSE));
	}

	public Cepa cepa() {
		return this.cepa;
	}

	@Override
	public MapCodec<? extends CultivoBlock> codec() {
		return CODEC;
	}

	@Override
	public IntegerProperty getAgeProperty() {
		return AGE;
	}

	@Override
	public int getMaxAge() {
		return MAX_AGE;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE, LLOVIDO);
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return ModItems.SEMILLAS.get(this.cepa);
	}

	/** getStateForAge de CropBlock pierde las properties extra, asi que se preserva LLOVIDO. */
	@Override
	public BlockState getStateForAge(int edad) {
		return super.getStateForAge(edad);
	}

	// ---------------------------------------------------------------- condiciones

	/** Hay una lampara de cultivo prendida hasta ALCANCE_LAMPARA bloques por encima. */
	public static boolean hayLamparaActiva(LevelReader level, BlockPos pos) {
		for (int i = 1; i <= ALCANCE_LAMPARA; i++) {
			BlockState arriba = level.getBlockState(pos.above(i));
			if (arriba.is(ModBlocks.LAMPARA_CULTIVO) && arriba.getValue(LamparaCultivoBlock.LIT)) {
				return true;
			}
		}
		return false;
	}

	/** La maceta va debajo del farmland, o sea dos bloques por debajo del cultivo. */
	public static boolean hayMacetaDebajo(LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below(2)).is(ModBlocks.MACETA_HIDROPONICA);
	}

	private boolean biomaFavorable(LevelReader level, BlockPos pos) {
		return this.cepa.biomasFavorables().stream().anyMatch(level.getBiome(pos)::is);
	}

	/**
	 * Purpura Nocturna necesita oscuridad; las otras tres, luz. Con una lampara prendida
	 * encima el requisito no se aplica para ninguna.
	 *
	 * Se usa getMaxLocalRawBrightness (luz real, considera la hora) en vez del
	 * getRawBrightness(pos, 0) del trigo vanilla, que ignora la noche a proposito: sin eso
	 * la regla de la nocturna no tendria sentido.
	 */
	private boolean luzAdecuada(LevelReader level, BlockPos pos) {
		if (hayLamparaActiva(level, pos)) {
			return true;
		}
		int luz = level.getMaxLocalRawBrightness(pos);
		return this.cepa == Cepa.PURPURA_NOCTURNA
				? luz <= LUZ_MAXIMA_NOCTURNA
				: luz >= LUZ_MINIMA_DIURNA;
	}

	// ---------------------------------------------------------------- crecimiento

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!luzAdecuada(level, pos)) {
			return;
		}

		boolean lloviendo = level.isRainingAt(pos);

		// El flag de lluvia se marca aunque la planta ya este madura: lo que importa es
		// que haya llovido durante el cultivo.
		if (lloviendo && !state.getValue(LLOVIDO)) {
			state = state.setValue(LLOVIDO, Boolean.TRUE);
			level.setBlock(pos, state, Block.UPDATE_INVISIBLE);
		}

		int edad = this.getAge(state);
		if (edad >= this.getMaxAge()) {
			return;
		}

		// Vanilla hace random.nextInt((int)(25/speed) + 1) == 0. Se calcula la probabilidad
		// explicitamente para no perder los multiplicadores en la division entera.
		float velocidad = getGrowthSpeed(this, level, pos);
		if (this.cepa == Cepa.VERDE_SOL && biomaFavorable(level, pos)) {
			velocidad *= BONUS_BIOMA;
		}
		if (hayLamparaActiva(level, pos)) {
			velocidad *= BONUS_LAMPARA;
		}

		float probabilidad = 1.0f / (25.0f / velocidad + 1.0f);
		if (lloviendo) {
			probabilidad *= BONUS_LLUVIA;
		}

		if (random.nextFloat() < probabilidad) {
			level.setBlock(pos, this.getStateForAge(edad + 1).setValue(LLOVIDO, state.getValue(LLOVIDO)),
					Block.UPDATE_CLIENTS);
		}
	}

	// ---------------------------------------------------------------- cosecha

	/**
	 * Calidad resultante segun las condiciones de cultivo. Calidad la clampea a 0-3.
	 */
	public int calcularCalidad(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		boolean lampara = hayLamparaActiva(level, pos);
		boolean maceta = hayMacetaDebajo(level, pos);

		int calidad = 0;
		if (lampara) {
			calidad++;
		}
		if (maceta) {
			calidad++;
		}
		if (biomaFavorable(level, pos)) {
			calidad++;
		}
		if (state.getValue(LLOVIDO)) {
			calidad++;
		}
		if (!lampara && !maceta && random.nextFloat() < PENALIZACION_SIN_ASISTENCIA) {
			calidad--;
		}
		return calidad;
	}

	/**
	 * La loot table JSON sigue decidiendo QUE y CUANTOS items caen; aca solo se le estampa
	 * la calidad a los cogollos, que es algo que una loot table no puede calcular porque
	 * depende de los bloques vecinos y del flag de lluvia.
	 */
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);

		ServerLevel level = params.getLevel();
		Vec3 origen = params.getOptionalParameter(LootContextParams.ORIGIN);
		if (level == null || origen == null) {
			return drops;
		}

		BlockPos pos = BlockPos.containing(origen);
		Calidad calidad = new Calidad(calcularCalidad(level, pos, state, level.getRandom()));

		for (ItemStack drop : drops) {
			if (drop.getItem() instanceof CogolloItem) {
				drop.set(ModComponents.CALIDAD, calidad);
			}
		}
		return drops;
	}
}
