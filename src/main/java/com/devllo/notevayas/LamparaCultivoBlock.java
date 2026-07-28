package com.devllo.notevayas;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

/**
 * Lampara de cultivo: se prende con redstone y emite luz 15.
 *
 * El efecto sobre los cultivos (ignorar el requisito de luz natural y crecer al doble)
 * lo resuelve CultivoBlock mirando hacia arriba; la lampara en si no conoce a los cultivos.
 */
public class LamparaCultivoBlock extends Block {
	public static final MapCodec<LamparaCultivoBlock> CODEC = simpleCodec(LamparaCultivoBlock::new);

	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	/** Nivel de luz que emite prendida. */
	public static final int LUZ_PRENDIDA = 15;

	public LamparaCultivoBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
	}

	@Override
	public MapCodec<? extends LamparaCultivoBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean alimentada = context.getLevel().hasNeighborSignal(context.getClickedPos());
		return this.defaultBlockState().setValue(LIT, alimentada);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block vecino,
			Orientation orientacion, boolean movedByPiston) {
		if (level.isClientSide()) {
			return;
		}

		boolean alimentada = level.hasNeighborSignal(pos);
		if (alimentada != state.getValue(LIT)) {
			level.setBlock(pos, state.setValue(LIT, alimentada), Block.UPDATE_ALL);
		}
	}
}
