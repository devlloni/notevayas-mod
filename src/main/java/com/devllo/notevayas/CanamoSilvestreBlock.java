package com.devllo.notevayas;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Canamo silvestre: la planta que aparece generada en el mundo. No se cultiva ni crece;
 * al romperla dropea semillas al azar (ver su loot table).
 */
public class CanamoSilvestreBlock extends VegetationBlock {
	public static final MapCodec<CanamoSilvestreBlock> CODEC = simpleCodec(CanamoSilvestreBlock::new);

	private static final VoxelShape FORMA = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

	public CanamoSilvestreBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends CanamoSilvestreBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return FORMA;
	}
}
