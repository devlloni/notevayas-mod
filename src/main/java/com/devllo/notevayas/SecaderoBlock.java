package com.devllo.notevayas;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Secadero. Click derecho con cogollo fresco = meter; con la mano vacia = sacar lo que
 * este listo.
 */
public class SecaderoBlock extends Block implements EntityBlock {
	public static final MapCodec<SecaderoBlock> CODEC = simpleCodec(SecaderoBlock::new);

	public SecaderoBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends SecaderoBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecaderoBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		if (level.isClientSide() || type != ModBlockEntities.SECADERO) {
			return null;
		}
		return (nivel, pos, estado, entidad) ->
				SecaderoBlockEntity.tick(nivel, pos, estado, (SecaderoBlockEntity) entidad);
	}

	/** Click derecho con un item en la mano: solo acepta cogollos frescos. */
	@Override
	protected InteractionResult useItemOn(ItemStack enMano, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(enMano.getItem() instanceof CogolloItem cogollo) || !cogollo.esFresco()) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(level.getBlockEntity(pos) instanceof SecaderoBlockEntity secadero)) {
			return InteractionResult.PASS;
		}

		if (!secadero.meter(enMano)) {
			return InteractionResult.CONSUME;
		}

		if (!player.getAbilities().instabuild) {
			enMano.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	/** Click derecho con la mano vacia: retira todo lo que ya se curo. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(level.getBlockEntity(pos) instanceof SecaderoBlockEntity secadero)) {
			return InteractionResult.PASS;
		}

		List<ItemStack> listos = secadero.sacarListos();
		if (listos.isEmpty()) {
			return InteractionResult.CONSUME;
		}

		for (ItemStack seco : listos) {
			if (!player.getInventory().add(seco)) {
				player.drop(seco, false);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Al romper el secadero se tira lo que tenga adentro, curado o no. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
			boolean movedByPiston) {
		if (level.getBlockEntity(pos) instanceof SecaderoBlockEntity secadero) {
			Containers.dropContents(level, pos, secadero.items());
		}
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}
}
