package kr.douid.brand.ai.presentation;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import kr.douid.brand.ai.testsupport.*;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({AiChatTestConfiguration.class,AiChatStatelessIntegrationTest.ObserveSql.class})
class AiChatStatelessIntegrationTest extends PostgresIntegrationTest {
    static final List<String> TABLES=List.of("client_identity","client_credential","client_email",
            "email_verification_challenge","client_recovery_token","conversation","message");
    static final AtomicInteger writes=new AtomicInteger();
    @LocalServerPort int port;
    @Autowired ControlledLlmGateway gateway;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    Map<String,Long> counts() {
        Map<String,Long> result=new TreeMap<>();
        TABLES.forEach(table->result.put(table,jdbc.queryForObject("select count(*) from "+table,Long.class)));
        return result;
    }
    @Test void 실제adapter_대역의_정상과_실패에도_채팅_DML과_행수변경_없음() throws Exception {
        var before=counts();writes.set(0);
        try(var stub=new OpenAiStubServer();var client=HttpClient.newHttpClient();
            var executor=new kr.douid.brand.ai.infrastructure.stream.BoundedChatStreamExecutor(2)) {
            var properties=new kr.douid.brand.ai.infrastructure.config.OpenAiProperties();
            properties.setEnabled(true);properties.setApiKey("FAKE_KEY_CANARY");
            var adapter=kr.douid.brand.ai.infrastructure.openai.OpenAiResponsesGateway.forLoopbackTest(properties,client,executor,
                    new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),stub.endpoint());
            var policy=Policies.defaults();
            var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new AiChatController(
                    new kr.douid.brand.ai.application.AiChatUseCase(adapter,
                            new kr.douid.brand.ai.infrastructure.guard.InMemoryChatRequestGuard(policy,System::nanoTime),policy,executor),policy))
                    .addInterceptors(new kr.douid.brand.ai.presentation.stream.AiChatAsyncInterceptor()).build();
            for(boolean fail:new boolean[]{false,true}) {
                stub.generation=e->OpenAiStubServer.reply(e,fail?500:200,fail?"application/json":"text/event-stream",
                        fail?"PRIVATE_ERROR":OpenAiStubServer.success("ephemeral-answer"));
                var result=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/public/ai-chat/stream")
                        .contentType("application/json").content("{\"message\":\"ephemeral-question\"}")).andReturn();
                result.getAsyncResult(5000);
                String body=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(result))
                        .andReturn().getResponse().getContentAsString();
                assertThat(body).contains(fail?"event:error":"event:completed");
            }
        }
        assertThat(counts()).isEqualTo(before);assertThat(writes.get()).isZero();
    }
    @Test void 정상과_실패_요청의_행수불변과_대상_DML실행_0회() throws Exception {
        var before=counts();writes.set(0);
        try(var client=HttpClient.newHttpClient()) {
            for(boolean fail:new boolean[]{false,true}) {
                gateway.behavior=(s,d)->{d.accept("ephemeral-answer");if(fail)throw new RuntimeException("private");};
                var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/public/ai-chat/stream"))
                        .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(
                                "{\"message\":\"ephemeral-question\",\"history\":[{\"role\":\"assistant\",\"content\":\"untrusted\"}]}" )).build();
                var response=client.send(request,HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains(fail?"event:error":"event:completed");
                assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
            }
        }
        assertThat(counts()).isEqualTo(before);assertThat(writes.get()).isZero();
    }
    @Test void SQL관찰은_rollback된_DML도_탐지() {
        var before=counts();writes.set(0);
        new TransactionTemplate(transactions).executeWithoutResult(status->{
            jdbc.update("insert into client_identity (created_at) values (current_timestamp)");
            status.setRollbackOnly();
        });
        assertThat(counts()).isEqualTo(before);assertThat(writes.get()).isEqualTo(1);
        writes.set(0);
    }
    @Test void 새_채팅_테이블이나_엔티티_repository_migration_없음() throws IOException {
        var root=java.nio.file.Path.of("src/main");
        try(var files=java.nio.file.Files.walk(root.resolve("java/kr/douid/brand/ai"))) {
            assertThat(files.filter(p->p.toString().endsWith(".java")).map(p->{
                try{return java.nio.file.Files.readString(p);}catch(IOException e){throw new UncheckedIOException(e);}
            }).toList()).noneMatch(s->s.contains("@Entity") || s.contains("JpaRepository") || s.contains("JdbcTemplate"));
        }
        try(var files=java.nio.file.Files.list(root.resolve("resources/db/migration"))) {
            assertThat(files.map(p->p.getFileName().toString()).toList()).containsExactly("V1__baseline_schema.sql");
        }
    }
    @TestConfiguration(proxyBeanMethods=false)
    static class ObserveSql {
        @Bean static BeanPostProcessor sqlObserver() {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean,String name) {
                    if(!(bean instanceof DataSource dataSource))return bean;
                    return new DelegatingDataSource(dataSource) {
                        @Override public Connection getConnection() throws SQLException {return connection(super.getConnection());}
                        @Override public Connection getConnection(String username,String password) throws SQLException {
                            return connection(super.getConnection(username,password));
                        }
                    };
                }
            };
        }
        static Connection connection(Connection target) {
            return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},
                    (proxy,method,args)->{
                        Object result=invoke(target,method,args);
                        if(result instanceof Statement statement) {
                            String sql=args!=null && args.length>0 && args[0] instanceof String text?text:null;
                            Class<?> type=result instanceof CallableStatement?CallableStatement.class:
                                    result instanceof PreparedStatement?PreparedStatement.class:Statement.class;
                            return java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
                                if(m.getName().startsWith("execute")) {
                                    String executed=sql!=null?sql:a!=null && a.length>0 && a[0] instanceof String text?text:null;
                                    if(executed!=null && mutation(executed))writes.incrementAndGet();
                                }
                                return invoke(statement,m,a);
                            });
                        }
                        return result;
                    });
        }
        static boolean mutation(String sql) {
            String normalized=sql.stripLeading().toLowerCase(Locale.ROOT);
            if(!(normalized.startsWith("insert ")||normalized.startsWith("update ")||normalized.startsWith("delete ")))return false;
            return TABLES.stream().anyMatch(table->normalized.matches("(?s).*\\b"+table+"\\b.*"));
        }
        static Object invoke(Object target,Method method,Object[] args)throws Throwable {
            try{return method.invoke(target,args);}catch(InvocationTargetException e){throw e.getCause();}
        }
    }
}
