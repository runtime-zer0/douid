package kr.douid.brand.client.application.port;

/**
 * 이메일 발송을 위한 application port
 *
 * 구현체는 {@code client.infrastructure.mail}에 두고, 실제 SMTP 연동 세부사항을 감춘다.
 */
public interface EmailSender {

    /**
     * 인증 코드를 이메일로 발송
     *
     * @param email 수신 이메일 주소
     * @param code  발송할 인증 코드 원문
     */
    void sendVerificationCode(String email, String code);

    /**
     * Recovery Magic Link를 이메일로 발송
     *
     * @param email         수신 이메일 주소
     * @param magicLinkUrl  발송할 Magic Link URL(raw token 포함)
     */
    void sendRecoveryMagicLink(String email, String magicLinkUrl);
}
