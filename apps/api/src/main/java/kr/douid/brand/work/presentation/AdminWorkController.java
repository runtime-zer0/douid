package kr.douid.brand.work.presentation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.douid.brand.shared.pagination.PageResponse;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.work.application.query.WorkQueryService;
import kr.douid.brand.work.presentation.response.AdminWorkDetailResponse;
import kr.douid.brand.work.presentation.response.AdminWorkListResponse;
import lombok.RequiredArgsConstructor;

/**
 * 작업물 관리자 Query API 컨트롤러
 *
 * 공개 여부와 무관하게 모든 작업물을 조회한다. 생성/수정/삭제 등 Command는 {@link WorkController}가 담당한다.
 */
@RestController
@RequestMapping("/api/admin/works")
@RequiredArgsConstructor
public class AdminWorkController {

    private static final String MEDIA_BASE_URL = "/api/media";

    private final WorkQueryService workQueryService;

    /**
     * 관리자 작업물 목록 조회 요청을 처리
     *
     * @param pageable 페이지네이션 파라미터 (기본: createdAt 내림차순, 20개)
     * @return 관리자 작업물 목록 응답
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminWorkListResponse>>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AdminWorkListResponse> response = PageResponse.from(
                workQueryService.getAdminWorkList(pageable)
                        .map(item -> AdminWorkListResponse.from(item, MEDIA_BASE_URL)));

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 관리자 작업물 상세 조회 요청을 처리
     *
     * @param id 조회할 작업물 ID
     * @return 관리자 작업물 상세 응답
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminWorkDetailResponse>> findById(@PathVariable Long id) {
        AdminWorkDetailResponse response = AdminWorkDetailResponse.from(
                workQueryService.getAdminWorkDetail(id), MEDIA_BASE_URL);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
