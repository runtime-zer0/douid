package kr.douid.brand.category.presentation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.douid.brand.category.application.CategoryQueryService;
import kr.douid.brand.category.presentation.dto.CategoryResponse;
import kr.douid.brand.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryQueryService categoryQueryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> findAll() {
        return ApiResponse.success(
                categoryQueryService.findAllVisible().stream()
                        .map(CategoryResponse::from)
                        .toList()
        );
    }
}
