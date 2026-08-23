package kr.douid.brand.client.infrastructure.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import kr.douid.brand.client.application.port.EmailSender;
import lombok.RequiredArgsConstructor;

/**
 * {@link EmailSender} application port의 {@link JavaMailSender} 기반 구현체
 *
 * 원문 코드/토큰을 로그에 남기지 않는다(FR-033) — 메일 발송 실패 시에도 예외 메시지에 원문을 포함하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class JavaMailEmailSenderAdapter implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("이메일 인증 코드 안내");
        message.setText("인증 코드: " + code + "\n5분 이내에 입력해주세요.");
        javaMailSender.send(message);
    }

    @Override
    public void sendRecoveryMagicLink(String email, String magicLinkUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("상담 복원 링크 안내");
        message.setText("아래 링크로 기존 상담을 복원할 수 있습니다.\n" + magicLinkUrl + "\n15분 이내에 사용해주세요.");
        javaMailSender.send(message);
    }
}
