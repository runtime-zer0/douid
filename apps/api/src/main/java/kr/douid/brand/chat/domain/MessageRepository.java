package kr.douid.brand.chat.domain;

/**
 * {@link Message} 저장을 위한 domain repository port
 */
public interface MessageRepository {

    /**
     * 메시지를 저장하고 반환
     *
     * @param message 저장할 메시지
     * @return 저장된 메시지
     */
    Message save(Message message);
}
