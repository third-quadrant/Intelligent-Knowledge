# Intelligent Knowledge (지적 지식)

> 마인크래프트 NeoForge 모드 — 학문 기반 지식 시스템

플레이어가 돌을 캐고, 책을 읽고, 지식을 쌓아가며 학위를 취득하는 시스템.

---

## 기술 스택

| 항목 | 버전 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| NeoForge ModDev Gradle | 2.0.144 |
| Java | 21 |
| Parchment Mappings | 2024.11.17 |

## 모드 기본 정보

| 항목 | 값 |
|---|---|
| Mod ID | `intelligentknowledge` |
| Package | `com.third_quadrant.intelligentknowledge` |
| Version | 1.0.0 |

---

## 목차

1. [NEOFORGE 개념 사전](#1-neoforge-개념-사전)
2. [프로젝트 구조](#2-프로젝트-구조)
3. [모든 클래스/파일 설명](#3-모든-클래스파일-설명)
4. [핵심 시스템 상세](#4-핵심-시스템-상세)
5. [게임 수치 밸런스](#5-게임-수치-밸런스)
6. [명령어](#6-명령어)
7. [빌드 및 실행](#7-빌드-및-실행)
8. [코드 컨벤션](#8-코드-컨벤션)
9. [향후 확장 계획](#9-향후-확장-계획)

---

## 1. NEOFORGE 개념 사전

> **이 섹션은 네오포지를 모르는 자바 개발자를 위한 것입니다.**

### 1.1 NeoForge란?

마인크래프트의 코드를 수정해서 새로운 기능을 추가하는 프레임워크. 원래 마인크래프트 코드를 직접 건드리는 대신, 네오포지가 제공하는 **API**를 통해 모듈 방식으로 기능을 넣는 방식.

### 1.2 @Mod 어노테이션

```java
@Mod("intelligentknowledge")
public class IntelligentKnowledge { ... }
```

`@Mod("모드ID")`가 달린 클래스가 모드의 **진입점(Entry Point)**. 네오포지가 게임을 시작할 때 이 클래스의 **생성자**를 호출해서 모드를 초기화한다. 마치 자바의 `main()` 메서드와 비슷하다고 생각하면 된다.

### 1.3 IEventBus (이벤트 버스)

네오포지의 핵심 개념. **"이벤트 버스"** 는 메시지 전달 시스템과 같다.

```
[이벤트 발생] → [이벤트 버스] → [리스너에게 전달]
```

마인크래프트에서 일어나는 모든 일(블록 파괴, 플레이어 접속, 점프 등)이 **이벤트**로 발생하고, 우리가 **리스너(Listener)** 를 등록해두면 그 이벤트를 받을 수 있다.

**비유:** 신문 구독과 같다. 신문사(이벤트 발생)가 신문을 발행하면, 구독자(리스너)에게 배달된다. 구독하지 않으면 신문을 받을 수 없다.

```java
// 등록: "점프 이벤트가 발생하면 onLivingJump 메서드를 호출해줘"
NeoForge.EVENT_BUS.addListener(IntelligentKnowledge::onLivingJump);

// 리스너: 실제로 처리할 메서드
private static void onLivingJump(LivingEvent.LivingJumpEvent event) {
    event.getEntity().heal(1);  // 점프할 때 체력1 회복
}
```

**이벤트 버스의 종류:**
- `NeoForge.EVENT_BUS`: 게임 이벤트 (블록 파괴, 엔티티 데미지 등)
- `modBus`: 모드 생명주기 이벤트 (레지스트리 등록, 데이터 생성 등)

### 1.4 DeferredRegister (지연 레지스트리)

마인크래프트의 모든 블록, 아이템, 메뉴는 **레지스트리**에 등록되어야 한다. DeferredRegister는 "나중에 한꺼번에 등록할게"라는 패턴.

**왜 등록이 필요한가?** 마인크래프트는 블록/아이템을 고유 ID로 관리. 등록하지 않으면 게임이 해당 요소를 모름.

```java
// 1단계: 레지스트리 객체 생성
DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

// 2단계: 아이템 등록 (아직 실제 등록 안 됨, 예약만)
DeferredItem<Item> MY_ITEM = ITEMS.register("my_item",
    () -> new Item(new Item.Properties()));

// 3단계: 모드 버스에 등록 (이때 실제로 등록됨)
ITEMS.register(modBus);
```

**용어 정리:**
- **DeferredItem**: 아직 등록되지 않은 아이템의 "예약증". 실제 `Item` 객체는 게임 시작 시 생성됨
- **모드 버스(modBus)**: 모드 생명주기 이벤트를 처리하는 별도의 이벤트 버스

### 1.5 DataSlot (데이터 슬롯)

서버와 클라이언트 사이에서 **숫자 하나**를 동기화하는 메커니즘. 메뉴(인벤토리 창) 안에서 서버의 값을 클라이언트가 실시간으로 읽을 때 사용.

```
서버: dsProgress.set(50)  →  클라이언트: dsProgress.get() == 50
```

### 1.6 Attachment (어태치먼트)

**플레이어별 커스텀 데이터**를 저장하는 네오포지 시스템. 마인크래프트 1.21.1에서 기존 NBT 방식을 대체.

```java
// 저장
player.setData(ModAttachments.STONE_KNOWLEDGE_SHARD, 100);

// 읽기
int shards = player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD);
```

- `serialize(Codec.INT)`: 디스크에 저장 (서버 재시작 후 유지). `Codec`은 데이터를 바이트로 변환/복원하는 규칙
- `sync(ByteBufCodecs.VAR_INT)`: 서버→클라이언트 동기화 (HUD 표시용). `ByteBuf`는 네트워크 전송용 바이트 배열

**DataSlot vs Attachment 사용 시점:**
- **DataSlot**: 메뉴(UI) 안에서만 동기화. 메뉴가 닫히면 값 소멸
- **Attachment**: 서버 전체에서 영구 저장. 재접속해도 유지

### 1.7 DataComponent (데이터 컴포넌트)

마인크래프트 1.21.1의 아이템 커스텀 데이터 시스템. 기존 NBT를 대체하며, 아이템에 **구조화된 데이터**를 붙일 때 사용.

**기존 NBT와의 차이:**
- NBT: `tag.put("pages", 5)` — 키 이름이 문자열이라 실수하기 쉬움
- DataComponent: `stack.set(NOTE_PAGES, 5)` — 타입 안전, 컴파일 시점 검증

```java
// 정의 (ModDataComponents.java)
DataComponentType<Integer> NOTE_PAGES = ...;

// 사용
stack.set(ModDataComponents.NOTE_PAGES.get(), 5);  // 페이지5 설정
int pages = stack.get(ModDataComponents.NOTE_PAGES.get());  // 읽기
```

**이 모드에서 사용하는 DataComponent:**
- `random_book_data`: 랜덤 책 확인 후 저장된 책 ID
- `note_pages`: 메모장에 기록된 페이지 수 (0~200)

### 1.8 메뉴와 스크린 (컨테이너 패턴)

마인크래프트의 UI는 **서버-클라이언트 분리** 구조:

```
[서버] KnowledgeBookMenu (데이터 관리)
   ↕ DataSlots로 동기화
[클라이언트] KnowledgeBookScreen (화면 렌더링)
```

- **Menu (메뉴)**: 서버에서 실행. 실제 데이터를 관리하고 버튼 입력을 처리.
- **Screen (스크린)**: 클라이언트에서 실행. 화면에 UI를 그림.
- `DeferredRegister<MenuType<?>>`로 메뉴 타입을 등록하고, `RegisterMenuScreensEvent`로 스크린과 연결.

**왜 분리하는가?** 마인크래프트는 서버와 클라이언트가 물리적으로 다름 (멀티플레이어). 서버의 데이터를 클라이언트가 화면에 그려야 하므로, DataSlot을 통해 숫자/상태를 동기화.

**실제 동작 흐름:**
1. 플레이어가 아이템 우클릭 → 서버에서 `KnowledgeBookMenu` 생성
2. 서버가 `DataSlot`에 값 설정 → 클라이언트에 자동 전송
3. 클라이언트가 `KnowledgeBookScreen.renderBg()`에서 DataSlot 값 읽어 화면에 표시
4. 플레이어가 버튼 클릭 → 클라이언트가 서버에 패킷 전송 → 서버가 처리

### 1.9 Dist.CLIENT

네오포지에서 **클라이언트 전용** 코드를 구분하는 어노테이션.

```java
@EventBusSubscriber(modid = "intelligentknowledge", value = Dist.CLIENT)
```

**왜 필요한가?** 마인크래프트는 서버와 클라이언트가 다름. 서버에는 화면이 없으므로 HUD/GUI 코드가 로드되면 오류 발생. `Dist.CLIENT`로 지정하면 서버에서 해당 클래스를 아예 로딩하지 않음.

**Dist 종류:**
- `Dist.CLIENT`: 클라이언트에서만 실행 (화면, UI, 사운드 등)
- `Dist.DEDICATED_SERVER`: 전용 서버에서만 실행
- 지정하지 않으면 양쪽에서 모두 실행

---

## 2. 프로젝트 구조

```
Intelligent-Knowledge/
├── build.gradle                              # 빌드 설정
├── gradle.properties                         # 버전/모드 속성
├── settings.gradle                           # Gradle 플러그인 설정
├── gradlew / gradlew.bat                     # Gradle 래퍼
│
└── src/main/
    ├── java/com/third_quadrant/intelligentknowledge/
    │   ├── IntelligentKnowledge.java         # @Mod 엔트리포인트 (26개 클래스 중 가장 중요)
    │   │
    │   ├── attachment/                        # 플레이어 데이터 저장
    │   │   └── ModAttachments.java           # 3개의 AttachmentType 정의
    │   │
    │   ├── client/                           # 클라이언트 전용 (서버에서 로딩 안 됨)
    │   │   ├── ClientModEvents.java          # 메뉴↔스크린 바인딩
    │   │   └── gui/
    │   │       ├── ClientKnowledgeHud.java   # 화면 우하단 지식조각 HUD
    │   │       ├── KnowledgeBookScreen.java  # 지식 책 읽기 UI
    │   │       └── RockAnalyzerScreen.java   # 암석 분석기 UI
    │   │
    │   ├── item/                             # 커스텀 아이템 클래스
    │   │   └── NoteItem.java                 # 메모장 (페이지 추적)
    │   │
    │   ├── knowledge/                        # 지식 시스템 공통
    │   │   └── common/
    │   │       ├── BookType.java             # 책 유형 열거형 (10종)
    │   │       ├── BookRarity.java           # 책 희귀도 열거형 (5등급)
    │   │       ├── KnowledgeTier.java        # 지식 단계/보상/시간 계산 엔진
    │   │       ├── KnowledgeBookDefinition.java  # 책 정의 (record)
    │   │       ├── KnowledgeRegistry.java    # 동적 책 레지스트리
    │   │       ├── KnowledgeBookItem.java    # 책 아이템 기본 클래스
    │   │       ├── RandomKnowledgeBookItem.java  # 랜덤 미확인 책
    │   │       ├── KnowledgeBookMenu.java    # 공부 메뉴 (타이머/보상 관리)
    │   │       └── LecternHandler.java       # 독서대 인터랙션
    │   │
    │   ├── knowledge/petrology/              # 암석학 학문 모듈
    │   │   ├── RockAnalyzerBlock.java        # 암석 분석기 블록
    │   │   ├── RockAnalyzerMenu.java         # 암석 분석기 메뉴
    │   │   ├── RockGates.java                # 발전과제 기반 자격 판정
    │   │   └── RockTypes.java                # 암석 목록/변환 로직
    │   │
    │   ├── registry/                         # 레지스트리 정의
    │   │   ├── ModBlocks.java                # 블록 (1종)
    │   │   ├── ModItems.java                 # 아이템 (10종)
    │   │   ├── ModMenus.java                 # 메뉴 타입 (2종)
    │   │   ├── ModCreativeTabs.java          # 크리에이티브 탭 (1종)
    │   │   └── ModDataComponents.java        # 커스텀 데이터 컴포넌트 (2종)
    │   │
    │   └── util/
    │       └── Chance.java                   # 확률 유틸리티
    │
    ├── templates/META-INF/
    │   └── neoforge.mods.toml               # 모드 디스크립터
    │
    └── resources/
        ├── assets/intelligentknowledge/
        │   ├── lang/                         # 다국어 (ko_kr, en_us)
        │   ├── blockstates/                  # 블록 스테이트
        │   ├── models/block/                 # 블록 모델 (1종)
        │   ├── models/item/                  # 아이템 모델 (10종)
        │   └── textures/                     # 텍스처 (10종 PNG)
        │
        └── data/intelligentknowledge/
            ├── recipe/                       # 조합법 (2종)
            ├── loot_table/                   # 루트 테이블 (2종)
            ├── advancement/knowledge/        # 발전과제 (4종)
            └── (via data/minecraft/tags/)     # 태그 (4종)
```

---

## 3. 모든 클래스/파일 설명

### 3.1 엔트리포인트

#### `IntelligentKnowledge.java` — 모드 심장

`@Mod("intelligentknowledge")` 어노테이션이 달린 메인 클래스.

**생성자** — 모드 초기화:
1. `ModAttachments.register()` — 어태치먼트 타입 등록 (최우선)
2. `ModDataComponents.register()` — 데이터 컴포넌트 등록
3. `ModBlocks.register()` — 블록 등록
4. `ModItems.register()` — 아이템 등록
5. `ModMenus.register()` — 메뉴 타입 등록
6. `ModCreativeTabs.register()` — 크리에이티브 탭 등록
7. 이벤트 리스너8개 `NeoForge.EVENT_BUS`에 등록

**이벤트 리스너:**

| 메서드 | 이벤트 | 역할 |
|---|---|---|
| `onServerTick` | `ServerTickEvent.Pre` | 매 틱마다 `KnowledgeBookMenu.tick()` 호출 → 공부 타이머 갱신 |
| `onLivingJump` | `LivingJumpEvent` | 점프 시 체력1 회복 (서버만) |
| `onBlockBreak` | `BlockEvent.BreakEvent` | 돌 채굴 → 확률적으로 지식조각 증가 → 마일스톤 확인 |
| `onBreakSpeed` | `PlayerEvent.BreakSpeed` | 석사 이상 돌 채굴 속도 ×1.25 |
| `onPlayerLoggedIn` | `PlayerLoggedInEvent` | 접속 시 입문 학위증명서/학문별 학위증명서 누락분 재지급 |
| `onRegisterCommands` | `RegisterCommandsEvent` | `/stone set/add`, `/knowledge reset` 명령어 등록 |
| `LecternHandler::onRightClickBlock` | `RightClickBlock` | 독서대 클릭 시 지식 책 UI 열기 |

**기타 메서드:**

| 메서드 | 역할 |
|---|---|
| `grantIfMilestone(player, shardCount)` | 100/300/500/1000개 도달 시 발전과제 + 학문 접두사 포함 학위증명서 지급 (`>=` 비교) |
| `grantKnowledgeAdvancement(player, shardCount)` | 발전과제 달성 처리 |
| `createDiploma(baseItem, subjectName, diplomaName)` | 학위증명서 ItemStack 생성 (`DataComponents.CUSTOM_NAME`으로 "암석학 학사 학위증명서" 설정) |
| `hasDiploma(player, item, displayName)` | 인벤토리에 해당 아이템 + 커스텀 이름이 있는지 확인 |
| `resetBookProgress(source, player, bookId)` | 특정 책 진행도 초기화 |

---

### 3.2 어태치먼트 (플레이어 데이터)

#### `ModAttachments.java`

| 어태치먼트 이름 | 타입 | 용도 | 동기화 |
|---|---|---|---|
| `stone_knowledge_shard` | `Integer` | 돌 지식조각 총 개수 | O |
| `book_read_progress` | `Map<String, Integer>` | 책별 독서 진행도 (획득 조각 수) | O |
| `book_snapshot_knowledge` | `Map<String, Integer>` | 공부 시작 시점의 지식 조각 스냅샷 | O |

- `book_read_progress`: 책 ID → 해당 책에서 획득한 조각 수. 재접속 후 유지.
- `book_snapshot_knowledge`: 책 ID → 공부 시작 시점의 지식 조각. 스냅샷 < 책 난이도이면 해당 책에서 최적 보상 보장.

---

### 3.3 클라이언트 (화면/HUD)

#### `ClientModEvents.java`

`@EventBusSubscriber(value = Dist.CLIENT)` — 서버에서 로딩 안 됨.

`onRegisterMenuScreensEvent`: 메뉴 타입↔스크린 클래스 연결.
- `ModMenus.ROCK_ANALYZER` → `RockAnalyzerScreen`
- `ModMenus.KNOWLEDGE_BOOK` → `KnowledgeBookScreen`

#### `ClientKnowledgeHud.java`

화면 우하단에 지식 조각 표시 HUD.

| 필드 | 역할 |
|---|---|
| `ICON_ITEM` | 돌 블록 아이템 (캐시, `static final`) |
| `ICON_SIZE = 16` | 아이콘 크기 |
| `ICON_SCALE = 0.4F` | 축소 배율 |

`renderHud()`: 0.4배 축소된 돌 블록 아이콘 + 지식 조각 수 표시. `STONE_KNOWLEDGE_SHARD` 어태치먼트의 동기화된 값을 읽음.

#### `KnowledgeBookScreen.java`

지식 책 읽기 UI. 바닐라 `book.png` 텍스처를 배경으로 사용.

| 필드 | 역할 |
|---|---|
| `BOOK_W=192, BOOK_H=192` | UI 크기 |
| `PAD_LEFT=40, PAD_RIGHT=18, PAD_TOP=18` | 여백 |
| `CONTENT_W = BOOK_W - PAD_LEFT - PAD_RIGHT` | 콘텐츠 영역 폭 |
| `LINE_H=9` | 한 줄 높이 |
| `BAR_LEN=8` | 게이지 바 길이 (█░ 문자) |
| `COL_TITLE`, `COL_LABEL`, `COL_VALUE` 등 | 색상 상수 |

**렌더링 흐름 (`renderBg`)**:
1. 배경 이미지 블릿
2. 제목 표시 (학문 아이콘 + 색상별 제목)
3. 종류/희귀도 표시
4. 구분선
5. 저자/학문/난이도 정보
6. 구분선
7. 이해도 게이지 (████░░░░ 50%)
8. 공부 중 → 진행 바 + 획득 조각 + 남은 시간
9. 공부 전 → 예상 시간 + 예상 조각 + 과목 아이콘 (현재: 돌 아이템)

**버튼**: "공부" (한 번 클릭으로 시작), "가져가기" (언제나 활성화)

#### `RockAnalyzerScreen.java`

암석 분석기 UI. 바닐라 석재절단기 텍스처/레이아웃 재사용.

- 4×3 스크롤 가능한 변환 목록
- 스크롤바 드래그, 마우스 휠, 클릭 지원
- 변환 아이템 툴팁 표시

---

### 3.4 아이템

#### `NoteItem.java` — 메모장

공부 자동 루프에 사용되는 소모품.

| 필드/메서드 | 역할 |
|---|---|
| `MAX_PAGES = 200` | 최대 페이지 수 |
| `appendHoverText()` | `§7[페이지] 0/200` 툴팁 표시 |
| `getPageCount(stack)` | DataComponent에서 페이지 수 읽기 |
| `setPageCount(stack, pages)` | 페이지 수 설정 |
| `addPage(stack)` | 페이지 +1 (실시간으로 즉시 기록) |
| `canUse(stack)` | pages < 200이면 true |

- `ModItems`에서 `stacksTo(1)`으로 등록 (스택 불가)
- `getMaxStackSize` 오버라이드 제거 (기본값 1 사용)

---

### 3.5 지식 시스템 공통 (`knowledge/common/`)

#### `BookType.java` — 책 유형 (10종)

| 열거형 | 한국어 |
|---|---|
| `TEXTBOOK` | 교과서 |
| `INTRODUCTION` | 입문서 |
| `SPECIALIZED` | 전문서 |
| `ENCYCLOPEDIA` | 백과사전 |
| `RESEARCH_PAPER` | 연구논문 |
| `EXPERIMENTAL_MANUAL` | 실험서 |
| `FIELD_REPORT` | 현장 기록 |
| `ANCIENT_BOOK` | 고서 |
| `DICTIONARY` | 사전 |
| `WORKBOOK` | 문제집 |

티어별로 사용 가능한 유형이 다름 (예: 입문+일반 = 교과서/입문서/문제집, 박사+전설 = 고서/원본논문).

#### `BookRarity.java` — 책 희귀도 (5등급)

| 열거형 | 한국어 | RGB | 채팅 색상 |
|---|---|---|---|
| `COMMON` | 일반 | 0xA0A0A0 | gray |
| `UNCOMMON` | 고급 | 0xA0AA00 | green |
| `RARE` | 희귀 | 0x3060D0 | blue |
| `EPIC` | 매우 희귀 | 0xA020F0 | dark_purple |
| `LEGENDARY` | 전설 | 0xFFAA00 | gold |

희귀도별 생성 확률: 일반 50%, 고급 30%, 희귀 15%, 매우 희귀 4%, 전설 1%.

#### `KnowledgeTier.java` — 지식 단계 엔진

이 모드의 **핵심 계산 로직**. 모든 보상/시간 공식이 여기에 있음.

**5단계 지식 티어:**

| 티어 | 범위 | 한국어 |
|---|---|---|
| `BELOW_ENTRY` | 0~99 | 입문 이전 |
| `ENTRY` | 100~299 | 입문 |
| `BACHELOR` | 300~499 | 학사 |
| `MASTER` | 500~999 | 석사 |
| `PHD` | 1000+ | 박사 |

**핵심 메서드:**

| 메서드 | 역할 |
|---|---|
| `fromKnowledge(shards)` | 조각 수 → 티어 판정 |
| `getDefaultBaseReward(tier)` | 티어별 기본 보상: 10/20/35/50/75 |
| `getDefaultMaxShards(tier)` | 티어별 최대 조각: 50/50/150/300/500 |
| `getMiningChance(count)` | 조각 수 → 채굴 확률: 10%/5%/3%/1%/0.5% |
| `computeMultiplier(gap)` | gap → 보상 배율 계산 (.piecewise 선형 보간) |
| `computeReward(gap, base, cur, max)` | 최종 보상 계산 |
| `computeReadTime(gap)` | 공부 시간(틱) 계산 |

#### `KnowledgeBookDefinition.java` — 책 정의 (record)

하나의 지식 책을 정의하는 불변 데이터 클래스.

```java
record KnowledgeBookDefinition(
    String id,           // 고유 ID (예: "petrology_entry_abc123")
    String title,        // 제목 (예: "암석학의 교과서")
    String author,       // 저자 (예: "화석박사")
    int difficulty,      // 난이도 (0~1500)
    String field,        // 학문 분야 (예: "petrology")
    int maxShards,       // 최대 획득 가능 조각
    int baseReward,      // 기본 보상
    BookType bookType,   // 책 유형
    BookRarity bookRarity // 희귀도
)
```

compact 생성자에서 `maxShards`와 `baseReward`를 티어 기본값으로 자동 계산.

#### `KnowledgeRegistry.java` — 동적 책 레지스트리

책은 사전에 정의되지 않고, 랜덤 책을 확인할 때 **동적으로** 생성/등록됨.

| 메서드 | 역할 |
|---|---|
| `registerDynamic(def)` | 새 책 등록 |
| `get(id)` | ID로 조회 |
| `contains(id)` | ID 존재 여부 |
| `getByDifficulty(diff)` | 난이도로 조회 |
| `getByProperties(diff, max, base)` | 속성 정확 매칭 (클라이언트 렌더링용) |

#### `KnowledgeBookItem.java` — 책 아이템 기본 클래스

| 메서드 | 역할 |
|---|---|
| `getBookId()` | 책 ID 반환 |
| `getDefinition()` | 레지스트리에서 정의 조회 |
| `isFoil(stack)` | 난이도500+이면 인챈트 글로우 |
| `appendHoverText()` | 풍부한 툴팁 표시 (유형/희귀도/저자/학문/난이도/진행도) |

#### `RandomKnowledgeBookItem.java` — 랜덤 미확인 책

플레이어가 오른쪽 클릭하거나 독서대에 놓으면 **랜덤으로** 책 속성을 생성.

| 필드 | 역할 |
|---|---|
| `minDifficulty, maxDifficulty` | 이 티어의 난이도 범위 |
| `FIELDS` | 학문 목록 (현재 "petrology"만) |
| `AUTHORS` |14명의 한국어 저자 이름 목록 |
| `RARITY_TABLE[100]` | 희귀도 가중치 테이블 |
| `ICON` | `Items.STONE` (UI 과목 아이콘용 캐시) |

**핵심 메서드:**

| 메서드 | 역할 |
|---|---|
| `isIdentified(stack)` | DataComponent 있으면 확인 완료 |
| `identifyBook(stack, id)` | 랜덤 속성 생성 → 레지스트리 등록 → DataComponent 설정 |
| `generateRandomBook(rng)` | 완전 랜덤 책 생성 (ThreadLocalRandom 사용) |
| `getTierCap(field)` | 티어별 최대 난이도: 입문200/학사500/석사1000/박사2000 |
| `getRarityMultiplier(rarity)` | 희귀도별 배율: 일반0.25/고급0.50/희귀0.75/매우희귀0.90/전설1.00 |
| `getBaseRewardForRarity(rarity)` | 희귀도별 기본 보상: 일반6/고급15/희귀25/매우희귀40/전설60 |

#### `KnowledgeBookMenu.java` — 공부 메뉴/타이머

이 모드에서 **가장 복잡한 클래스**. 서버에서 공부 진행 상황을 관리.

**핵심 구조:**
- `ActiveStudy` 내부 클래스: 플레이어당 하나의 공부 세션 상태 저장
- `ACTIVE_STUDIES`: `Map<UUID, ActiveStudy>` — 모든 활성 공부 상태

**ActiveStudy 필드:**

| 필드 | 역할 |
|---|---|
| `durationTicks` | 이번 사이클 공부 시간 (틱) |
| `totalReward` | 이번 루프에서 획득할 총 조각 |
| `cycleTicks` | 조각1개당 틱 수 (duration / totalReward) |
| `elapsedTicks` | 경과 틱 |
| `grantedShards` | 이번 루프에서 이미 획득한 조각 |
| `cycleCount` | 완료된 사이클 수 |
| `snapshotKnowledge` | 공부 시작 시점의 지식 조각 스냅샷 |

**주요 흐름:**

1. **공부 시작** (`handleStudyStart`): 검증 → 총 보상 계산 → 스냅샷 저장 → `ActiveStudy` 생성
2. **틱 업데이트** (`tick`): 매서버 틱마다 → 조각 지급 시 즉시 메모장에 페이지 기록 → 사이클 완료 시 → 다음 사이클 또는 종료
3. **노트 사용** (`useNote`): 유효성 검사만 수행 (페이지 추가는 `tick()`에서 처리)
4. **완료** (`completeStudy`): 효과음 → 메시지 → 마일스톤 → 메뉴 닫기

**노트 실시간 기록:**
- `tick()`에서 매 cycleTicks마다 조각 지급 시, 즉시 인벤토리의 메모장에 페이지 추가
- `BOOK_PAGE_TURN` 사운드 재생
- 메모장 없거나 모두200페이지 → `"§c메모장이 없습니다! 공부를 중단합니다."` → 공부 중단

#### `LecternHandler.java` — 독서대 인터랙션

우클릭 시 독서대 위의 지식 책을 처리.

**흐름:**
1. 독서대 위에 지식 책이 없으면 → 무시 (바닐라 동작)
2. **시프트+우클릭** → 책을 인벤토리로 가져가기
3. **확인된 책** → 바로 UI 열기
4. **미확인 책** → 자동 확인 → 메시지 표시 → UI 열기
5. 확인 후 독서대에 책 동기화 (`lectern.setBook(bookStack)` + `setChanged()`)

---

### 3.6 암석학 모듈 (`knowledge/petrology/`)

현재 구현된 유일한 학문 모듈.

#### `RockAnalyzerBlock.java`

- 낮은 테이블 형태 (높이9px)
- `RockGates.hasBachelor()` 확인 — 학사(300+)가 아니면 사용 불가
- `RockAnalyzerMenu` 열기

#### `RockAnalyzerMenu.java`

- 입력 슬롯(좌) + 결과 슬롯(우)
- 8종 암석을 서로 1:1 변환
- 변환 목록: 입력과 다른 암석7종
- DataSlot으로 레시피 선택 동기화

#### `RockGates.java`

`hasBachelor(player)`: `knowledge/stone_300` 발전과제 달성 여부로 자격 판정. 서버 전용.

#### `RockTypes.java`

8종 암석 목록: STONE, COBBLESTONE, GRANITE, DIORITE, ANDESITE, TUFF, DEEPSLATE, COBBLED_DEEPSLATE

---

### 3.7 레지스트리

#### `ModBlocks.java`

| 블록 | 비고 |
|---|---|
| `ROCK_ANALYZER` | hardness=3.5, 돌 사운드, 곡괭이 필요 |

#### `ModItems.java`

| 아이템 | 클래스 | 비고 |
|---|---|---|
| `ROCK_ANALYZER` | `BlockItem` | 블록 아이템 |
| `GEOLOGIST_CERTIFICATE` | `Item` | 입문 학위증명서 |
| `BACHELOR_DIPLOMA` | `Item` | 학사 학위증명서 (학문 접두사 포함) |
| `MASTER_DIPLOMA` | `Item` | 석사 학위증명서 (학문 접두사 포함) |
| `PHD_DIPLOMA` | `Item` | 박사 학위증명서 (학문 접두사 포함) |
| `NOTE` | `NoteItem` | 메모장 (stacksTo1, 페이지0~200) |
| `RANDOM_BOOK_ENTRY` | `RandomKnowledgeBookItem` | 난이도0~299 |
| `RANDOM_BOOK_BACHELOR` | `RandomKnowledgeBookItem` | 난이도300~499 |
| `RANDOM_BOOK_MASTER` | `RandomKnowledgeBookItem` | 난이도500~999 |
| `RANDOM_BOOK_PHD` | `RandomKnowledgeBookItem` | 난이도1000~1500 |

#### `ModMenus.java`

| 메뉴 | 스크린 |
|---|---|
| `ROCK_ANALYZER` | `RockAnalyzerScreen` |
| `KNOWLEDGE_BOOK` | `KnowledgeBookScreen` |

#### `ModDataComponents.java`

| 컴포넌트 | 타입 | 용도 |
|---|---|---|
| `random_book_data` | `CompoundTag` | 랜덤 책 확인된 속성 (bookId 등) |
| `note_pages` | `Integer` | 메모장 페이지 진행도 (0~200) |

---

### 3.8 유틸리티

#### `Chance.java`

| 메서드 | 역할 |
|---|---|
| `chance(random, percent)` | `random.nextFloat() * 100 < percent` |
| `chanceBlock(random, count)` | `KnowledgeTier.getMiningChance(count)` 기반 확률 |

---

## 4. 핵심 시스템 상세

### 4.1 지식조각 획득 흐름

```
플레이어가 돌 블록을 캠
    │
    ▼
onBlockBreak() 호출 (서버만)
    │
    ├─ BASE_STONE_OVERWORLD가 아니면 → 무시
    │
    ├─ 석사 이상(500+) → 50% 확률로 내구도 소모 무효화
    │
    ├─ chanceBlock(random, 현재_조각수) → 확률 판정
    │   ├─ 0~99개: 10%
    │   ├─ 100~299개: 5%
    │   ├─ 300~499개: 3%
    │   ├─ 500~999개: 1%
    │   └─ 1000+개: 0.5%
    │
    ├─ 성공 시 → 조각 +1 → DataSlot에 반영
    │
    └─ grantIfMilestone() 호출
        ├─ 100개 → "암석학 입문" 발전과제
        ├─ 300개 → "암석학 학사" 발전과제 + "암석학 학사 학위증명서" 지급
        ├─ 500개 → "암석학 석사" 발전과제 + "암석학 석사 학위증명서" 지급
        └─ 1000개 → "암석학 박사" 발전과제 + "암석학 박사 학위증명서" 지급
```

### 4.2 랜덤 지식 책 시스템

```
랜덤 책 아이템을 오른쪽 클릭
    │
    ▼
RandomKnowledgeBookItem.use()
    │
    ├─ 이미 확인됨? → 무시
    │
    ├─ 랜덤 속성 생성:
    │   ├─ 난이도: min~max 범위 내 랜덤
    │   ├─ 희귀도: 가중치 테이블 (일반50%/고급30%/희귀15%/매우희귀4%/전설1%)
    │   ├─ 유형: 티어+희귀도 조합 이름 풀에서 랜덤
    │   ├─ 저자:14명 중 랜덤
    │   ├─ 제목: "{학문}의 {유형}"
    │   └─ maxShards, baseReward: 티어×희귀도 배율 적용
    │
    ├─ KnowledgeRegistry에 동적 등록
    ├─ DataComponent(bookId) 설정
    ├─ 아이템 이름 변경 (희귀도 색상 적용)
    └─ 채팅 메시지 표시
```

### 4.3 공부 시스템

```
독서대 위 확인된 책 → 우클릭 → UI 열기
    │
    ▼
"공부" 버튼 클릭
    │
    ▼
handleStudyStart()
    ├─ 독서대/책 검증
    ├─ 스냅샷 저장 (지식 조각)
    ├─ 총 보상 계산 (예상 조각)
    ├─ 공부 시간 계산 (gap 기반)
    └─ ActiveStudy 생성 → ACTIVE_STUDIES에 저장
         │
         ▼
    tick() 매서버 틱 호출
         │
         ├─ elapsedTicks++ → 진행률 갱신
         │
         ├─ cycleTicks마다1조각 지급:
         │   ├─ 지식 조각 +1
         │   ├─ 책 이해도 +1
         │   ├─ DataSlot 갱신 (클라이언트 UI 반영)
         │   └─ 남은 보상 계산
         │
          └─ 사이클 완료 (elapsedTicks >= durationTicks):
              ├─ 전체 보상 소진? → 완료 처리
              ├─ 노트 사용 가능? → useNote() → BOOK_PAGE_TURN 사운드 + 루프 재시작
              ├─ 인벤토리 가득 참 → 경고 메시지 + 공부 종료
              └─ 다음 사이클 → gap 재계산 → duration/cycleTicks 갱신
```

### 4.4 메모장 실시간 페이지 기록 시스템

```
tick()에서 조각 지급 시 (cycleTicks마다):
    │
    ├─ usable note 탐색 (pages < 200)
    │
    ├─ 노트 발견 시:
    │   ├─ NoteItem.addPage(note) → 즉시 페이지 +1
    │   ├─ BOOK_PAGE_TURN 사운드 재생
    │   └─ 공부 계속 진행
    │
    └─ 노트 미발견 시:
        ├─ 경고 메시지: "§c메모장이 없습니다! 공부를 중단합니다."
        └─ 공부 중단 (completeStudy 호출)

cycle 완료 후:
    ├─ 전체 보상 소진? → 완료 처리
    ├─ 다음 cycle → gap 재계산 → duration/cycleTicks 갱신
    └─ useNote()는 유효성 검사만 수행 (페이지 추가는 tick()에서 처리)
```

### 4.5 스냅샷 시스템

공부 시작 시점의 지식 조각을 저장. 나중에 다시 공부할 때:
- **스냅샷 < 책 난이도** → 해당 책에서 최적 보상 보장 (지식이 올라가도 페널티 없음)
- **스냅샷 ≥ 책 난이도** → 현재 지식 기반으로 페널티 적용

---

## 5. 게임 수치 밸런스

### 5.1 채굴 확률

| 현재 조각 | 확률 |
|---|---|
| 0~99 | 10% |
| 100~299 | 5% |
| 300~499 | 3% |
| 500~999 | 1% |
| 1000+ | 0.5% |

### 5.2 티어 보상

| 티어 | 기본 보상 | 최대 조각 | 채굴 보너스 |
|---|---|---|---|
| 입문 이전 | 10 | 50 | - |
| 입문 | 20 | 50 | - |
| 학사 | 35 | 150 | 분석기 사용 가능 |
| 석사 | 50 | 300 | 내구도50% 무효화 + 채굴속도×1.25 |
| 박사 | 75 | 500 | - |

### 5.3 랜덤 책 희귀도

| 희귀도 | 확률 | 기본 보상 | 티어 배율 | 최대 조각 |
|---|---|---|---|---|
| 일반 | 50% | 6 | ×0.25 | tierCap ×0.25 |
| 고급 | 30% | 15 | ×0.50 | tierCap ×0.50 |
| 희귀 | 15% | 25 | ×0.75 | tierCap ×0.75 |
| 매우 희귀 | 4% | 40 | ×0.90 | tierCap ×0.90 |
| 전설 | 1% | 60 | ×1.00 | tierCap ×1.00 |

tierCap: 입문=200, 학사=500, 석사=1000, 박사=2000

### 5.4 보상 배율 곡선

`gap = 책 난이도 - 플레이어 지식`

| gap 범위 | 배율 | 설명 |
|---|---|---|
| < -15 | 0~2 | 수준 훨씬 이하 (감소 보상) |
| -15 ~ 15 | 1.0 | 최적 수준 |
| 15 ~ 50 | 1.0 → 0.8 | 완만한 감소 |
| 50 ~ 100 | 0.8 → 0.6 | |
| 100 ~ 200 | 0.6 → 0.4 | |
| 200 ~ 400 | 0.4 → 0.25 | |
| 400 ~ 800 | 0.25 → 0.15 | |

### 5.5 공부 시간

| gap | 시간 |
|---|---|
| ≤ 15 | 100틱(5초) |
| 15~99 | `100 × (gap/15)^1.337` 틱 |
| 100+ | `1264 × (gap/100)^1.8` 틱 (최대18000틱/15분) |

### 5.6 메모장

- 최대 페이지: 200
- stacksTo(1) (스택 불가)
- 페이지 추가: `tick()`에서 조각 지급 시 즉시 기록 (cycle 완료 후 아님)
- 조합법: 종이2개 + 철조각2개 (모양: PP/NN)

---

## 6. 명령어

| 명령어 | 설명 | 권한 |
|---|---|---|
| `/stone set <개수>` | 지식조각을 정확히 설정 | OP |
| `/stone add <개수>` | 지식조각 추가 | OP |
| `/knowledge reset <book_id>` | 특정 책 진행도 초기화 | OP(2) |
| `/knowledge reset <player> <book_id>` | 특정 플레이어의 책 진행도 초기화 | OP(2) |

---

## 7. 빌드 및 실행

```bash
# 클라이언트 실행
./gradlew runClient

# 서버 실행
./gradlew runServer

# 클린 빌드
./gradlew clean build

# 의존성 새로고침
./gradlew --refresh-dependencies
```

---

## 8. 코드 컨벤션

### 주석
- 모든 주석은 **한국어**
- 복잡한 로직에는 "왜"를 설명하는 인라인 주석
- import 위에 한 줄로 클래스 역할 설명

### 네이밍
- 클래스: `PascalCase` (예: `RockAnalyzerBlock`)
- 메서드: `camelCase` (예: `hasBachelor`)
- 상수: `UPPER_SNAKE_CASE` (예: `ROCK_ITEMS`)
- 리소스 ID: `lower_snake_case` (예: `rock_analyzer`)

### 아키텍처
- **학문별 모듈**: `knowledge/<학문명>/` 패키지
- **클라이언트/서버 분리**: `Dist.CLIENT`로 분리
- **어태치먼트 기반 데이터**: 플레이어별 지속 데이터
- **DeferredRegister 패턴**: 모든 레지스트리에 적용

---

## 9. 향후 확장 계획

### 9.1 새 학문 모듈 추가

현재 암석학(petrology)만 구현. 아래 절차로 새 학문 추가 가능:

1. `knowledge/<학문명>/` 패키지 생성
2. `<학문>AnalyzerBlock.java` — 블록 구현
3. `<학문>AnalyzerMenu.java` — 메뉴 구현
4. `<학문>Gates.java` — 자격 판정 (발전과제 기반)
5. `<학문>Types.java` — 학문별 아이템/변환 목록
6. `RandomKnowledgeBookItem.FIELDS`에 새 학문 이름 추가
7. `ModBlocks`, `ModItems`, `ModMenus`에 새 항목 등록
8. 발전과제 JSON 추가

**예정 학문 (추천):**
- **화학(chemistry)**: 광물 분석, 합성 시스템
- **생물학(biology)**: 동식물 연구, 생태계
- **물리학(physics)**: 에너지 시스템, 회로
- **역사(history)**: 고대 유물, 고서 분석

### 9.2 지식 조각 → 실질적 보상

현재 지식 조각은 표시용. 향후:
- 조각 수에 따른 패시브 능력 (예: 채굴 속도, 이동 속도)
- 조각 소모 스킬 시스템
- 조각 기반 특수 아이템 제작

### 9.3 책 시스템 확장

- 고정된 책 추가 (기존 코드에서 제거했으나 재도입 가능)
- 퀘스트 기반 책 (특정 조건 달성 시 자동 생성)
- 멀티플레이어 공유 도서관
- 책 거래/교환 시스템

### 9.4 UI 고도화

- 커스텀 배경 텍스처 (현재 바닐라 book.png 재사용)
- 애니메이션 효과 (책 넘기는 연출)
- 사운드 디자인 (공부 중 배경음)
- 키보드 단축키

### 9.5 멀티플레이어

- 서버별 설정 파일 (채굴 확률, 보상 배율 커스터마이징)
- 플레이어 간 지식 비교 시스템
- 경쟁/협력 요소

### 9.6 데이터팩/리소스팩 지원

- 커스텀 학문을 데이터팩으로 정의
- 커스텀 텍스처/모델을 리소스팩으로 교체
- 서버 설정을 데이터팩에서 로드

### 9.7 발전과제 체인 확장

현재4개 (100/300/500/1000). 확장 가능:
- 중간 마일스톤 (150, 200, 400, 750 등)
- 학문별 독립 발전과제
- 숨겨진 발전과제 (특수 조건)

### 9.8 주민 시스템 리서치

#### 주민 머리 방향 고정 (아래쪽)

| 항목 | 내용 |
|---|---|
| **가능성** | 가능 (매 틱마다 강제 설정 필요) |
| **권장 방식** | `LivingEvent.LivingTickEvent`에서 `setXRot(90.0F)` 강제 |
| **이동 지장** | 없음 (`LookControl`과 `MoveControl`은 독립) |
| **주의사항** | 한 번만 설정하면 AI가 덮어씀. 매 틱 반복 필수 |

```java
@SubscribeEvent
public static void onLivingTick(LivingEvent.LivingTickEvent event) {
    Entity entity = event.getEntity();
    if (entity instanceof Villager) {
        entity.setXRot(90.0F);  // 정면 아래
        entity.xRotO = 90.0F;  // 이전 틱 보간용
    }
}
```

#### 주민 교배 API

| API | 설명 | 주민 전용 여부 |
|---|---|---|
| `BabyEntitySpawnEvent` | 교배 직전 발생, 취소/자녀 변경 가능 | 모든 Animal |
| `VillagerTradesEvent` | 거래 목록 수정 | 주민 전용 |
| `FinalizeSpawnEvent` | 모든 스폰 시 발생 (교배 포함) | 모든 Mob |

- **제한**: 주민-specific 교배 제어 이벤트는 없음
- vanilla `VillagerMakeLove` brain behavior에 직접 접근 필요
- 교배 조건(음식, 침대) 제어를 위한 별도 이벤트 없음

#### 타사 주민 API (참고용)

| 모드 | 기능 | NeoForge 1.21.1 지원 |
|---|---|---|
| **Villager API** (frikinjay) | 데이터 기반 커스텀 직업/타입/거래 | O (v1.26.1.3) |
| **Villager Overhaul** (Z2SIX) | 주민 AI/전투/거래 개선 | O (v3.0.0) |
| **Easy Villagers** | 주민 관리 간소화 | O |
