package com.third_quadrant.intelligentknowledge;

// 서버->클라이언트로 텍스트(대화/액션바 등)를 보낼 때 쓰는 마인크래프트 핵심 클래스.
// 마인크래프트는 문자열을 그대로 받지 않고 Component(채팅/문장 데이터)로 감싸서 주고받음.
// LivingEntity = 모든 생명체(플레이어, 몹)의 부모 클래스. heal(), level() 같은 공통 메서드가 여기 있음.
// 점프 이벤트는 LivingEntity 단위로 발생하므로 여기서 받는다.
import com.mojang.brigadier.arguments.IntegerArgumentType;
// 지식조각 어태치먼트(돌 지식조각 개수 저장/동기화). 학문 공통 인프라.
import com.third_quadrant.intelligentknowledge.attachment.ModAttachments;
// 암석학(돌 지식) 전용: 발전과제 기반 자격(암석학 학사/석사) 판정.
import com.third_quadrant.intelligentknowledge.knowledge.petrology.RockGates;
// 레지스트리 등록 클래스들. 블록/아이템/메뉴/크리에이티브 탭을 여기서 모드 버스에 등록한다.
import com.third_quadrant.intelligentknowledge.registry.ModBlocks;
import com.third_quadrant.intelligentknowledge.registry.ModCreativeTabs;
import com.third_quadrant.intelligentknowledge.registry.ModItems;
import com.third_quadrant.intelligentknowledge.registry.ModMenus;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
// Player = 플레이어 전용 클래스. getUUID(), displayClientMessage() 같은 플레이어 고유 기능이 있음.
// 채굴 이벤트(event.getPlayer())는 Player 타입으로 가져온다.
import net.minecraft.world.entity.player.Player;
// BlockState = "어떤 위치에 실제로 설치된 블록의 상태" (블록 종류 + 물/눈 등 추가 상태 포함).
// 채굴 이벤트에서 실제 깨진 블록을 조회할 때 BlockState로 받아온다.
import net.minecraft.world.level.block.state.BlockState;
// ItemStack = 아이템 + 개수 + 내구도(NBT 등)를 묶은 실제 스택 객체. 내구도 수리를 위해 사용한다.
import net.minecraft.world.item.ItemStack;
// IEventBus = 이벤트 버스의 인터페이스. @Mod 생성자에 주입되어 모드 이벤트 버스를 받아온다.
// (여기서는 게임 이벤트 버스인 NeoForge.EVENT_BUS에 직접 리스너를 등록하므로 주입받기만 하고 안 쓰지만,
//  네오포지의 정석적인 이벤트 등록 진입점이라 생성자 인자로 받는 게 규칙임)
import net.neoforged.bus.api.IEventBus;
// @Mod = "이 클래스가 모드의 엔트리포인트다"라는 네오포지 핵심 어노테이션.
// 게임 로딩 시 이 어노테이션을 스캔해서 아래 모드ID("intelligentknowledge")로 이 클래스를 찾아 객체를 만든다.
import net.neoforged.fml.common.Mod;
// NeoForge = 네오포지 API의 루트 클래스. EVENT_BUS(게임 이벤트 버스)가 static 필드로 들어 있음.
// 서버/클라이언트 전역 이벤트(점프, 채굴 등)는 여기 EVENT_BUS에 리스너를 등록해야 받을 수 있다.
import net.neoforged.neoforge.common.NeoForge;
// LivingEvent = 생명체 관련 이벤트들의 부모 클래스. 그 안의 내부 클래스로 LivingJumpEvent가 정의되어 있음.
// 1.21.1에서는 LivingJumpEvent가 별도 파일이 아니라 LivingEvent.LivingJumpEvent 형태로 존재한다.
import net.neoforged.neoforge.event.entity.living.LivingEvent;
// BlockEvent.BreakEvent = 플레이어가 블록을 부수려고 할 때(성공 전에) 발생하는 이벤트.
// getPlayer()로 부순 사람, getState()로 깨진 블록을 알 수 있어 "돌을 캤을 때"를 감지하는 데 쓴다.
import net.neoforged.neoforge.event.level.BlockEvent;
// RegisterCommandsEvent = 서버가 채팅 명령어를 등록할 때 발생. 테스트용 명령어를 여기에 추가한다.
import net.neoforged.neoforge.event.RegisterCommandsEvent;
// PlayerEvent = 플레이어 관련 이벤트 부모 클래스.
// BreakSpeed(채굴 속도 변경, cancel 가능), PlayerLoggedIn(접속 시) 등이 여기 정의되어 있다.
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import static com.third_quadrant.intelligentknowledge.util.Chance.chance;
import static com.third_quadrant.intelligentknowledge.util.Chance.chanceBlock;

