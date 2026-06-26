package kr.douid.brand.media.presentation;

import java.io.IOException;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.douid.brand.media.application.command.MediaCommandService;
import kr.douid.brand.media.application.command.MediaResult;
import kr.douid.brand.media.application.command.MediaUploadCommand;
import kr.douid.brand.media.application.query.MediaFileResult;
import kr.douid.brand.media.application.query.MediaQueryService;
import kr.douid.brand.media.application.query.MediaView;
import kr.douid.brand.media.domain.EmptyMediaFileException;
import kr.douid.brand.media.presentation.response.MediaListResponse;
import kr.douid.brand.media.presentation.response.MediaUploadResponse;
import kr.douid.brand.shared.pagination.PageResponse;
import kr.douid.brand.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * 미디어 관리 API 컨트롤러
 *
 * 읽기(목록/단건/파일서빙)는 /api/media/**, 쓰기(업로드/삭제)는 /api/admin/media/** 경로 사용
 * 보안 정책은 SecurityConfig의 /api/media/** permitAll, /api/admin/** authenticated 기준
 */
@RestController
@RequiredArgsConstructor
public class MediaController {

    private static final String MEDIA_BASE_URL = "/api/media";
    private static final String ADMIN_MEDIA_BASE = "/api/admin/media";

    private final MediaCommandService mediaCommandService;
    private final MediaQueryService mediaQueryService;

    /**
     * 이미지 파일 업로드 요청을 처리
     *
     * @param file 업로드할 파일
     * @return 업로드된 미디어 응답
     * @throws EmptyMediaFileException 빈 파일인 경우
     */
    @PostMapping(ADMIN_MEDIA_BASE)
    public ResponseEntity<ApiResponse<MediaUploadResponse>> upload(
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new EmptyMediaFileException();
        }

        MediaUploadCommand command = new MediaUploadCommand(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getInputStream(),
                file.getSize());

        MediaResult result = mediaCommandService.upload(command);
        MediaUploadResponse response = MediaUploadResponse.from(result, MEDIA_BASE_URL);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 미디어 단건 메타데이터 조회 요청을 처리
     *
     * @param id 미디어 식별자
     * @return 미디어 메타데이터 응답
     */
    @GetMapping(MEDIA_BASE_URL + "/{id}")
    public ResponseEntity<ApiResponse<MediaListResponse>> getMedia(@PathVariable Long id) {
        MediaView view = mediaQueryService.getMedia(id);
        return ResponseEntity.ok(ApiResponse.success(MediaListResponse.from(view, MEDIA_BASE_URL)));
    }

    /**
     * 미디어 목록 페이지네이션 조회 요청을 처리
     *
     * @param pageable 페이지네이션 파라미터 (기본: createdAt 내림차순, 20개)
     * @return 미디어 목록 응답
     */
    @GetMapping(MEDIA_BASE_URL)
    public ResponseEntity<ApiResponse<PageResponse<MediaListResponse>>> getMediaList(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MediaListResponse> response = PageResponse.from(
                mediaQueryService.getMediaList(pageable)
                        .map(view -> MediaListResponse.from(view, MEDIA_BASE_URL)));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 미디어 삭제 요청을 처리
     *
     * @param id 삭제할 미디어 식별자
     * @return 빈 성공 응답
     */
    @DeleteMapping(ADMIN_MEDIA_BASE + "/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        mediaCommandService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 저장된 이미지 파일을 스트리밍으로 반환
     *
     * @param id 미디어 식별자
     * @return 파일 바이너리 스트림
     */
    @GetMapping(MEDIA_BASE_URL + "/{id}/file")
    public ResponseEntity<InputStreamResource> serveFile(@PathVariable Long id) {
        MediaFileResult result = mediaQueryService.getMediaFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + result.originalFilename() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(new InputStreamResource(result.inputStream()));
    }
}
