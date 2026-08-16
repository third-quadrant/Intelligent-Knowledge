package com.third_quadrant.intelligentknowledge.attachment;

import net.minecraft.network.codec.ByteBufCodecs;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    private static final String MOD_ID = "intelligentknowledge";

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    public static final Supplier<AttachmentType<Integer>> STONE_KNOWLEDGE_SHARD =
            ATTACHMENT_TYPES.register("stone_knowledge_shard",
                    () -> AttachmentType.builder(() -> 0)
                            // serialize(Codec.INT): 디스크에 저장(재접속·재시작 후에도 유지). sync: 클라이언트 HUD에도 전송.
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
