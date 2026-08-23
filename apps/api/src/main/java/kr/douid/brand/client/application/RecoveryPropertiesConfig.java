package kr.douid.brand.client.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * {@link RecoveryProperties} 활성화 설정
 */
@Configuration
@EnableConfigurationProperties(RecoveryProperties.class)
class RecoveryPropertiesConfig {
}
