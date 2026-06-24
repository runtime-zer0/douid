package kr.douid.brand.category.presentation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.douid.brand.category.application.command.CategoryCommandService;
import kr.douid.brand.category.application.command.DeleteCategoryCommand;
import kr.douid.brand.category.application.query.CategoryQueryService;
import kr.douid.brand.category.presentation.request.CreateCategoryRequest;
import kr.douid.brand.category.presentation.request.UpdateCategoryRequest;
import kr.douid.brand.category.presentation.response.CategoryCommandResponse;
import kr.douid.brand.category.presentation.response.CategoryListResponse;
import kr.douid.brand.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryCommandService categoryCommandService;
    private final CategoryQueryService categoryQueryService;

    /**
     * 관리자 카테고리 생성 요청을 처리
     *
     * @param request 카테고리 생성 요청값
     * @return 생성된 카테고리 응답
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryCommandResponse>> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryCommandResponse response = CategoryCommandResponse.from(
                categoryCommandService.createCategory(request.toCommand()));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 관리자 카테고리 수정 요청을 처리
     *
     * @param id 수정할 카테고리 ID
     * @param request 카테고리 수정 요청값
     * @return 수정된 카테고리 응답
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryCommandResponse>> update(@PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryCommandResponse response = CategoryCommandResponse.from(
                categoryCommandService.updateCategory(request.toCommand(id)));

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 관리자 카테고리 삭제 요청을 처리
     *
     * @param id 삭제할 카테고리 ID
     * @return 빈 성공 응답
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryCommandService.deleteCategory(DeleteCategoryCommand.of(id));

        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 관리자 카테고리 목록 조회 요청을 처리
     *
     * @return 관리자 카테고리 목록 응답
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryListResponse>>> findAll() {
        List<CategoryListResponse> response = categoryQueryService.getAdminCategoryList().stream()
                .map(CategoryListResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
