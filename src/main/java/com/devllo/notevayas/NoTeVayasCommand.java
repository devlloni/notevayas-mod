package com.devllo.notevayas;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public final class NoTeVayasCommand {
	// Volume also drives the audible radius for a variable-range SoundEvent:
	// 16 blocks at <= 1.0f, 16 * VOLUME above that. Raise it to make the sound carry further.
	private static final float VOLUME = 1.0f;
	private static final float PITCH = 1.0f;

	private NoTeVayasCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// No .requires(...) on purpose: every player can run this, no permission level needed.
		dispatcher.register(Commands.literal("notevayas").executes(NoTeVayasCommand::run));
	}

	private static int run(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayer();

		// Console / command block / anything without a player: fail softly.
		if (player == null) {
			source.sendFailure(Component.literal("Este comando solo lo puede ejecutar un jugador."));
			return 0;
		}

		// null as the excluded entity means the packet goes to everyone in range,
		// including the player who ran the command.
		player.level().playSound(
				null,
				player.getX(),
				player.getY(),
				player.getZ(),
				ModSounds.NO_TE_VAYAS,
				SoundSource.PLAYERS,
				VOLUME,
				PITCH
		);

		return Command.SINGLE_SUCCESS;
	}
}
