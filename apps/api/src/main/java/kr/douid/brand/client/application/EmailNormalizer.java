package kr.douid.brand.client.application;

import org.springframework.stereotype.Component;

/**
 * 이메일 주소를 일관되게 비교하기 위한 정규화 정책
 *
 * 이메일 공급자별 추정 규칙(예: Gmail의 점(.) 무시)은 적용하지 않는다 — 과도한 추정은 요구사항 범위 밖이다.
 */
@Component
public class EmailNormalizer {

    /**
     * 이메일을 정규화
     *
     * @param email 사용자 입력 원본 이메일
     * @return trim 후 소문자로 변환된 이메일
     */
    public String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