// 모드ID를 선언한 @Mod. gradle.properties의 mod_id와 반드시 일치해야 로더가 매칭해서 로드한다.
@Mod("intelligentknowledge")
public class IntelligentKnowledge {

    // @Mod 클래스의 생성자. 네오포지가 모드 객체를 만들 때 호출하며, IEventBus를 인자로 주입해준다.
    // 이벤트 리스너 등록은 여기서 해야 "모드 로딩 후 바로" 이벤트를 받을 수 있다.
    public IntelligentKnowledge(IEventBus modBus) {
        // 어태치먼트 타입(STONE_COUNT)을 네오포지 레지스트리에 등록한다.
        // 등록 전에는 player.getData()로 값을 조회할 수 없으므로 반드시 최우선으로 실행해야 한다.
        ModAttachments.register(modBus);

        // 암석 분석기 관련 레지스트리(블록/아이템/메뉴/크리에이티브 탭)를 등록한다.
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModMenus.register(modBus);
        ModCreativeTabs.register(modBus);

        // NeoForge.EVENT_BUS: 서버+클라이언트 공용 게임 이벤트 버스.
        // addListener(메서드 참조)로 해당 메서드를 이벤트 버스에 구독시킨다.
        NeoForge.EVENT_BUS.addListener(IntelligentKnowledge::onLivingJump);
        // 블록 채굴 이벤트도 같은 버스에 구독. 이벤트 타입은 파라미터 시그니처로 자동 매칭된다.
        NeoForge.EVENT_BUS.addListener(IntelligentKnowledge::onBlockBreak);
        // 채굴 속도 이벤트(암석학 석사 보너스)와 접속 이벤트(자격증 지급)도 같은 버스에 구독.
        NeoForge.EVENT_BUS.addListener(IntelligentKnowledge::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(IntelligentKnowledge::onPlayerLoggedIn);
        // 테스트용 명령어 등록도 같은 버스에 구독.
        NeoForge.EVENT_BUS.addListener(IntelligentKnowledge::onRegisterCommands);
    }

    // 점프 이벤트 리스너. LivingJumpEvent는 생명체가 점프할 때 발생한다.
    private static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        // event.getEntity(): 점프한 생명체(플레이어/몹). LivingEntity로 받아 heal() 등을 쓸 수 있다.
        LivingEntity entity = event.getEntity();

        // isClientSide()가 true = "지금 실행 중인 게임이 클라이언트 쪽".
        // 클라이언트에서도 이벤트가 중복 발생하므로, 실제 게임 로직은 서버(논리적)에서만 실행해야 한다.
        // 서버에서 실행된 결과는 자동으로 클라이언트에 동기화된다. (= 중복 처리 방지)
        if (!entity.level().isClientSide()) {
            // heal(1): 해당 생명체의 체력을 1 회복시킨다. (Entity가 아닌 LivingEntity에만 존재)
            entity.heal(1);
        }
    }

    // 블록 채굴 이벤트 리스너. BreakEvent는 블록이 실제로 부서지기 직전에 발생한다.
    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        // event.getPlayer(): 블록을 부순 플레이어. Player 타입이므로 UUID·메시지 전송 기능 사용 가능.
        Player player = event.getPlayer();

        // 서버에서만 카운트를 올려야 하므로 클라이언트 쪽 이벤트는 무시하고 끝낸다.
        // (같은 판정이 서버/클라이언트 두 번 발생 → 카운트가 2배로 오르는 것 방지)
        if (player.level().isClientSide()) {
            return;
        }

        // event.getState(): 부서진 블록의 실제 상태. getBlock()으로 블록 종류를 얻는다.
        BlockState state = event.getState();
        // 돌 목록(STONE_BLOCKS)에 없으면 이 리스너의 관심 대상이 아니므로 무시.
        if (!state.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return;
        }

