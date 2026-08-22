package kr.douid.brand.work.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;
import kr.douid.brand.work.application.query.AdminWorkDetail;
import kr.douid.brand.work.application.query.AdminWorkListItem;
import kr.douid.brand.work.application.query.WorkCategoryView;
import kr.douid.brand.work.application.query.WorkMediaView;
import kr.douid.brand.work.application.query.WorkQueryService;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkNotFoundException;
import kr.douid.brand.work.domain.WorkVisibility;

@WebMvcTest(AdminWorkController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
class AdminWorkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private WorkQueryService workQueryService;

    @Test
    void findAll_목록_200() throws Exception {
        WorkCategoryView category = new WorkCategoryView(1L, "브랜딩", "branding", true);
        WorkMediaView thumbnail = new WorkMediaView(10L, WorkMediaRole.THUMBNAIL, 0, "대표 이미지",
                "media/thumb.png", "thumb.png");
        AdminWorkListItem item = new AdminWorkListItem(1L, "브랜드 리뉴얼", "brand-renewal",
                WorkVisibility.HIDDEN, category, thumbnail, LocalDateTime.now(), LocalDateTime.now());

        given(workQueryService.getAdminWorkList(any()))
                .willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/works"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].visibility").value("HIDDEN"))
                .andExpect(jsonPath("$.data.content[0].category.id").value(1))
                .andExpect(jsonPath("$.data.content[0].thumbnail.mediaId").value(10));
    }

    @Test
    void findAll_페이지_파라미터_반영() throws Exception {
        given(workQueryService.getAdminWorkList(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/admin/works").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    @Test
    void findById_상세_200() throws Exception {
        WorkCategoryView category = new WorkCategoryView(1L, "브랜딩", "branding", false);
        WorkMediaView mediaItem = new WorkMediaView(10L, WorkMediaRole.DETAIL_IMAGE, 0, "상세 이미지",
                "media/detail.png", "detail.png");
        AdminWorkDetail detail = new AdminWorkDetail(1L, "브랜드 리뉴얼", "brand-renewal", "요약", "상세",
                WorkVisibility.HIDDEN, category, List.of(mediaItem), LocalDateTime.now(), LocalDateTime.now());

        given(workQueryService.getAdminWorkDetail(1L)).willReturn(detail);

        mockMvc.perform(get("/api/admin/works/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("brand-renewal"))
                .andExpect(jsonPath("$.data.category.visible").doesNotExist())
                .andExpect(jsonPath("$.data.mediaItems[0].role").value("DETAIL_IMAGE"));
    }

    @Test
    void findById_미존재_404() throws Exception {
        willThrow(new WorkNotFoundException()).given(workQueryService).getAdminWorkDetail(anyLong());

        mockMvc.perform(get("/api/admin/works/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("WORK_NOT_FOUND"));
    }

    @Test
    @WithAnonymousUser
    void findAll_미인증_401() throws Exception {
        mockMvc.perform(get("/api/admin/works"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }
}
