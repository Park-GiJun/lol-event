package com.gijun.main.application.handler

import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.shared.domain.vo.MatchId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 색인이 쓸 경기를 **트랜잭션 안에서** 읽는다.
 *
 * 경기 엔티티의 팀·참가자는 지연 로딩이라, 세션 없이 읽으면 도메인 모델로 옮기는 순간
 * `LazyInitializationException` 이 난다. 색인은 요청 스레드가 아니라 뒤에서 도는 스레드와 Kafka
 * 컨슈머에서 불리므로 열려 있는 세션이 없다.
 *
 * 색인 핸들러 전체에 `@Transactional` 을 걸지 않고 읽기만 떼어 낸 이유는, 색인이 임베딩 서버를
 * 수백 번 다녀오는 몇 분짜리 일이기 때문이다. 그동안 DB 커넥션을 쥐고 있으면 안 된다.
 * 같은 클래스 안에서 부르면 프록시를 안 거쳐 트랜잭션이 안 걸리므로 빈을 따로 둔다.
 */
@Component
class RagMatchReader(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
) {
    @Transactional(readOnly = true)
    fun findAll(queueIds: List<Int>): List<MatchModel> = matchQueryPersistencePort.findAllWithParticipants(queueIds)

    @Transactional(readOnly = true)
    fun find(matchId: MatchId): MatchModel? = matchQueryPersistencePort.findByMatchId(matchId)
}
