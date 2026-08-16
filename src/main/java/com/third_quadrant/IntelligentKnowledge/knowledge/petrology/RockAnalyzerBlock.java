package com.third_quadrant.intelligentknowledge.knowledge.petrology;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// 암석 분석기 블록. 바닐라 석재절단기와 구조가 같으며,
// "암석학 학사(stone_300) 이상"이 아니면 UI를 열 수 없다.
public class RockAnalyzerBlock extends Block {
    // GUI 상단에 표시할 제목. (lang 파일에서 번역)
    private static final Component CONTAINER_TITLE = Component.translatable("container.rock_analyzer");
    // 형태: 돌 조각대처럼 낮은 테이블 모양 (16x9).
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 9.0, 16.0);

    public RockAnalyzerBlock(Properties properties) {
        super(properties);
    }

    // 우클릭 시 발생. 클라이언트는 성공으로만 응답하고, 실제 자격 검사는 서버에서만 한다.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 서버: 암석학 학사(stone_300) 달성 여부로 사용을 제한한다.
        if (!RockGates.hasBachelor((ServerPlayer) player)) {
            player.displayClientMessage(Component.literal("암석 분석기를 사용하려면 암석학 학사(돌 지식조각 300개) 자격이 필요합니다."), true);
            return InteractionResult.CONSUME;
        }

        // 자격이 있으면 바닐라 석재절단기처럼 메뉴를 연다.
        player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.CONSUME;
    }

    // 열린 메뉴의 내용물: 해당 블록의 위치(ContainerLevelAccess)를 넘겨주는 메뉴 제공자.
    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new RockAnalyzerMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos)),
                CONTAINER_TITLE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
