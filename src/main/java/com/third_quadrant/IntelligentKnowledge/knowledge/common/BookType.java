package com.third_quadrant.intelligentknowledge.knowledge.common;

// 책의 종류 분류.
public enum BookType {
    TEXTBOOK("교과서", "Textbook"),
    INTRODUCTION("입문서", "Introduction"),
    SPECIALIZED("전문서", "Specialized Book"),
    ENCYCLOPEDIA("백과사전", "Encyclopedia"),
    RESEARCH_PAPER("연구논문", "Research Paper"),
    EXPERIMENTAL_MANUAL("실험서", "Laboratory Manual"),
    FIELD_REPORT("현장 기록", "Field Report"),
    ANCIENT_BOOK("고서", "Ancient Book"),
    DICTIONARY("사전", "Dictionary"),
    WORKBOOK("문제집", "Workbook");

    private final String displayNameKo;

    BookType(String displayNameKo, String displayNameEn) {
        this.displayNameKo = displayNameKo;
    }

    public String getDisplayNameKo() {
        return displayNameKo;
    }
}
