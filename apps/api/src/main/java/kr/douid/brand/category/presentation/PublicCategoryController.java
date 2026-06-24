package kr.douid.brand.category.presentation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.douid.brand.category.application.query.CategoryQueryService;
import kr.douid.brand.category.presentation.response.CategoryListResponse;
import kr.douid.brand.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryQueryService categoryQueryService;

    /**
     * 공개 카테고리 목록 조회 요청을 처리
     *
     * @return 공개 카테고리 목록 응답
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryListResponse>>> findAll() {
        List<CategoryListResponse> response = categoryQueryService.getPublicCategoryList().stream()
                .map(CategoryListResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
