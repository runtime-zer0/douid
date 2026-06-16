package kr.douid.brand.category.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kr.douid.brand.category.application.CategoryCommandService;
import kr.douid.brand.category.application.CategoryQueryService;
import kr.douid.brand.category.application.CategoryResult;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.exception.BusinessException;
import kr.douid.brand.shared.exception.ErrorCode;
import kr.douid.brand.shared.exception.GlobalExceptionHandler;

@WebMvcTest(AdminCategoryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryCommandService categoryCommandService;

    @MockitoBean
    private CategoryQueryService categoryQueryService;

    @Test
    void create_정상_201() throws Exception {
        given(categoryCommandService.createCategory(any()))
                .willReturn(fakeCategoryResult(1L, "브랜딩", "branding", 1, true));

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"브랜딩","slug":"branding","displayOrder":1,"visible":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.slug").value("branding"));
    }

    @Test
    void create_슬러그_중복_409() throws Exception {
        given(categoryCommandService.createCategory(any()))
                .willThrow(new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATE));

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"브랜딩","slug":"branding","displayOrder":1,"visible":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("CATEGORY_SLUG_DUPLICATE"));
    }

    @Test
    void create_이름_빈값_400() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","slug":"branding","displayOrder":1,"visible":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("INVALID_INPUT"));
    }

    @Test
    void update_정상_200() throws Exception {
        given(categoryCommandService.updateCategory(eq(1L), any()))
                .willReturn(fakeCategoryResult(1L, "UX", "ux", 2, false));

        mockMvc.perform(put("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"UX","slug":"ux","displayOrder":2,"visible":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("UX"));
    }

    @Test
    void update_미존재_404() throws Exception {
        given(categoryCommandService.updateCategory(eq(99L), any()))
                .willThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(put("/api/admin/categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"UX","slug":"ux","displayOrder":2,"visible":false}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void delete_정상_204() throws Exception {
        willDoNothing().given(categoryCommandService).deleteCategory(1L);

        mockMvc.perform(delete("/api/admin/categories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_미존재_404() throws Exception {
        willThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND))
                .given(categoryCommandService).deleteCategory(99L);

        mockMvc.perform(delete("/api/admin/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void findAll_목록_200() throws Exception {
        given(categoryQueryService.findAllForAdmin()).willReturn(List.of(
                fakeCategoryResult(1L, "브랜딩", "branding", 1, true),
                fakeCategoryResult(2L, "내부", "internal", 2, false)
        ));

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    private CategoryResult fakeCategoryResult(Long id, String name, String slug, int displayOrder, boolean visible) {
        return new CategoryResult(id, name, slug, displayOrder, visible, LocalDateTime.now(), LocalDateTime.now());
    }
}
