package kr.douid.brand.media.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kr.douid.brand.media.application.command.MediaDeleteCommandService;
import kr.douid.brand.media.application.command.MediaResult;
import kr.douid.brand.media.application.command.MediaUploadUseCase;
import kr.douid.brand.media.application.query.MediaFileResult;
import kr.douid.brand.media.application.query.MediaQueryService;
import kr.douid.brand.media.application.query.MediaView;
import kr.douid.brand.media.domain.EmptyMediaFileException;
import kr.douid.brand.media.domain.InvalidMediaFileTypeException;
import kr.douid.brand.media.domain.MediaNotFoundException;
import kr.douid.brand.shared.config.SecurityConfig;
import kr.douid.brand.shared.presentation.GlobalExceptionHandler;

@WebMvcTest(MediaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaUploadUseCase mediaUploadUseCase;

    @MockitoBean
    private MediaDeleteCommandService mediaDeleteCommandService;

    @MockitoBean
    private MediaQueryService mediaQueryService;

    @Test
    void upload_정상_201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        given(mediaUploadUseCase.upload(any())).willReturn(fakeMediaResult(1L));

        mockMvc.perform(multipart("/api/admin/media").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.url").value("/api/media/1/file"));
    }

    @Test
    void upload_빈파일_400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[0]);
        given(mediaUploadUseCase.upload(any())).willThrow(new EmptyMediaFileException());

        mockMvc.perform(multipart("/api/admin/media").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("EMPTY_FILE"));
    }

    @Test
    void upload_이미지아닌파일_400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1});
        given(mediaUploadUseCase.upload(any())).willThrow(new InvalidMediaFileTypeException());

        mockMvc.perform(multipart("/api/admin/media").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void getMedia_단건_200() throws Exception {
        given(mediaQueryService.getMedia(1L)).willReturn(fakeMediaView(1L, "photo.jpg"));

        mockMvc.perform(get("/api/media/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.originalFilename").value("photo.jpg"));
    }

    @Test
    void getMedia_미존재_404() throws Exception {
        given(mediaQueryService.getMedia(99L)).willThrow(new MediaNotFoundException());

        mockMvc.perform(get("/api/media/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("MEDIA_NOT_FOUND"));
    }

    @Test
    void getMediaList_목록_200() throws Exception {
        PageImpl<MediaView> page = new PageImpl<>(
                List.of(fakeMediaView(2L, "banner.png"), fakeMediaView(1L, "photo.jpg")),
                PageRequest.of(0, 20), 2);
        given(mediaQueryService.getMediaList(any())).willReturn(page);

        mockMvc.perform(get("/api/media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void delete_정상_200() throws Exception {
        willDoNothing().given(mediaDeleteCommandService).delete(anyLong());

        mockMvc.perform(delete("/api/admin/media/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void delete_미존재_404() throws Exception {
        willThrow(new MediaNotFoundException()).given(mediaDeleteCommandService).delete(99L);

        mockMvc.perform(delete("/api/admin/media/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("MEDIA_NOT_FOUND"));
    }

    @Test
    @WithAnonymousUser
    void upload_미인증_401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/admin/media").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    @Test
    void serveFile_정상_파일스트림() throws Exception {
        given(mediaQueryService.getMediaFile(1L)).willReturn(new MediaFileResult(
                "photo.jpg", "image/jpeg", new ByteArrayInputStream(new byte[]{1, 2, 3})));

        mockMvc.perform(get("/api/media/1/file"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"photo.jpg\""));
    }

    @Test
    void serveFile_미존재_404() throws Exception {
        given(mediaQueryService.getMediaFile(99L)).willThrow(new MediaNotFoundException());

        mockMvc.perform(get("/api/media/99/file"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("MEDIA_NOT_FOUND"));
    }

    private MediaResult fakeMediaResult(Long id) {
        return new MediaResult(id, "photo.jpg", "uploads/media/uuid.jpg",
                "image/jpeg", 1024L, LocalDateTime.now());
    }

    private MediaView fakeMediaView(Long id, String originalFilename) {
        return new MediaView(id, originalFilename, "uploads/media/uuid.jpg",
                "image/jpeg", 1024L, LocalDateTime.now());
    }
}
