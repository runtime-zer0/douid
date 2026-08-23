package kr.douid.brand.client.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.client.domain.ClientCredential;
import kr.douid.brand.client.domain.ClientCredentialRepository;
import kr.douid.brand.client.domain.ClientRecoveryToken;
import kr.douid.brand.client.domain.ClientRecoveryTokenRepository;
import kr.douid.brand.client.domain.RecoveryTokenExpiredException;
import kr.douid.brand.client.domain.RecoveryTokenInvalidException;

@ExtendWith(MockitoExtension.class)
class RecoveryConfirmationServiceTest {

    @Mock
    private ClientRecoveryTokenRepository clientRecoveryTokenRepository;

    @Mock
    private ClientCredentialRepository clientCredentialRepository;

    private final RecoveryTokenIssuer recoveryTokenIssuer = new RecoveryTokenIssuer(new ClientCredentialIssuer());
    private final ClientCredentialIssuer clientCredentialIssuer = new ClientCredentialIssuer();

    private RecoveryConfirmationService service;

    @BeforeEach
    void setUp() {
        service = new RecoveryConfirmationService(clientRecoveryTokenRepository, clientCredentialRepository,
                recoveryTokenIssuer, clientCredentialIssuer);
    }

    @Test
    void confirm_정상복원_새credential이_발급되고_기존credential은_조회되지않는다() {
        String rawToken = recoveryTokenIssuer.issueRawToken();
        ClientRecoveryToken token = ClientRecoveryToken.issue(1L, 10L, recoveryTokenIssuer.hash(rawToken),
                LocalDateTime.now().plusMinutes(15));
        given(clientRecoveryTokenRepository.findByTokenHashForUpdate(anyString())).willReturn(Optional.of(token));

        String rawClientToken = service.confirm(rawToken);

        assertThat(rawClientToken).isNotBlank();
        verify(clientCredentialRepository).save(any(ClientCredential.class));
        verify(clientCredentialRepository, never()).findByTokenHash(anyString());
    }

    @Test
    void confirm_만료된토큰_RecoveryTokenExpiredException() {
        String rawToken = recoveryTokenIssuer.issueRawToken();
        ClientRecoveryToken token = ClientRecoveryToken.issue(1L, 10L, recoveryTokenIssuer.hash(rawToken),
                LocalDateTime.now().minusMinutes(1));
        given(clientRecoveryTokenRepository.findByTokenHashForUpdate(anyString())).willReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirm(rawToken))
                .isInstanceOf(RecoveryTokenExpiredException.class);

        verify(clientCredentialRepository, never()).save(any());
    }

    @Test
    void confirm_이미소비된토큰_RecoveryTokenInvalidException() {
        String rawToken = recoveryTokenIssuer.issueRawToken();
        ClientRecoveryToken token = ClientRecoveryToken.issue(1L, 10L, recoveryTokenIssuer.hash(rawToken),
                LocalDateTime.now().plusMinutes(15));
        token.consume(LocalDateTime.now());
        given(clientRecoveryTokenRepository.findByTokenHashForUpdate(anyString())).willReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirm(rawToken))
                .isInstanceOf(RecoveryTokenInvalidException.class);

        verify(clientCredentialRepository, never()).save(any());
    }

    @Test
    void confirm_존재하지않는토큰_RecoveryTokenInvalidException() {
        given(clientRecoveryTokenRepository.findByTokenHashForUpdate(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm("unknown-token"))
                .isInstanceOf(RecoveryTokenInvalidException.class);
    }

    @Test
    void confirm_현재브라우저에임시Identity가있어도_토큰이가리키는Identity로새credential이발급된다() {
        String rawToken = recoveryTokenIssuer.issueRawToken();
        Long recoveryTargetIdentityId = 100L;
        ClientRecoveryToken token = ClientRecoveryToken.issue(recoveryTargetIdentityId, 10L,
                recoveryTokenIssuer.hash(rawToken), LocalDateTime.now().plusMinutes(15));
        given(clientRecoveryTokenRepository.findByTokenHashForUpdate(anyString())).willReturn(Optional.of(token));

        service.confirm(rawToken);

        verify(clientCredentialRepository).save(argThat(
                credential -> credential.getClientIdentityId().equals(recoveryTargetIdentityId)));
    }
}
