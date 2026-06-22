package kr.douid.brand.category.presentation;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryCommandResponse> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(CategoryCommandResponse
                .from(categoryCommandService.createCategory(request.toCommand())));
    }

    /**
     * 관리자 카테고리 수정 요청을 처리
     *
     * @param id 수정할 카테고리 ID
     * @param request 카테고리 수정 요청값
     * @return 수정된 카테고리 응답
     */
    @PutMapping("/{id}")
    public ApiResponse<CategoryCommandResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(CategoryCommandResponse
                .from(categoryCommandService.updateCategory(request.toCommand(id))));
    }

    /**
     * 관리자 카테고리 삭제 요청을 처리
     *
     * @param id 삭제할 카테고리 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryCommandService.deleteCategory(DeleteCategoryCommand.of(id));
    }

    /**
     * 관리자 카테고리 목록 조회 요청을 처리
     *
     * @return 관리자 카테고리 목록 응답
     */
    @GetMapping
    public ApiResponse<List<CategoryListResponse>> findAll() {
        return ApiResponse.success(categoryQueryService.getAdminCategoryList().stream()
                .map(CategoryListResponse::from).toList());
    }
}
