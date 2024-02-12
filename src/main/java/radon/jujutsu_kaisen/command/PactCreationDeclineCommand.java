package radon.jujutsu_kaisen.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.command.EnumArgument;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.sorcerer.Pact;

public class PactCreationDeclineCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(Commands.literal("pactcreationdecline")
                .then(Commands.argument("player", EntityArgument.entity())
                        .then(Commands.argument("pact", EnumArgument.enumArgument(Pact.class))
                                .executes(ctx -> decline(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ctx.getArgument("pact", Pact.class))))));

        dispatcher.register(Commands.literal("pactcreationdecline").redirect(node));
    }

    public static int decline(CommandSourceStack stack, ServerPlayer dst, Pact pact) {
        ServerPlayer src = stack.getPlayer();

        if (src == null) return 0;

        IJujutsuCapability jujutsuCap = dst.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (jujutsuCap == null) return 0;

        ISorcererData dstData = jujutsuCap.getSorcererData();

        if (dstData == null) return 0;

        if (dstData.hasRequestedPactCreation(src.getUUID(), pact)) {
            dstData.removePactCreationRequest(src.getUUID(), pact);

            dst.sendSystemMessage(Component.translatable(String.format("chat.%s.pact_creation_decline", JujutsuKaisen.MOD_ID), src.getName(), pact.getName().getString().toLowerCase()));
        } else {
            src.sendSystemMessage(Component.translatable(String.format("chat.%s.pact_creation_decline", JujutsuKaisen.MOD_ID), dst.getName(), pact.getName().getString().toLowerCase()));
            return 0;
        }
        return 1;
    }
}
