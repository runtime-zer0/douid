package kr.douid.brand.work.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kr.douid.brand.category.domain.CategoryNotFoundException;
import kr.douid.brand.media.domain.MediaNotFoundException;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;
import kr.douid.brand.work.application.command.WorkCommandService;
import kr.douid.brand.work.application.command.WorkResult;
import kr.douid.brand.work.domain.WorkNotFoundException;
import kr.douid.brand.work.domain.WorkSlugDuplicateException;
import kr.douid.brand.work.domain.WorkVisibility;

@WebMvcTest(WorkController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser
class WorkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkCommandService workCommandService;

    @Test
    void create_정상_201() throws Exception {
        given(workCommandService.create(any()))
                .willReturn(new WorkResult(1L, WorkVisibility.VISIBLE));

        mockMvc.perform(post("/api/admin/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"브랜드 리뉴얼",
                                  "slug":"brand-renewal",
                                  "summary":"요약",
                                  "description":"상세",
                                  "categoryId":1,
                                  "visibility":"VISIBLE",
                                  "mediaItems":[
                                    {"mediaId":10,"role":"THUMBNAIL","sortOrder":0,"altText":"대표 이미지"}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    void create_슬러그_중복_409() throws Exception {
        given(workCommandService.create(any()))
                .willThrow(new WorkSlugDuplicateException());

        mockMvc.perform(post("/api/admin/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("WORK_SLUG_DUPLICATE"));
    }

    @Test
    void create_카테고리_미존재_404() throws Exception {
        given(workCommandService.create(any()))
                .willThrow(new CategoryNotFoundException());

        mockMvc.perform(post("/api/admin/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void create_미디어_미존재_404() throws Exception {
        given(workCommandService.create(any()))
                .willThrow(new MediaNotFoundException());

        mockMvc.perform(post("/api/admin/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("MEDIA_NOT_FOUND"));
    }

    @Test
    void create_제목_빈값_400() throws Exception {
        mockMvc.perform(post("/api/admin/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","slug":"brand-renewal","visibility":"VISIBLE","mediaItems":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("INVALID_INPUT"));
    }

    @Test
    void update_정상_200() throws Exception {
        given(workCommandService.update(any()))
                .willReturn(new WorkResult(1L, WorkVisibility.HIDDEN));

        mockMvc.perform(put("/api/admin/works/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    void update_미존재_404() throws Exception {
        given(workCommandService.update(any()))
                .willThrow(new WorkNotFoundException());

        mockMvc.perform(put("/api/admin/works/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("WORK_NOT_FOUND"));
    }

    @Test
    void delete_정상_204() throws Exception {
        willDoNothing().given(workCommandService).delete(anyLong());

        mockMvc.perform(delete("/api/admin/works/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_미존재_404() throws Exception {
        willThrow(new WorkNotFoundException()).given(workCommandService).delete(99L);

        mockMvc.perform(delete("/api/admin/works/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("WORK_NOT_FOUND"));
    }

    @Test
    void changeVisibility_정상_200() throws Exception {
        given(workCommandService.changeVisibility(any()))
                .willReturn(new WorkResult(1L, WorkVisibility.VISIBLE));

        mockMvc.perform(patch("/api/admin/works/1/visibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"visibility":"VISIBLE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("VISIBLE"));
    }

    @Test
    @WithAnonymousUser
    void create_미인증_401() throws Exception {
        mockMvc.perform(post("/api/admin/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    private String validCreateJson() {
        return """
                {
                  "title":"브랜드 리뉴얼",
                  "slug":"brand-renewal",
                  "summary":"요약",
                  "description":"상세",
                  "categoryId":1,
                  "visibility":"VISIBLE",
                  "mediaItems":[
                    {"mediaId":10,"role":"THUMBNAIL","sortOrder":0,"altText":"대표 이미지"}
                  ]
                }
                """;
    }
}
