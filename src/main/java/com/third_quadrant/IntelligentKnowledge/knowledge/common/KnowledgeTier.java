package com.third_quadrant.intelligentknowledge.knowledge.common;

// 지식 조각 단계 판정 + 책 독서 효율 계산.
// chanceBlock()과 grantIfMilestone()의 기존 경계값(99/299/499/999)을 단일 위치에서 관리한다.
public enum KnowledgeTier {
    BELOW_ENTRY("입문 이전", 0, 99),
    ENTRY("입문", 100, 299),
    BACHELOR("학사", 300, 499),
    MASTER("석사", 500, 999),
    PHD("박사", 1000, Integer.MAX_VALUE);

    private final String displayName;
    private final int minInclusive;
    private final int maxInclusive;

    KnowledgeTier(String displayName, int minInclusive, int maxInclusive) {
        this.displayName = displayName;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public String getDisplayName() {
        return displayName;
    }

    // 기존 chanceBlock(), grantIfMilestone()과 동일한 경계값으로 단계를 판정한다.
    public static KnowledgeTier fromKnowledge(int shards) {
        if (shards <= 99) return BELOW_ENTRY;
        if (shards <= 299) return ENTRY;
        if (shards <= 499) return BACHELOR;
        if (shards <= 999) return MASTER;
        return PHD;
    }

    // 티어별 기본 baseReward (적정 난이도에서 1회 획득량).
    public static int getDefaultBaseReward(KnowledgeTier tier) {
        return switch (tier) {
            case BELOW_ENTRY -> 10;
            case ENTRY -> 20;
            case BACHELOR -> 35;
            case MASTER -> 50;
            case PHD -> 75;
        };
    }

    // 티어별 기본 maxShards (이 책에서 얻을 수 있는 최대 조각 수).
    public static int getDefaultMaxShards(KnowledgeTier tier) {
        return switch (tier) {
            case BELOW_ENTRY -> 50;
            case ENTRY -> 50;
            case BACHELOR -> 150;
            case MASTER -> 300;
            case PHD -> 500;
        };
    }

    // 세부 수치 (티어 내 sub-value). 기존 지식 수준과 동일 스케일.
    // 예: 150이면 ENTRY(100~299)의 세부 수치 50.
    public static int getSubValue(int knowledge) {
        KnowledgeTier tier = fromKnowledge(knowledge);
        return knowledge - tier.minInclusive;
    }

    // 기존 단계별 확률 판정 (chanceBlock과 동일 로직).
    public static float getMiningChance(int count) {
        if (count <= 99) return 10f;
        if (count <= 299) return 5f;
        if (count <= 499) return 3f;
        if (count <= 999) return 1f;
        return 0.5f;
    }

    // === 독서 효율 계산 ===
    // gap = bookDifficulty - playerKnowledge
    // gap < -15 → 수준 훨씬 이하 → 감소 보상 (0~2개)
    // gap -15 ~ 15 → 적정 → 배율 1.0 (전체 보상)
    // gap > 15 → 너무 어려운 책 → 점진적 감소

    // 기준점: (gap, multiplier) — 완화된 곡선.
    // gap=1~15(적정)에서 1.0, 이후 점진적으로 감소.
    private static final int[] BREAKPOINT_GAPS   = {  15,   50,  100,  200,  400,  800};
    private static final double[] BREAKPOINT_MULTS = {1.00, 0.80, 0.60, 0.40, 0.25, 0.15};

    // gap이 특정 기준점 구간에 속하는지 판정.
    // gap <= -15이면 0 반환.
    // gap -15 ~ 15는 배율 1.0 (적정 구간).
    public static double computeMultiplier(int gap) {
        if (gap < -15) {
            return 0.0;
        }

        // gap -15 ~ 15: 적정 구간, 배율 1.0.
        if (gap <= BREAKPOINT_GAPS[0]) {
            return 1.0;
        }

        // 기준점 사이 선형 보간.
        for (int i = 0; i < BREAKPOINT_GAPS.length - 1; i++) {
            if (gap <= BREAKPOINT_GAPS[i + 1]) {
                return linearInterpolate(gap,
                        BREAKPOINT_GAPS[i], BREAKPOINT_GAPS[i + 1],
                        BREAKPOINT_MULTS[i], BREAKPOINT_MULTS[i + 1]);
            }
        }

        // 마지막 기준점 초과.
        return BREAKPOINT_MULTS[BREAKPOINT_MULTS.length - 1];
    }

    // 두 점 (x0,y0), (x1,y1) 사이의 선형 보간.
    private static double linearInterpolate(double x, double x0, double x1, double y0, double y1) {
        if (x1 == x0) return y0;
        double t = (x - x0) / (x1 - x0);
        return y0 + t * (y1 - y0);
    }

    // 한 번 읽었을 때 실제 획득 조각 수.
    public static int computeReward(int gap, int baseReward, int currentBookShards, int maxShards) {
        // 이미 최대치에 도달.
        if (currentBookShards >= maxShards) {
            return 0;
        }

        // gap < -15 → 수준 훨씬 이하 책. 절대 갭(|gap|)에 따라 감소 보상.
        if (gap < -15) {
            int absGap = -gap;
            if (absGap >= 100) return 0;
            if (absGap >= 50) return 1;
            return 2;
        }

        // gap -15 ~ 15 → 최적 수준 (전체 보상).
        double multiplier = computeMultiplier(gap);
        int reward = Math.max(1, (int) Math.round(baseReward * multiplier));
        return Math.min(reward, maxShards - currentBookShards);
    }

    // 독서 시간 계산 (게임 틱 단위).
    // gap ≤ -15(수준 훨씬 이하): 100틱(5초, 최소 시간)
    // gap -15 ~ 15(적정): 100틱(5초)
    // gap 15~99: 완만한 지수 — gap=50일 때 ~25초
    // gap ≥ 100: 가파른 지수(exponent 1.8) — 연속적이나 기울기 급증, 15분 상한
    public static int computeReadTime(int gap) {
        if (gap <= 0) return 100;
        if (gap <= 15) return 100;
        if (gap < 100) {
            // 완만한 지수: y = 100 × (gap/15)^1.337
            double ratio = (double) gap / 15.0;
            return (int) (100 * Math.pow(ratio, 1.337));
        }
        // 가파른 지수: y = 1264 × (gap/100)^1.8, 15분 상한.
        // gap=100에서 1264틱으로 연속 연결, 이후 급격히 상승.
        return Math.min((int) (1264 * Math.pow(gap / 100.0, 1.8)), 18000);
    }
}
