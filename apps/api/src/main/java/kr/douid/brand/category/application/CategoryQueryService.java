package kr.douid.brand.category.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.category.domain.CategoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResult> findAllForAdmin() {
        return categoryRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc()
                .stream()
                .map(CategoryResult::from)
                .toList();
    }

    public List<CategoryResult> findAllVisible() {
        return categoryRepository.findAllByVisibleTrueOrderByDisplayOrderAscCreatedAtAsc()
                .stream()
                .map(CategoryResult::from)
                .toList();
    }
}
