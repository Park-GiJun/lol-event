package com.gijun.main.application.port.out.external

/**
 * 글을 벡터로 바꾼다.
 *
 * **문서를 넣을 때와 질문을 검색할 때 같은 구현을 써야 한다.** 모델이 다르면 두 벡터가 다른 공간에
 * 있어서 거리가 아무 뜻도 없다. 임베딩 모델을 바꾸면 쌓아 둔 문서를 전부 다시 임베딩한다.
 */
interface TextEmbeddingPort {
    /** @return 길이가 `rag.embedding.dimensions` 인 벡터 */
    fun embed(text: String): List<Float>
}
