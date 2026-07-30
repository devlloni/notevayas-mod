package com.devllo.notevayas.client;

import com.devllo.notevayas.Cepa;
import com.devllo.notevayas.ModBlocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class NoTeVayasClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Los modelos 3D de las plantas usan texturas con transparencia;
		// sin esto el fondo se renderiza opaco.
		for (Cepa cepa : Cepa.values()) {
			BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CULTIVOS.get(cepa), RenderType.cutout());
		}
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CANAMO_SILVESTRE, RenderType.cutout());
	}
}