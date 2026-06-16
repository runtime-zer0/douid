package kr.douid.brand.category.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kr.douid.brand.category.application.CategoryQueryService;
import kr.douid.brand.category.application.CategoryResult;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.exception.GlobalExceptionHandler;

@WebMvcTest(PublicCategoryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryQueryService categoryQueryService;

    @Test
    void findAll_공개_목록_200() throws Exception {
        given(categoryQueryService.findAllVisible()).willReturn(List.of(
                fakeCategoryResult(1L, "브랜딩", "branding", 1, true)
        ));

        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].visible").value(true));
    }

    @Test
    void findAll_빈_목록_200() throws Exception {
        given(categoryQueryService.findAllVisible()).willReturn(List.of());

        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private CategoryResult fakeCategoryResult(Long id, String name, String slug, int displayOrder, boolean visible) {
        return new CategoryResult(id, name, slug, displayOrder, visible, LocalDateTime.now(), LocalDateTime.now());
    }
}
