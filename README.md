# Intelligent Knowledge (지적 지식)

> 마인크래프트 모드 - 학문 기반 지식 시스템

---

## 기술 스택

| 항목 | 버전 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| NeoForge ModDev Gradle | 2.0.144 |
| Java | 21 |
| Parchment Mappings | 2024.11.17 |

---

## 모드 기본 정보

| 항목 | 값 |
|---|---|
| Mod ID | `intelligentknowledge` |
| Mod Name | Intelligent Knowledge |
| Package | `com.third_quadrant.intelligentknowledge` |
| Version | 1.0.0 |
| License | All Rights Reserved |

---

## 디렉토리 구조

```
Intelligent-Knowledge/
├── build.gradle                          # 빌드 설정 (ModDevGradle 플러그인)
├── gradle.properties                     # 버전/모드 속성 정의
└── src/main/
    ├── java/com/third_quadrant/intelligentknowledge/
    │   ├── IntelligentKnowledge.java         # 모드 엔트리포인트 (@Mod)
    │   ├── attachment/
    │   │   └── ModAttachments.java           # 데이터 어태치먼트 타입 정의
    │   ├── client/
    │   │   ├── ClientModEvents.java          # 클라이언트 전용 이벤트 (스크린 등록)
    │   │   └── gui/
    │   │       ├── ClientKnowledgeHud.java   # HUD 오버레이 (지식조각 표시)
    │   │       └── RockAnalyzerScreen.java   # 암석 분석기 GUI 스크린
    │   ├── knowledge/
    │   │   └── petrology/                    # 암석학 학문 모듈
    │   │       ├── RockAnalyzerBlock.java    # 암석 분석기 블록 로직
    │   │       ├── RockAnalyzerMenu.java     # 암석 분석기 컨테이너/메뉴
    │   │       ├── RockGates.java            # 발전과제 기반 자격 판정
    │   │       └── RockTypes.java            # 암석 아이템 목록 및 변환
    │   ├── registry/
    │   │   ├── ModBlocks.java                # 블록 레지스트리
    │   │   ├── ModItems.java                 # 아이템 레지스트리
    │   │   ├── ModMenus.java                 # 메뉴 타입 레지스트리
    │   │   └── ModCreativeTabs.java          # 크리에이티브 탭 레지스트리
    │   └── util/
    │       ├── Chance.java                   # 확률 유틸리티
    │       └── Text.java                     # 텍스트 컴포넌트 빌더
    ├── templates/META-INF/
    │   └── neoforge.mods.toml               # 모드 디스크립터 (변수 치환)
    └── resources/
        ├── assets/intelligentknowledge/
        │   ├── blockstates/rock_analyzer.json
        │   ├── lang/
        │   │   ├── en_us.json
        │   │   └── ko_kr.json
        │   └── models/
        │       ├── block/rock_analyzer.json
        │       └── item/
        │           ├── rock_analyzer.json
        │           └── geologist_certificate.json
        └── data/intelligentknowledge/
            ├── recipe/rock_analyzer.json
            └── advancement/knowledge/petrology/
                ├── stone_100.json            # 암석학 입문
                ├── stone_300.json            # 암석학 학사
                ├── stone_500.json            # 암석학 석사
                └── stone_1000.json           # 암석학 박사
```

---

## 아키텍처 개요

