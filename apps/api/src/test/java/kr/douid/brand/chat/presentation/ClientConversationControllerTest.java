package kr.douid.brand.chat.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import kr.douid.brand.chat.application.ConversationLookupService;
import kr.douid.brand.chat.application.ConversationStartService;
import kr.douid.brand.chat.application.MessageSendService;
import kr.douid.brand.chat.application.command.StartConversationResult;
import kr.douid.brand.chat.application.query.ConversationMessageQueryService;
import kr.douid.brand.chat.application.query.MessageView;
import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationAccessDeniedException;
import kr.douid.brand.chat.domain.ConversationClosedException;
import kr.douid.brand.chat.domain.ConversationNotFoundException;
import kr.douid.brand.chat.domain.ConversationStatus;
import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.client.application.ClientAuthenticationService;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;
import kr.douid.brand.shared.security.ClientTokenCookieProvider;

@WebMvcTest(ClientConversationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithAnonymousUser
class ClientConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private ClientAuthenticationService clientAuthenticationService;

    @MockitoBean
    private ConversationStartService conversationStartService;

    @MockitoBean
    private ConversationLookupService conversationLookupService;

    @MockitoBean
    private ConversationMessageQueryService conversationMessageQueryService;

    @MockitoBean
    private MessageSendService messageSendService;

    @MockitoBean
    private ClientTokenCookieProvider clientTokenCookieProvider;

    @Test
    void start_쿠키없음_상담시작_클라이언트토큰쿠키발급() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(conversationStartService.start(Optional.empty()))
                .willReturn(new StartConversationResult(conversationId, ConversationStatus.OPEN, false, "raw-token"));
        given(clientTokenCookieProvider.issueCookie("raw-token"))
                .willReturn(ResponseCookie.from("client_token", "raw-token").httpOnly(true).build());

        mockMvc.perform(post("/api/client/conversations").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.data.resumed").value(false))
                .andExpect(header().string("Set-Cookie", containsString("client_token=raw-token")));
    }

    @Test
    void start_유효한클라이언트토큰_상담복원_쿠키재발급없음() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));
        given(conversationStartService.start(Optional.of(1L)))
                .willReturn(new StartConversationResult(conversationId, ConversationStatus.OPEN, true, null));

        mockMvc.perform(post("/api/client/conversations").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumed").value(true))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void start_활성상담없는기존클라이언트_새상담생성_쿠키재발급없음() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));
        given(conversationStartService.start(Optional.of(1L)))
                .willReturn(new StartConversationResult(conversationId, ConversationStatus.OPEN, false, null));

        mockMvc.perform(post("/api/client/conversations").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumed").value(false))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void find_타인의클라이언트토큰_403() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(2L));
        given(conversationLookupService.getOwned(eq(conversationId), eq(Optional.of(2L))))
                .willThrow(new ConversationAccessDeniedException());

        mockMvc.perform(get("/api/client/conversations/" + conversationId)
                        .cookie(new Cookie("client_token", "other-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.code").value("CONVERSATION_ACCESS_DENIED"));
    }

    @Test
    void find_클라이언트토큰없음_403() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(conversationLookupService.getOwned(eq(conversationId), eq(Optional.empty())))
                .willThrow(new ConversationAccessDeniedException());

        mockMvc.perform(get("/api/client/conversations/" + conversationId))
                .andExpect(status().isForbidden());
    }

    @Test
    void find_존재하지않는상담_404() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(conversationLookupService.getOwned(eq(conversationId), any()))
                .willThrow(new ConversationNotFoundException());

        mockMvc.perform(get("/api/client/conversations/" + conversationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void findMessages_ownership불일치_403() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(conversationLookupService.getOwned(eq(conversationId), any()))
                .willThrow(new ConversationAccessDeniedException());

        mockMvc.perform(get("/api/client/conversations/" + conversationId + "/messages"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendMessage_정상전송_201() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.of(1L));
        given(messageSendService.send(eq(conversationId), eq(Optional.of(1L)), anyString()))
                .willReturn(new MessageView(1L, "안녕하세요", LocalDateTime.now()));

        mockMvc.perform(post("/api/client/conversations/" + conversationId + "/messages").with(csrf())
                        .cookie(new Cookie("client_token", "valid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("안녕하세요"));
    }

    @Test
    void sendMessage_빈내용_400() throws Exception {
        UUID conversationId = UUID.randomUUID();

        mockMvc.perform(post("/api/client/conversations/" + conversationId + "/messages").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_종료된상담_409() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(messageSendService.send(eq(conversationId), any(), anyString()))
                .willThrow(new ConversationClosedException());

        mockMvc.perform(post("/api/client/conversations/" + conversationId + "/messages").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("CONVERSATION_CLOSED"));
    }

    @Test
    void sendMessage_ownership불일치_403() throws Exception {
        UUID conversationId = UUID.randomUUID();
        given(messageSendService.send(eq(conversationId), any(), anyString()))
                .willThrow(new ConversationAccessDeniedException());

        mockMvc.perform(post("/api/client/conversations/" + conversationId + "/messages").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\"}"))
                .andExpect(status().isForbidden());
    }
}
