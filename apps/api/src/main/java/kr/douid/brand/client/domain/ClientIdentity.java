package kr.douid.brand.client.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.douid.brand.shared.entity.BaseTimeEntity;
import lombok.Getter;

/**
 * 비회원 상담 주체 Aggregate Root
 *
 * 일반 회원 계정이 아니며, 실제 상담 시작 시점에만 생성된다.
 * 별도의 외부 공개용 식별자를 갖지 않고, 다른 Aggregate에서는 id로만 참조된다.
 */
@Getter
@Entity
@Table(name = "client_identity")
public class ClientIdentity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected ClientIdentity() {
    }

    /**
     * 새 상담 주체를 생성
     *
     * @return 생성된 상담 주체
     */
    public static ClientIdentity create() {
        return new ClientIdentity();
    }
}
