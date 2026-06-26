package kr.douid.brand.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

/**
 * JPA Auditing 활성화 설정
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * QueryDSL JPAQueryFactory bean 제공
     *
     * @param entityManager JPA entity manager
     * @return JPAQueryFactory
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
