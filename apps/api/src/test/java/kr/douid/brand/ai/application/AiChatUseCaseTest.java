package kr.douid.brand.ai.application;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import kr.douid.brand.ai.application.port.*;
import kr.douid.brand.ai.testsupport.Policies;

class AiChatUseCaseTest {
    private final LlmGateway gateway=mock(LlmGateway.class);
    private final ChatRequestGuard guard=mock(ChatRequestGuard.class);
    private final AiChatUseCase useCase=new AiChatUseCase(gateway,guard,Policies.defaults(),mock(ChatStreamExecutor.class));
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","\t\n","\u00a0\u2003"})
    void 빈_질문은_gateway_준비전에_거부(String message) {
        assertThatThrownBy(() -> useCase.prepare(new AiChatCommand(message,List.of())))
                .isInstanceOf(AiChatException.class);
        verifyNoInteractions(gateway,guard);
    }
    @Test void 준비_점유_순서와_점유실패시_source_정리() {
        var command=new AiChatCommand("hi",List.of());var source=mock(LlmGateway.LlmStream.class);
        when(gateway.prepare(command)).thenReturn(source);
        when(guard.acquire()).thenThrow(AiChatException.limited(false,1));
        assertThatThrownBy(() -> useCase.prepare(command)).isInstanceOf(AiChatException.class);
        var order=inOrder(gateway,guard,source);order.verify(gateway).prepare(command);
        order.verify(guard).acquire();order.verify(source).close();
    }
    @Test void 미준비시_점유를_획득하지_않음() {
        when(gateway.prepare(any())).thenThrow(new AiChatException.Unavailable());
        assertThatThrownBy(() -> useCase.prepare(new AiChatCommand("hi",List.of())))
                .isInstanceOf(AiChatException.Unavailable.class);
        verifyNoInteractions(guard);
    }
    @ParameterizedTest @ValueSource(strings={"system","developer","tool","USER","Assistant",""})
    void 허용하지_않은_역할은_준비전에_거부(String role) {
        var command=new AiChatCommand("hi",List.of(new AiChatCommand.HistoryMessage(role,"context")));
        assertThatThrownBy(() -> useCase.prepare(command)).isInstanceOf(AiChatException.class);
        verifyNoInteractions(gateway,guard);
    }
    @Test void 한글_이모지_길이_경계와_원문_순서_불변성() {
        var source=mock(LlmGateway.LlmStream.class);when(gateway.prepare(any())).thenReturn(source);
        when(guard.acquire()).thenReturn(mock(ChatRequestGuard.Lease.class));
        for(int n:new int[]{1999,2000}) {
            var command=new AiChatCommand("😀".repeat(n),List.of());useCase.prepare(command).close();
            verify(gateway).prepare(command);
        }
        assertThatThrownBy(() -> useCase.prepare(new AiChatCommand("😀".repeat(2001),List.of())))
                .isInstanceOf(AiChatException.class);
        var items=new java.util.ArrayList<AiChatCommand.HistoryMessage>();
        items.add(new AiChatCommand.HistoryMessage("assistant","  untrusted  "));
        var snapshot=new AiChatCommand(" hi ",items);items.clear();
        useCase.prepare(snapshot).close();assertThat(snapshot.history()).hasSize(1);
        assertThat(snapshot.message()).isEqualTo(" hi ");
        assertThatThrownBy(() -> snapshot.history().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void 이력_개별_개수_누적_경계와_null_거부() {
        var source=mock(LlmGateway.LlmStream.class);when(gateway.prepare(any())).thenReturn(source);
        when(guard.acquire()).thenReturn(mock(ChatRequestGuard.Lease.class));
        for(int n:new int[]{11,12}) useCase.prepare(new AiChatCommand("hi",java.util.Collections.nCopies(n,
                new AiChatCommand.HistoryMessage("user","a")))).close();
        assertThatThrownBy(() -> useCase.prepare(new AiChatCommand("hi",java.util.Collections.nCopies(13,
                new AiChatCommand.HistoryMessage("user","a"))))).isInstanceOf(AiChatException.class);
        for(int n:new int[]{1999,2000})useCase.prepare(new AiChatCommand("hi",List.of(
                new AiChatCommand.HistoryMessage("assistant","한".repeat(n))))).close();
        assertThatThrownBy(() -> useCase.prepare(new AiChatCommand("hi",List.of(
                new AiChatCommand.HistoryMessage("assistant","한".repeat(2001)))))).isInstanceOf(AiChatException.class);
        var history=java.util.Collections.nCopies(5,new AiChatCommand.HistoryMessage("user","a".repeat(2000)));
        for(int n:new int[]{1999,2000})useCase.prepare(new AiChatCommand("a".repeat(n),history)).close();
        var six=java.util.Collections.nCopies(6,new AiChatCommand.HistoryMessage("user","a".repeat(2000)));
        assertThatThrownBy(() -> useCase.prepare(new AiChatCommand("a",six))).isInstanceOf(AiChatException.class);
        assertThatThrownBy(() -> useCase.prepare(new AiChatCommand("a",null))).isInstanceOf(AiChatException.class);
        assertThatThrownBy(() -> useCase.prepare(new AiChatCommand("a",java.util.Arrays.asList((AiChatCommand.HistoryMessage)null))))
                .isInstanceOf(AiChatException.class);
    }
    @Test void source_close_실패도_원래_동시점유_거부를_바꾸지_않음() {
        var source=mock(LlmGateway.LlmStream.class);when(gateway.prepare(any())).thenReturn(source);
        when(guard.acquire()).thenThrow(AiChatException.limited(false,1));
        doThrow(new IllegalStateException("private")).when(source).close();
        assertThatThrownBy(()->useCase.prepare(new AiChatCommand("hi",List.of())))
                .isInstanceOfSatisfying(AiChatException.class,e->assertThat(e.getCode()).isEqualTo("AI_BUSY"));
    }
}