```
┌─────────────────────────────────────────────────────────┐
│                    IntelligentKnowledge                   │
│                  (@Mod 엔트리포인트)                       │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐  │
│  │ ModBlocks│  │ ModItems │  │ ModMenus │  │ ModTabs │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬────┘  │
│       │             │             │              │       │
│       └──────┬──────┴─────────────┴──────────────┘       │
│              │ DeferredRegister 등록                      │
│              ▼                                           │
│  ┌──────────────────────┐    ┌──────────────────────┐   │
│  │   NeoForge EVENT_BUS  │    │  Mod Attachments      │   │
│  │  (게임 이벤트 리스너)  │    │  (플레이어 데이터 저장) │   │
│  └──────────┬───────────┘    └──────────┬───────────┘   │
│             │                           │                │
│             ▼                           ▼                │
│  ┌──────────────────────┐    ┌──────────────────────┐   │
│  │   지식 시스템 로직      │    │   UI/HUD 시스템        │   │
│  │  (petrology 모듈)     │    │  (클라이언트 전용)      │   │
│  └──────────────────────┘    └──────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

## 핵심 시스템 상세

### 1. 모드 엔트리포인트 (`IntelligentKnowledge.java`)

`@Mod("intelligentknowledge")` 어노테이션이 달린 메인 클래스. NeoForge가 모드를 로드할 때 이 클래스의 생성자를 호출한다.

**생성자에서 하는 일:**
1. `ModAttachments` 등록 (최우선 - 어태치먼트 사용 전에 등록 필요)
2. `ModBlocks`, `ModItems`, `ModMenus`, `ModCreativeTabs` 등록 (모드 버스에)
3. `NeoForge.EVENT_BUS`에 이벤트 리스너 5개 등록

**등록된 이벤트 리스너:**

| 리스너 메서드 | 이벤트 타입 | 역할 |
|---|---|---|
| `onLivingJump` | `LivingEvent.LivingJumpEvent` | 점프 시 체력 1 회복 |
| `onBlockBreak` | `BlockEvent.BreakEvent` | 돌 채굴 시 지식조각 획득 + 마일스톤 발전과제 |
| `onBreakSpeed` | `PlayerEvent.BreakSpeed` | 석사 이상 돌 채굴 속도 25% 보너스 |
| `onPlayerLoggedIn` | `PlayerLoggedInEvent` | 학사 이상 자격증 재지급 |
| `onRegisterCommands` | `RegisterCommandsEvent` | `/stone set/add` 디버그 명령어 |

### 2. 데이터 어태치먼트 (`ModAttachments.java`)

NeoForge의 `AttachmentType`를 사용하여 플레이어별 데이터를 저장한다. 마인크래프트 1.21.1+의 NBT 시스템을 대체하는 네오포지 데이터 저장 방식이다.

**현재 등록된 어태치먼트:**

| 이름 | 타입 | 용도 | 동기화 |
|---|---|---|---|
| `stone_knowledge_shard` | `Integer` | 돌 지식조각 개수 | O (클라이언트 HUD용) |

**특징:**
- `serialize(Codec.INT)`: 디스크에 저장 (서버 재시작 후 유지)
- `sync(ByteBufCodecs.VAR_INT)`: 서버→클라이언트 동기화 (HUD 표시용)
- 기본값: `0`

### 3. 지식 시스템 (암석학 / Petrology)

현재 구현된 유일한 학문 모듈. 돌 채굴을 통해 "지식조각"을 얻는 시스템이다.

**지식조각 확률 (채굴당):**

| 보유 개수 | 확률 | 비고 |
|---|---|---|
| 0~99 | 10% | 입문 단계 |
| 100~299 | 5% | 입문 달성 후 |
| 300~499 | 3% | 학사 달성 후 |
| 500~999 | 1% | 석사 달성 후 |
| 1000+ | 0.5% | 박사 달성 후 |

**마일스톤 (발전과제 달성):**

| 개수 | 제목 | 색상 | 프레임 | 보상 |
|---|---|---|---|---|
| 100 | 암석학 입문 | `#39d6fe` (하늘색) | task | - |
| 300 | 암석학 학사 | `#fe9b2c` (주황) | task | 지질학 자격증 |
| 500 | 암석학 석사 | `#2c69fe` (파랑) | goal | - |
| 1000 | 암석학 박사 | `#9c2ad8` (보라) | challenge | - |

**석사 이상 보너스:**
- 돌 채굴 시 내구도 소모 50% 무시 (50% 확률로 무효화)
- 돌 채굴 속도 25% 증가

### 4. 암석 분석기

**블록 (`RockAnalyzerBlock`):**
- 바닐라 석재절단기와 동일한 형태 (VoxelShape: 높이 9px)
- 학사(300개) 이상이 아니면 사용 불가 (화면에 안내 메시지 표시)
- `RockAnalyzerMenu`를 통한 컨테이너 UI 제공

