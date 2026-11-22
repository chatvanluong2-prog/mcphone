package com.november.mcphone.feature.flashlight.net;

import com.november.mcphone.MCphone;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: bật / tắt đèn pin. */
public record FlashlightPacket(boolean on) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FlashlightPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(MCphone.MODID, "flashlight"));

    public static final StreamCodec<ByteBuf, FlashlightPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, FlashlightPacket::on, FlashlightPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
