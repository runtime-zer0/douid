package kr.douid.brand.chat.infrastructure.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.douid.brand.chat.application.query.ConversationMessageQueryService;
import kr.douid.brand.chat.application.query.MessageView;
import static kr.douid.brand.chat.domain.QMessage.message;
import kr.douid.brand.shared.pagination.CursorPageResponse;
import lombok.RequiredArgsConstructor;

/**
 * {@link ConversationMessageQueryService} port의 QueryDSL 구현체
 *
 * {@code conversationId} + {@code id} 오름차순 기준 cursor(keyset) pagination으로 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class JpaConversationMessageQueryService implements ConversationMessageQueryService {

    private final JPAQueryFactory queryFactory;

    /**
     * Conversation의 메시지 목록을 cursor 기반으로 페이지네이션 조회
     *
     * @param conversationId 조회할 상담의 내부 ID
     * @param cursor          이전 페이지 마지막 메시지 ID (없으면 처음부터)
     * @param size            페이지 크기
     * @return 메시지 목록 페이지
     */
    @Override
    public CursorPageResponse<MessageView> findMessages(Long conversationId, Long cursor, int size) {
        var predicate = message.conversationId.eq(conversationId);
        if (cursor != null) {
            predicate = predicate.and(message.id.gt(cursor));
        }

        List<MessageView> fetched = queryFactory
                .select(Projections.constructor(
                        MessageView.class,
                        message.id,
                        message.content,
                        message.createdAt))
                .from(message)
                .where(predicate)
                .orderBy(message.id.asc())
                .limit(size + 1L)
                .fetch();

        boolean hasNext = fetched.size() > size;
        List<MessageView> items = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasNext ? items.get(items.size() - 1).id() : null;

        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }
}
