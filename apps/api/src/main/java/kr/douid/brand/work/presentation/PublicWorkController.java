package kr.douid.brand.work.presentation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import kr.douid.brand.shared.pagination.PageResponse;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.work.application.query.WorkQueryService;
import kr.douid.brand.work.presentation.response.PublicWorkDetailResponse;
import kr.douid.brand.work.presentation.response.PublicWorkListResponse;
import lombok.RequiredArgsConstructor;

/**
 * 작업물 공개 Query API 컨트롤러
 *
 * Public Visibility Policy(Work 공개 AND Category 공개)를 만족하는 작업물만 조회한다.
 */
@RestController
@RequiredArgsConstructor
public class PublicWorkController {

    private static final String MEDIA_BASE_URL = "/api/media";

    private final WorkQueryService workQueryService;

    /**
     * 공개 작업물 목록 조회 요청을 처리
     *
     * @param pageable 페이지네이션 파라미터 (기본: createdAt 내림차순, 20개)
     * @return 공개 작업물 목록 응답
     */
    @GetMapping("/api/public/works")
    public ResponseEntity<ApiResponse<PageResponse<PublicWorkListResponse>>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PublicWorkListResponse> response = PageResponse.from(
                workQueryService.getPublicWorkList(pageable)
                        .map(item -> PublicWorkListResponse.from(item, MEDIA_BASE_URL)));

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 공개 작업물 상세 조회 요청을 처리
     *
     * @param slug 조회할 작업물 슬러그
     * @return 공개 작업물 상세 응답
     */
    @GetMapping("/api/public/works/{slug}")
    public ResponseEntity<ApiResponse<PublicWorkDetailResponse>> findBySlug(@PathVariable String slug) {
        PublicWorkDetailResponse response = PublicWorkDetailResponse.from(
                workQueryService.getPublicWorkDetail(slug), MEDIA_BASE_URL);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 공개 카테고리 기준 작업물 목록 조회 요청을 처리
     *
     * @param categorySlug 조회할 카테고리 슬러그
     * @param pageable     페이지네이션 파라미터 (기본: createdAt 내림차순, 20개)
     * @return 공개 작업물 목록 응답 (카테고리 미존재/비공개면 빈 목록)
     */
    @GetMapping("/api/public/categories/{categorySlug}/works")
    public ResponseEntity<ApiResponse<PageResponse<PublicWorkListResponse>>> findAllByCategory(
            @PathVariable String categorySlug,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PublicWorkListResponse> response = PageResponse.from(
                workQueryService.getPublicWorkListByCategory(categorySlug, pageable)
                        .map(item -> PublicWorkListResponse.from(item, MEDIA_BASE_URL)));

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
