package com.third_quadrant.intelligentknowledge.knowledge.common;

import java.util.HashMap;
import java.util.Map;

// 지식 책 정의 레지스트리.
// 랜덤 책은 registerDynamic()으로 런타임에 등록한다.
public class KnowledgeRegistry {
    // 런타임에 생성된 랜덤 책 저장소.
    private static final Map<String, KnowledgeBookDefinition> DYNAMIC_BOOKS = new HashMap<>();

    // 런타임에 랜덤 책을 등록한다 (서버에서만 호출).
    public static void registerDynamic(KnowledgeBookDefinition book) {
        DYNAMIC_BOOKS.put(book.id(), book);
    }

    public static KnowledgeBookDefinition get(String id) {
        return DYNAMIC_BOOKS.get(id);
    }

    public static boolean contains(String id) {
        return DYNAMIC_BOOKS.containsKey(id);
    }

    public static KnowledgeBookDefinition getByDifficulty(int difficulty) {
        for (KnowledgeBookDefinition book : DYNAMIC_BOOKS.values()) {
            if (book.difficulty() == difficulty) return book;
        }
        return null;
    }

    // difficulty + maxShards + baseReward 조합으로 정확히 매칭 (클라이언트 렌더링용).
    public static KnowledgeBookDefinition getByProperties(int difficulty, int maxShards, int baseReward) {
        for (KnowledgeBookDefinition book : DYNAMIC_BOOKS.values()) {
            if (book.difficulty() == difficulty && book.maxShards() == maxShards
                    && book.baseReward() == baseReward) return book;
        }
        return null;
    }
}
