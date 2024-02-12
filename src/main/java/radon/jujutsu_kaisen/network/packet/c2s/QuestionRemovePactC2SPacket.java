package radon.jujutsu_kaisen.network.packet.c2s;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.sorcerer.Pact;

import java.util.UUID;

public class QuestionRemovePactC2SPacket implements CustomPacketPayload {
    public static final ResourceLocation IDENTIFIER = new ResourceLocation(JujutsuKaisen.MOD_ID, "question_remove_pact_serverbound");

    private final UUID identifier;
    private final Pact pact;

    public QuestionRemovePactC2SPacket(UUID identifier, Pact pact) {
        this.identifier = identifier;
        this.pact = pact;
    }

    public QuestionRemovePactC2SPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readEnum(Pact.class));
    }

    public void handle(PlayPayloadContext ctx) {
        ctx.workHandler().execute(() -> {
            if (!(ctx.player().orElseThrow() instanceof ServerPlayer sender)) return;

            IJujutsuCapability jujutsuCap = sender.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (jujutsuCap == null) return;

        ISorcererData data = jujutsuCap.getSorcererData();

            data.createPactRemovalRequest(this.identifier, this.pact);

            Player player = sender.serverLevel().getPlayerByUUID(this.identifier);

            if (player == sender) return;

            if (player != null) {
                Component accept = Component.translatable(String.format("chat.%s.pact_question_accept", JujutsuKaisen.MOD_ID))
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/pactremovalaccept %s %s", sender.getName().getString(), this.pact.name())))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(String.format("chat.%s.pact_question_remove", JujutsuKaisen.MOD_ID)))));
                Component decline = Component.translatable(String.format("chat.%s.pact_question_decline", JujutsuKaisen.MOD_ID))
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/pactremovaldecline %s %s", sender.getName().getString(), this.pact.name())))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(String.format("chat.%s.pact_question_remove", JujutsuKaisen.MOD_ID)))));

                Component message = Component.translatable(String.format("chat.%s.pact_question_remove", JujutsuKaisen.MOD_ID), accept, decline,
                        this.pact.getName().getString().toLowerCase(), sender.getName());

                player.sendSystemMessage(message);
            }
        });
    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUUID(this.identifier);
        pBuffer.writeEnum(this.pact);
    }

    @Override
    public @NotNull ResourceLocation id() {
        return IDENTIFIER;
    }
}