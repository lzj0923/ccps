package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.AdminPropertyPhotoMapper;
import com.ccps.backend.mapper.AdminPropertyPhotoMapper.DocumentRow;
import com.ccps.backend.mapper.AdminPropertyPhotoMapper.PhotoRow;

@ExtendWith(MockitoExtension.class)
class AdminPropertyPhotoServiceTest {
    @Mock private AdminPropertyPhotoMapper mapper;
    @TempDir java.nio.file.Path tempDir;
    private AdminPropertyPhotoService service;

    @BeforeEach
    void setUp() { service = new AdminPropertyPhotoService(mapper, tempDir.toString()); }

    @Test
    void createsFirstPhotoAsCoverAndPersistsDocument() throws Exception {
        when(mapper.ownsProperty(7L, 12L)).thenReturn(1);
        when(mapper.countRegular(12L)).thenReturn(0, 1);
        when(mapper.regularCoverCount(12L)).thenReturn(1);
        when(mapper.insertDocument(any(DocumentRow.class))).thenAnswer(invocation -> {
            invocation.<DocumentRow>getArgument(0).setId(90L); return 1;
        });
        when(mapper.insertPhoto(any(PhotoRow.class))).thenAnswer(invocation -> {
            invocation.<PhotoRow>getArgument(0).setId(91L); return 1;
        });
        PhotoRow stored = new PhotoRow(); stored.setId(91L); stored.setOwnerUnitId(12L); stored.setDocumentId(90L);
        stored.setTitle("客廳"); stored.setCategory("interior"); stored.setSortOrder(0); stored.setCoverFlag(true);
        stored.setOriginalName("living-room.png"); stored.setMimeType("image/png"); stored.setFileSize(68L);
        when(mapper.find(12L, 91L)).thenReturn(stored);

        var result = service.create(1L, 7L, 12L, "客廳", "interior", null, 0, false,null,null,
                new MockMultipartFile("file", "living-room.png", "image/png", png()));

        assertThat(result.cover()).isTrue();
        verify(mapper).clearRegularCover(12L);
        verify(mapper).insertDocument(any(DocumentRow.class));
        verify(mapper).insertPhoto(any(PhotoRow.class));
    }

    @Test
    void rejectsNonImageUpload() {
        when(mapper.ownsProperty(7L, 12L)).thenReturn(1);
        MockMultipartFile pdf = new MockMultipartFile("file", "fake.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> service.create(1L, 7L, 12L, "假照片", "other", null, 0, false,null,null, pdf))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("JPG and PNG");
    }

    @Test
    void rejectsRentalPhotoAsPropertyCover() {
        when(mapper.ownsProperty(7L, 12L)).thenReturn(1);
        when(mapper.leaseBelongs(12L, 44L)).thenReturn(1);

        assertThatThrownBy(() -> service.create(1L, 7L, 12L, "入住照片", "other", null, 0,
                true, 44L, "before", new MockMultipartFile("file", "before.png", "image/png", new byte[0])))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot be used as property cover");
    }

    @Test
    void findsRegularPropertyPhotoAssetsForLease() throws Exception {
        Files.createDirectories(tempDir.resolve("12"));
        Files.write(tempDir.resolve("12/living.jpg"), png());
        PhotoRow photo = new PhotoRow();
        photo.setStorageKey("12/living.jpg");
        photo.setMimeType("image/png");
        photo.setSortOrder(3);
        photo.setCoverFlag(true);
        when(mapper.listRegularByLease(44L)).thenReturn(List.of(photo));

        var result = service.regularAssetsForLease(44L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).path()).isEqualTo(tempDir.resolve("12/living.jpg"));
        assertThat(result.get(0).coverFlag()).isTrue();
    }

    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
