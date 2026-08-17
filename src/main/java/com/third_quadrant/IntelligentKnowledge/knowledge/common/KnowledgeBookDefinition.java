package com.third_quadrant.intelligentknowledge.knowledge.common;

// 지식 책 하나의 정의. 데이터 기반으로 관리하며, 앞으로 추가될 책도 이 구조를 따른다.
public record KnowledgeBookDefinition(
    String id,
    String title,
    String author,
    int difficulty,
    String field,
    int maxShards,
    int baseReward,
    BookType bookType,
    BookRarity bookRarity
) {
    // 기본 저자/종류/희귀도 미지정 시 자동 설정.
    public KnowledgeBookDefinition(String id, String title, String author, int difficulty, String field,
                                    BookType bookType, BookRarity bookRarity) {
        this(id, title, author, difficulty, field,
                KnowledgeTier.getDefaultMaxShards(KnowledgeTier.fromKnowledge(difficulty)),
                KnowledgeTier.getDefaultBaseReward(KnowledgeTier.fromKnowledge(difficulty)),
                bookType, bookRarity);
    }

    // 기존 지식 수준 스케일과 동일한 티어 판정.
    public KnowledgeTier getTier() {
        return KnowledgeTier.fromKnowledge(difficulty);
    }

    // 난이도에 따른 이름 색상. 석사=0x60E0F7, 박사=0x9760F7, 그 외 검정.
    public int getTitleColor() {
        KnowledgeTier tier = getTier();
        return switch (tier) {
            case MASTER -> 0x60E0F7;
            case PHD -> 0x9760F7;
            default -> 0x000000;
        };
    }
}
