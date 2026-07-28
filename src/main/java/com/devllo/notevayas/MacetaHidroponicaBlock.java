package com.devllo.notevayas;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Maceta hidroponica. Es un bloque comun sin logica propia: los efectos (mantener el
 * farmland humedo y sumar +1 de calidad) los aplican FarmlandBlockMixin y CultivoBlock
 * respectivamente, que son quienes la buscan debajo.
 */
public class MacetaHidroponicaBlock extends Block {
	public static final MapCodec<MacetaHidroponicaBlock> CODEC = simpleCodec(MacetaHidroponicaBlock::new);

	public MacetaHidroponicaBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends MacetaHidroponicaBlock> codec() {
		return CODEC;
	}
}