**메뉴 (`RockAnalyzerMenu`):**
- 입력 슬롯(좌측) + 결과 슬롯(우측) 구조
- 8종 암석(STONE, COBBLESTONE, GRANITE, DIORITE, ANDESITE, TUFF, DEEPSLATE, COBBLED_DEEPSLATE)을 서로 1:1 변환
- 변환 목록: 입력과 다른 암석 7종 표시
- 바닐라 석재절단기와 동일한 스크롤/클릭 UI 패턴

**스크린 (`RockAnalyzerScreen`):**
- 4×3 스크롤 가능한 변환 목록 그리드
- 바닐라 stonecutter 텍스처/스프라이트 재사용 (임시)
- 스크롤바 드래그, 마우스 휠, 클릭 지원
- 변환 아이템 툴팁 표시

**자격 판정 (`RockGates`):**
- `hasBachelor(player)`: `knowledge/stone_300` 발전과제 달성 여부 확인
- 서버 전용 (서버의 발전과제 매니저에서 판정)

### 5. UI/HUD 시스템

**지식 HUD (`ClientKnowledgeHud`):**
- 클라이언트 전용 (`@EventBusSubscriber(value = Dist.CLIENT)`)
- 화면 우하단에 돌 아이콘(0.4배 축소) + 지식조각 개수 표시
- `RegisterGuiLayersEvent`로 바닐라 HUD 위에 레이어 등록
- `STONE_KNOWLEDGE_SHARD` 어태치먼트의 동기화된 값을 읽어 표시

**암석 분석기 스크린 (`RockAnalyzerScreen`):**
- `RegisterMenuScreensEvent`로 `RockAnalyzerMenu`와 연결
- `@OnlyIn(Dist.CLIENT)`로 서버에서 로딩 방지

### 6. 레지스트리 패턴

모든 레지스트리는 동일한 `DeferredRegister` 패턴을 사용한다:

```java
public class ModXxx {
    private static final String MOD_ID = "intelligentknowledge";

    public static final DeferredRegister<TYPE> REGISTRY =
            DeferredRegister.create(Registries.XXX, MOD_ID);

    public static final DeferredItem/DeferredBlock/Supplier<TYPE> ENTRY =
            REGISTRY.register("name", () -> new ...);

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}
```

**등록 순서 (IntelligentKnowledge 생성자):**
1. `ModAttachments.register(modBus)` - 최우선
2. `ModBlocks.register(modBus)`
3. `ModItems.register(modBus)`
4. `ModMenus.register(modBus)`
5. `ModCreativeTabs.register(modBus)`

### 7. 유틸리티

**`Chance.java`** - 확률 계산
- `chance(random, percent)`: 일반 백분율 확률
- `chanceBlock(random, count)`: 지식조각 개수 기반 감소 확률

**`Text.java`** - 텍스트 컴포넌트 빌더
- `rgb()`, `color()`, `bold()`, `italic()`, `underline()`, `strike()`, `obfuscated()` 등
- 조합 가능: `bold(text, color)` = 볼드 + 색상

---

## 이벤트 처리 흐름

```
게임 이벤트 발생
    │
    ▼
NeoForge.EVENT_BUS (전역 이벤트 버스)
    │
    ├─ onLivingJump: 점프 → heal(1) (서버만)
    │
    ├─ onBlockBreak: 블록 파괴
    │   ├─ 클라이언트 → 무시
    │   ├─ BASE_STONE_OVERWORLD가 아니면 무시
    │   ├─ 석사 이상 → 내구도 보너스 (50% 확률)
    │   └─ chanceBlock() → 지식조각++ → 마일스톤 확인
    │
    ├─ onBreakSpeed: 채굴 속도
    │   ├─ 돌이 아니면 무시
    │   └─ 석사 이상 → 속도 ×1.25
    │
    ├─ onPlayerLoggedIn: 접속
    │   ├─ 학사 이상 + 자격증 없음 → 자격증 지급
    │
    └─ onRegisterCommands: 명령어 등록
        └─ /stone set|add
```

