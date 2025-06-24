package com.example.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.internal.io.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    private final String bucketName = "mock-bucket";

    @BeforeEach
    void setUp() throws Exception {
        // Manually inject the value into the @Value field (since Spring isn't running)
        Field bucketField = S3Service.class.getDeclaredField("bucketName");
        bucketField.setAccessible(true);
        bucketField.set(s3Service, bucketName);
    }

    @Test
    void testUploadFile() {
        String key = "file.txt";
        String content = "Test upload";

        s3Service.uploadFile(key, content);

        verify(s3Client).putObject(
                argThat(req -> req.bucket().equals(bucketName) && req.key().equals(key)),
                any()
        );
    }

    @Test
    void testDownloadFile() {
        String key = "file.txt";
        String content = "Test download content";

        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(is)
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        String result = s3Service.downloadFile(key);
        assertEquals(content, result);
    }
}
