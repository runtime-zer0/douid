package kr.douid.brand.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import tools.jackson.databind.json.JsonMapper;

class CustomLoginFailureHandlerTest {

    private final CustomLoginFailureHandler failureHandler =
            new CustomLoginFailureHandler(JsonMapper.builder().build());

    @Test
    void 계정_미존재_비밀번호_불일치_비활성_계정_모두_동일한_401_응답() throws Exception {
        String accountNotFoundBody = writeFailure(new UsernameNotFoundException("unknown@douid.kr"));
        String badCredentialsBody = writeFailure(new BadCredentialsException("bad credentials"));
        String disabledBody = writeFailure(new DisabledException("disabled"));

        assertThat(accountNotFoundBody)
                .isEqualTo(badCredentialsBody)
                .isEqualTo(disabledBody);
    }

    @Test
    void 실패_응답은_401_상태와_인증실패_코드를_포함() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("bad credentials"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("AUTHENTICATION_FAILED");
    }

    private String writeFailure(org.springframework.security.core.AuthenticationException exception)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, exception);

        return response.getContentAsString();
    }
}
