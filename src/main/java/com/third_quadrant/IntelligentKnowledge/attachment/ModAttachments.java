package com.third_quadrant.intelligentknowledge.attachment;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

    // 책별 독서 진행도. Map<String, Integer>: 책 ID → 해당 책에서 획득한 지식 조각 수.
    // 재접속·재시작 후에도 유지, 클라이언트에 동기화됨 (아이템 Lore 표시용).
    // 주의: Codec.unboundedMap은 Guava ImmutableMap을 반환하므로 xmap으로 HashMap을 보장한다.
    // StreamCodec도 Map 타입을 사용하여 불변 맵 캐스팅 ClassCastException을 방지한다.
    public static final Supplier<AttachmentType<Map<String, Integer>>> BOOK_READ_PROGRESS =
            ATTACHMENT_TYPES.register("book_read_progress",
                    () -> AttachmentType.<Map<String, Integer>>builder(() -> new HashMap<String, Integer>())
                            .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT)
                                    .xmap(HashMap::new, Function.identity()))
                            .sync(new StreamCodec<RegistryFriendlyByteBuf, Map<String, Integer>>() {
                                @Override
                                public Map<String, Integer> decode(RegistryFriendlyByteBuf buf) {
                                    int size = buf.readInt();
                                    Map<String, Integer> map = new HashMap<>();
                                    for (int i = 0; i < size; i++) {
                                        map.put(buf.readUtf(), buf.readInt());
                                    }
                                    return map;
                                }

                                @Override
                                public void encode(RegistryFriendlyByteBuf buf, Map<String, Integer> map) {
                                    buf.writeInt(map.size());
                                    for (var entry : map.entrySet()) {
                                        buf.writeUtf(entry.getKey());
                                        buf.writeInt(entry.getValue());
                                    }
                                }
                            })
                            .build());

    // 책별 공부 시작 시점의 플레이어 지식 조각 스냅샷.
    // Map<String, Integer>: 책 ID → 공부 시작 시의 지식 조각 수.
    // 스냅샷 < 책 난이도이면 해당 책에서 최적 보상 보장 (지식 상승에도 페널티 없음).
    public static final Supplier<AttachmentType<Map<String, Integer>>> BOOK_SNAPSHOT_KNOWLEDGE =
            ATTACHMENT_TYPES.register("book_snapshot_knowledge",
                    () -> AttachmentType.<Map<String, Integer>>builder(() -> new HashMap<String, Integer>())
                            .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT)
                                    .xmap(HashMap::new, Function.identity()))
                            .sync(new StreamCodec<RegistryFriendlyByteBuf, Map<String, Integer>>() {
                                @Override
                                public Map<String, Integer> decode(RegistryFriendlyByteBuf buf) {
                                    int size = buf.readInt();
                                    Map<String, Integer> map = new HashMap<>();
                                    for (int i = 0; i < size; i++) {
                                        map.put(buf.readUtf(), buf.readInt());
                                    }
                                    return map;
                                }

                                @Override
                                public void encode(RegistryFriendlyByteBuf buf, Map<String, Integer> map) {
                                    buf.writeInt(map.size());
                                    for (var entry : map.entrySet()) {
                                        buf.writeUtf(entry.getKey());
                                        buf.writeInt(entry.getValue());
                                    }
                                }
                            })
                            .build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
