package balance.storage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class R2StorageServiceTest {

    private R2StorageService service;
    private S3Client          s3;

    @BeforeEach
    void setUp() {
        service = new R2StorageService();
        s3      = mock(S3Client.class);

        // Inyectar valores de configuracion y S3Client mock (evita llamar @PostConstruct real)
        ReflectionTestUtils.setField(service, "accessKey",    "test-key");
        ReflectionTestUtils.setField(service, "secretKey",    "test-secret");
        ReflectionTestUtils.setField(service, "endpoint",     "https://r2.test.example.com");
        ReflectionTestUtils.setField(service, "bucket",       "test-bucket");
        ReflectionTestUtils.setField(service, "publicUrl",    "https://pub.test.example.com");
        ReflectionTestUtils.setField(service, "folderPrefix", "local");
        ReflectionTestUtils.setField(service, "s3",           s3);
    }

    // ── upload ────────────────────────────────────────────────────────────────

    @Test
    void upload_returnsPublicUrl() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", "fake-content".getBytes());

        String url = service.upload(file, "comprobantes");

        assertThat(url).startsWith("https://pub.test.example.com/local/comprobantes/");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    void upload_callsPutObjectOnS3() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "receipt.png", "image/png", "data".getBytes());

        service.upload(file, "comprobantes");

        verify(s3).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_usesCorrectBucketAndFolder() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "data".getBytes());

        service.upload(file, "mis-fotos");

        verify(s3).putObject(
            argThat((PutObjectRequest req) ->
                req.bucket().equals("test-bucket") &&
                req.key().contains("local/mis-fotos/")),
            any(RequestBody.class));
    }

    @Test
    void upload_usesJpgExtensionWhenFilenameHasNoExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "noextension", "image/jpeg", "data".getBytes());

        String url = service.upload(file, "comprobantes");

        assertThat(url).endsWith(".jpg");
    }

    @Test
    void upload_preservesOriginalExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", "data".getBytes());

        String url = service.upload(file, "docs");

        assertThat(url).endsWith(".pdf");
    }

    @Test
    void upload_wrapsS3ExceptionAsRuntimeException() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "data".getBytes());
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(new RuntimeException("S3 unavailable"));

        assertThatThrownBy(() -> service.upload(file, "comprobantes"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Error al subir imagen a R2");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_callsDeleteObjectWithCorrectKey() {
        String fileUrl = "https://pub.test.example.com/local/comprobantes/photo.jpg";

        service.delete(fileUrl);

        verify(s3).deleteObject(
            argThat((DeleteObjectRequest req) ->
                req.bucket().equals("test-bucket") &&
                req.key().equals("local/comprobantes/photo.jpg")));
    }

    @Test
    void delete_doesNothingWhenUrlIsNull() {
        service.delete(null);

        verifyNoInteractions(s3);
    }

    @Test
    void delete_doesNothingWhenUrlDoesNotMatchPublicUrl() {
        service.delete("https://other-cdn.com/some/file.jpg");

        verifyNoInteractions(s3);
    }

    @Test
    void delete_silentlyIgnoresS3Errors() {
        String fileUrl = "https://pub.test.example.com/local/file.jpg";
        when(s3.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(new RuntimeException("S3 error"));

        // No debe lanzar excepcion
        assertThatCode(() -> service.delete(fileUrl)).doesNotThrowAnyException();
    }
}
