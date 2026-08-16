package com.third_quadrant.intelligentknowledge.registry;

import com.third_quadrant.intelligentknowledge.knowledge.petrology.RockAnalyzerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    private static final String MOD_ID = "intelligentknowledge";

    // createBlocks(): 블록 전용 DeferredRegister. 등록 이름에 modid가 자동으로 붙는다.
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);

    // 암석 분석기 블록. 경도 3.5(바닐라 석재절단기와 동일), 돌 소리.
    public static final DeferredBlock<RockAnalyzerBlock> ROCK_ANALYZER =
            BLOCKS.register("rock_analyzer", () -> new RockAnalyzerBlock(
                    BlockBehaviour.Properties.of()
                            .strength(3.5F)
                            .sound(SoundType.STONE)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
