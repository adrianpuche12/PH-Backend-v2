package balance.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Service
public class R2StorageService {

    @Value("${r2.access-key}")      private String accessKey;
    @Value("${r2.secret-key}")      private String secretKey;
    @Value("${r2.endpoint}")        private String endpoint;
    @Value("${r2.bucket}")          private String bucket;
    @Value("${r2.public-url}")      private String publicUrl;
    @Value("${r2.folder-prefix:prod}") private String folderPrefix;

    private S3Client s3;

    @PostConstruct
    private void init() {
        s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * Sube un archivo a R2 y devuelve su URL pública.
     * @param file     archivo recibido por multipart
     * @param folder   carpeta dentro del bucket (ej: "comprobantes")
     * @return URL pública accesible desde el navegador
     */
    public String upload(MultipartFile file, String folder) {
        try {
            String ext      = getExtension(file.getOriginalFilename());
            String fileName = folderPrefix + "/" + folder + "/" + Instant.now().toEpochMilli()
                              + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3.putObject(req, RequestBody.fromBytes(file.getBytes()));

            return publicUrl + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Error al subir imagen a R2: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un archivo de R2 a partir de su URL pública.
     * No lanza excepción si el archivo no existe.
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(publicUrl)) return;
        try {
            String key = fileUrl.substring(publicUrl.length() + 1);
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception ignored) {}
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
