package com.devllo.notevayas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.devllo.notevayas.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Una maceta hidroponica justo debajo del farmland lo mantiene humedo para siempre.
 *
 * Hace falta un mixin porque el secado vive en FarmlandBlock.randomTick, que es vanilla:
 * no hay evento ni API de Fabric para intervenir ahi.
 */
@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {
	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	private void notevayas$macetaMantieneHumedad(BlockState state, ServerLevel level, BlockPos pos,
			RandomSource random, CallbackInfo ci) {
		if (!level.getBlockState(pos.below()).is(ModBlocks.MACETA_HIDROPONICA)) {
			return;
		}

		if (state.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE) {
			level.setBlock(pos, state.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE),
					Block.UPDATE_CLIENTS);
		}

		// Cancela el random tick vanilla, que es el que seca y convierte en tierra.
		ci.cancel();
	}
}
