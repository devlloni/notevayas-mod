package com.devllo.notevayas;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bong. Se carga con un balde de agua (4 usos) y se usa con un cogollo seco.
 */
public class BongBlock extends Block {
	public static final MapCodec<BongBlock> CODEC = simpleCodec(BongBlock::new);

	/** Usos de agua restantes. */
	public static final int USOS_POR_BALDE = 4;
	public static final IntegerProperty AGUA = IntegerProperty.create("agua", 0, USOS_POR_BALDE);

	private static final VoxelShape FORMA = Block.box(5.0, 0.0, 5.0, 11.0, 12.0, 11.0);

	public BongBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(AGUA, 0));
	}

	@Override
	public MapCodec<? extends BongBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGUA);
	}

	@Override
	protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
			BlockPos pos, CollisionContext context) {
		return FORMA;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack enMano, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		// Balde de agua: recarga a tope.
		if (enMano.is(Items.WATER_BUCKET)) {
			if (state.getValue(AGUA) >= USOS_POR_BALDE) {
				return InteractionResult.PASS;
			}
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			level.setBlock(pos, state.setValue(AGUA, USOS_POR_BALDE), Block.UPDATE_ALL);
			if (!player.getAbilities().instabuild) {
				player.setItemInHand(hand, new ItemStack(Items.BUCKET));
			}
			level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
			return InteractionResult.SUCCESS;
		}

		// Cogollo seco: se fuma.
		if (!(enMano.getItem() instanceof CogolloItem cogollo) || cogollo.esFresco()) {
			return InteractionResult.PASS;
		}

		if (state.getValue(AGUA) <= 0) {
			// El segundo parametro true manda el mensaje a la action bar.
			if (player instanceof ServerPlayer servidor) {
				servidor.sendSystemMessage(Component.translatable("mensaje.notevayas.bong_sin_agua"), true);
			}
			return InteractionResult.CONSUME;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		EfectosCepa.consumir(player, cogollo.cepa(), CogolloItem.calidadDe(enMano), PerfilConsumo.BONG);

		level.setBlock(pos, state.setValue(AGUA, state.getValue(AGUA) - 1), Block.UPDATE_ALL);
		if (!player.getAbilities().instabuild) {
			enMano.shrink(1);
		}
		level.playSound(null, pos, SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
		return InteractionResult.SUCCESS;
	}
}
