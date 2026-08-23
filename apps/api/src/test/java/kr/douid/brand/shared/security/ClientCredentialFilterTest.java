package kr.douid.brand.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.Cookie;
import kr.douid.brand.client.application.ClientAuthenticationService;

class ClientCredentialFilterTest {

    private final ClientAuthenticationService clientAuthenticationService = mock(ClientAuthenticationService.class);
    private final ClientCredentialFilter filter = new ClientCredentialFilter(clientAuthenticationService);

    @Test
    void doFilter_무효한_토큰_요청차단하지않고_컨텍스트비어있음() throws Exception {
        given(clientAuthenticationService.resolve(anyString())).willReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("client_token", "invalid-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(ClientIdentityContext.get()).isEmpty();
        verify(clientAuthenticationService).resolve("invalid-token");
    }

    @Test
    void doFilter_쿠키없음_요청통과_컨텍스트비어있음() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(ClientIdentityContext.get()).isEmpty();
    }
}
