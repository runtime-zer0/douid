package kr.douid.brand.work.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.client.application.ClientAuthenticationService;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;
import kr.douid.brand.work.application.query.PublicWorkDetail;
import kr.douid.brand.work.application.query.PublicWorkListItem;
import kr.douid.brand.work.application.query.WorkCategoryView;
import kr.douid.brand.work.application.query.WorkMediaView;
import kr.douid.brand.work.application.query.WorkQueryService;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkNotFoundException;

@WebMvcTest(PublicWorkController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PublicWorkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private ClientAuthenticationService clientAuthenticationService;

    @MockitoBean
    private WorkQueryService workQueryService;

    @Test
    void findAll_공개_목록_200_관리_전용_필드_없음() throws Exception {
        WorkCategoryView category = new WorkCategoryView(1L, "브랜딩", "branding", true);
        WorkMediaView thumbnail = new WorkMediaView(10L, WorkMediaRole.THUMBNAIL, 0, "대표 이미지",
                "media/thumb.png", "thumb.png");
        PublicWorkListItem item = new PublicWorkListItem("브랜드 리뉴얼", "brand-renewal", "요약",
                category, thumbnail);

        given(workQueryService.getPublicWorkList(any()))
                .willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/public/works"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].slug").value("brand-renewal"))
                .andExpect(jsonPath("$.data.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].visibility").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].category.id").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].thumbnail.mediaId").doesNotExist());
    }

    @Test
    void findBySlug_공개_상세_200_관리_전용_필드_없음() throws Exception {
        WorkCategoryView category = new WorkCategoryView(1L, "브랜딩", "branding", true);
        WorkMediaView mediaItem = new WorkMediaView(10L, WorkMediaRole.DETAIL_IMAGE, 0, "상세 이미지",
                "media/detail.png", "detail.png");
        PublicWorkDetail detail = new PublicWorkDetail("브랜드 리뉴얼", "brand-renewal", "요약", "상세",
                category, List.of(mediaItem));

        given(workQueryService.getPublicWorkDetail("brand-renewal")).willReturn(detail);

        mockMvc.perform(get("/api/public/works/brand-renewal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("브랜드 리뉴얼"))
                .andExpect(jsonPath("$.data.mediaItems[0].altText").value("상세 이미지"))
                .andExpect(jsonPath("$.data.mediaItems[0].sortOrder").doesNotExist());
    }

    @Test
    void findBySlug_미존재_404() throws Exception {
        willThrow(new WorkNotFoundException()).given(workQueryService).getPublicWorkDetail(anyString());

        mockMvc.perform(get("/api/public/works/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("WORK_NOT_FOUND"));
    }

    @Test
    void findBySlug_Work_비공개_404() throws Exception {
        willThrow(new WorkNotFoundException()).given(workQueryService).getPublicWorkDetail("hidden-work");

        mockMvc.perform(get("/api/public/works/hidden-work"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("WORK_NOT_FOUND"));
    }

    @Test
    void findBySlug_Category_비공개_404_동일_응답형태() throws Exception {
        willThrow(new WorkNotFoundException()).given(workQueryService).getPublicWorkDetail("hidden-category-work");

        mockMvc.perform(get("/api/public/works/hidden-category-work"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("WORK_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void findAllByCategory_카테고리별_목록_200() throws Exception {
        WorkCategoryView category = new WorkCategoryView(1L, "브랜딩", "branding", true);
        PublicWorkListItem item = new PublicWorkListItem("브랜드 리뉴얼", "brand-renewal", "요약", category, null);

        given(workQueryService.getPublicWorkListByCategory(any(), any()))
                .willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/public/categories/branding/works"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void findAllByCategory_존재하지않는_카테고리_200_빈목록() throws Exception {
        given(workQueryService.getPublicWorkListByCategory(any(), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/public/categories/missing-category/works"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }
}