        // [암석학 석사(stone_500) 이상 보너스] 암석 채굴 시 50% 확률로 내구도 소모를 없앤다.
        // BreakEvent는 실제 내구도 감소(mineBlock)보다 먼저 발생하므로,
        // "먼저 1 수리해두면 이어지는 감소와 상쇄"되어 결과적으로 내구도가 깎이지 않는다. (방패 방식)
        if (player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD) >= 500) {
            ItemStack tool = player.getMainHandItem();
            if (tool.isDamageableItem() && tool.getDamageValue() > 0
                    && !player.getAbilities().instabuild && chance(player.getRandom(), 50)) {
                tool.setDamageValue(tool.getDamageValue() - 1);
            }
        }

        RandomSource random = player.getRandom();
        int stone_shard = player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD);

        if (chanceBlock(random, stone_shard)) {
            stone_shard++;
            player.setData(ModAttachments.STONE_KNOWLEDGE_SHARD, stone_shard);

            // 마일스톤(100/300/500/1000) 도달 시 해당 발전과제를 달성시켜 바닐라 토스트 UI로 표시한다.
            // (이벤트가 서버에서만 실행되므로 ServerPlayer로 캐스팅하는 것이 안전하다.)
            grantIfMilestone((ServerPlayer) player, stone_shard);
        }
    }

    // [암석학 석사(stone_500) 이상 보너스] 암석 채굴 속도를 25% 증가시킨다.
    // BreakSpeed는 서버(실제 채굴 판정)와 클라이언트(진행도 예측) 양쪽에서 발생한다.
    // 어태치먼트는 양쪽 모두 동기화되므로 500 이상 판정으로 석사 여부를 확인할 수 있다.
    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        // 돌 목록이 아니면 보너스를 적용하지 않는다.
        if (!event.getState().is(BlockTags.BASE_STONE_OVERWORLD)) {
            return;
        }

        Player player = event.getEntity();
        if (player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD) >= 500) {
            event.setNewSpeed(event.getNewSpeed() * 1.25F);
        }
    }

    // 접속 시점에 암석학 학사(stone_300) 이상인데 자격증이 없으면 지급한다.
    // (발전과제 보상으로 받은 뒤 조합에서 소모해도, 재접속하면 다시 받을 수 있도록 함)
    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (RockGates.hasBachelor(player)
                && !player.getInventory().contains(stack -> stack.is(ModItems.GEOLOGIST_CERTIFICATE.get()))) {
            player.getInventory().add(new ItemStack(ModItems.GEOLOGIST_CERTIFICATE.get()));
        }
    }

    // 마일스톤(100/300/500/1000)에 정확히 도달했을 때만 발전과제를 달성시킨다.
    private static void grantIfMilestone(ServerPlayer player, int shardCount) {
        if (shardCount == 100 || shardCount == 300 || shardCount == 500 || shardCount == 1000) {
            grantKnowledgeAdvancement(player, shardCount);
        }
    }

    // 테스트용 명령어. 채팅창에 입력하면 바로 지식조각 개수를 조절할 수 있다.
    // /stone set <개수>  → 개수를 정확히 <개수>개로 설정 (마일스톤이면 발전과제도 바로 달성)
    // /stone add <개수>  → 개수를 <개수>개만큼 추가
    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stone")
                .then(Commands.literal("set").then(Commands.argument("count", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int count = IntegerArgumentType.getInteger(context, "count");
                            player.setData(ModAttachments.STONE_KNOWLEDGE_SHARD, count);
                            grantIfMilestone(player, count);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("돌 지식조각: " + count + "개"), false);
                            return 1;
                        })))
                .then(Commands.literal("add").then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int count = IntegerArgumentType.getInteger(context, "count");
                            int newCount = player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD) + count;
                            player.setData(ModAttachments.STONE_KNOWLEDGE_SHARD, newCount);
                            grantIfMilestone(player, newCount);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("돌 지식조각: " + newCount + "개"), false);
                            return 1;
                        }))));
    }

    // 지식 조각 발전과제를 서버에 달성시킨다. 달성 시 클라이언트에 자동으로 발전과제 토스트가 표시된다.
    private static void grantKnowledgeAdvancement(ServerPlayer player, int shardCount) {
        // 발전과제 JSON 파일(id: intelligentknowledge:knowledge/stone_<개수>)의 리소스 키를 만든다.
        ResourceKey<Advancement> key = ResourceKey.create(Registries.ADVANCEMENT,
                ResourceLocation.fromNamespaceAndPath("intelligentknowledge", "knowledge/stone_" + shardCount));

        // 서버의 발전과제 매니저에서 해당 발전과제를 찾는다. (JSON이 로드되지 않았으면 null)
        AdvancementHolder holder = player.server.getAdvancements().get(key.location());
        if (holder != null) {
            // JSON에 정의한 크라이테리온 이름("count")을 달성시킨다.
            player.getAdvancements().award(holder, "count");
        }
    }
}
