package kr.douid.brand.client.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.client.domain.ClientIdentity;

/**
 * {@link ClientIdentity} Spring Data JPA 기술 전용 repository
 */
public interface ClientIdentityJpaRepository extends JpaRepository<ClientIdentity, Long> {
}