---

## 커스텀 코드 컨벤션

### 주석 스타일
- 코드 설명은 **한국어**로 작성
- import 위에 한 줄 주석으로 클래스 역할 설명
- 복잡한 로직에는 인라인 주석으로 "왜"를 설명
- 마크다운 형식의 상세한 설명 주석 사용

### 네이밍 컨벤션
- 클래스: `PascalCase` (예: `RockAnalyzerBlock`)
- 메서드: `camelCase` (예: `hasBachelor`)
- 상수: `UPPER_SNAKE_CASE` (예: `ROCK_ITEMS`)
- 리소스 ID: `lower_snake_case` (예: `rock_analyzer`, `stone_knowledge_shard`)

### 아키텍처 패턴
- **모듈 학문 단위**: `knowledge/<학문명>/` 패키지 아래에 블록, 메뉴, 게이트, 타입 분리
- **클라이언트/서버 분리**: `@EventBusSubscriber(value = Dist.CLIENT)`로 클라이언트 전용 클래스 분리
- **어태치먼트 기반 데이터**: 플레이어별 지속 데이터는 `AttachmentType` 사용
- **바닐라 재사용**: UI 텍스처/스프라이트는 바닐라 리소스를 그대로 재사용 (임시)

### 레지스트리 등록
- 반드시 `DeferredRegister` 패턴 사용
- `DeferredRegister.create()`로 생성 → `.register(modBus)`로 모드 버스에 등록
- 아이템: `DeferredItem<T>`, 블록: `DeferredBlock<T>`

---

## 리소스 구조

### 자산 (assets)

**아이템 모델** (`models/item/`):
- 각 아이템별 JSON 모델 파일
- 현재 바닐라 아이템 텍스처 참조 (예: `minecraft:item/paper`)

**블록 스테이트** (`blockstates/`):
- 블록별 스테이트 JSON (현재 rock_analyzer만)

**언어 파일** (`lang/`):
- `ko_kr.json`: 한국어 번역
- `en_us.json`: 영어 번역

### 데이터 (data)

**레시피** (`recipe/`):
- 암석 분석기 조합법 (비무자 shapeless)
- 재료: 자격증 + 돌 4개 + 자갈 4개

**발전과제** (`advancement/knowledge/petrology/`):
- 4단계 마일스톤 발전과제 (입문→학사→석사→박사)
- 부모-자식 관계로 체인 구성
- `trigger: "minecraft:impossible"` (프로그래밍 방식으로만 달성)
- 학사(300) 달성 시 `geologist_certificate` 보상

---

## 커맨드

| 명령어 | 설명 | 권한 |
|---|---|---|
| `/stone set <개수>` | 지식조각 개수를 정확히 설정 | OP |
| `/stone add <개수>` | 지식조각 개수를 추가 | OP |

- 마일스톤(100/300/500/1000)에 정확히 도달하면 발전과제 자동 달성

---

## 빌드 및 실행

```bash
# 의존성 새로고침
gradlew --refresh-dependencies

# 클라이언트 실행
gradlew runClient

# 서버 실행
gradlew runServer

# 데이터 생성
gradlew runData

# 클린 빌드
gradlew clean build
```

---

## 의존성

| 의존성 | 타입 | 비고 |
|---|---|---|
| NeoForge 21.1.248 | required | 필수 |
| Minecraft 1.21.1 | required | 필수 |
| Parchment 2024.11.17 | mappings | 파라미터 이름 매핑 |

---

## 향후 확장 포인트

1. **새 학문 모듈 추가**: `knowledge/<학문명>/` 패키지 생성, 해당 블록/메뉴/게이트 구현
2. **새 어태치먼트 추가**: `ModAttachments`에 새 `AttachmentType` 등록
3. **새 아이템/블록**: `ModItems`/`ModBlocks`에 `DeferredItem`/`DeferredBlock` 추가
4. **새 메뉴/스크린**: `ModMenus`에 타입 등록 → `ClientModEvents`에서 스크린 연결
5. **발전과제 확장**: `data/intelligentknowledge/advancement/`에 새 JSON 추가
