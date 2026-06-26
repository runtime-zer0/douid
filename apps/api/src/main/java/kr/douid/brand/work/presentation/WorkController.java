package kr.douid.brand.work.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.douid.brand.work.application.command.WorkCommandService;
import kr.douid.brand.work.presentation.request.ChangeVisibilityRequest;
import kr.douid.brand.work.presentation.request.CreateWorkRequest;
import kr.douid.brand.work.presentation.request.UpdateWorkRequest;
import kr.douid.brand.work.presentation.response.WorkCommandResponse;
import kr.douid.brand.work.presentation.response.WorkVisibilityResponse;
import kr.douid.brand.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * 작업물 관리자 Command API 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/works")
@RequiredArgsConstructor
public class WorkController {

    private final WorkCommandService workCommandService;

    /**
     * 작업물 생성 요청을 처리
     *
     * @param request 작업물 생성 요청값
     * @return 생성된 작업물 응답
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkCommandResponse>> create(
            @Valid @RequestBody CreateWorkRequest request) {
        WorkCommandResponse response = WorkCommandResponse.from(
                workCommandService.create(request.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 작업물 수정 요청을 처리
     *
     * @param id      수정할 작업물 ID
     * @param request 작업물 수정 요청값
     * @return 수정된 작업물 응답
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkCommandResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkRequest request) {
        WorkCommandResponse response = WorkCommandResponse.from(
                workCommandService.update(request.toCommand(id)));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 작업물 삭제 요청을 처리
     *
     * @param id 삭제할 작업물 ID
     * @return 빈 성공 응답
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 작업물 공개 여부 변경 요청을 처리
     *
     * @param id      변경할 작업물 ID
     * @param request 공개 여부 변경 요청값
     * @return 변경된 공개 여부 응답
     */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ApiResponse<WorkVisibilityResponse>> changeVisibility(
            @PathVariable Long id,
            @Valid @RequestBody ChangeVisibilityRequest request) {
        WorkVisibilityResponse response = WorkVisibilityResponse.from(
                workCommandService.changeVisibility(request.toCommand(id)));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
