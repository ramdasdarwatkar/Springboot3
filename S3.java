package com.example.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.internal.io.AbortableInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    private S3Service s3Service;

    private final String bucketName = "test-bucket";

    @BeforeEach
    void setup() {
        s3Service = new S3Service(s3Client, bucketName);
    }

    @Test
    void testUpload() {
        String key = "file.txt";
        String content = "Hello, S3!";

        s3Service.upload(key, content);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertEquals(bucketName, request.bucket());
        assertEquals(key, request.key());
    }

    @Test
    void testGetObjectAsString() {
        String key = "file.txt";
        String expectedContent = "This is from S3";

        InputStream mockStream = new ByteArrayInputStream(expectedContent.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> responseInputStream =
                new ResponseInputStream<>(GetObjectResponse.builder().build(), AbortableInputStream.create(mockStream));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        String result = s3Service.getObjectAsString(key);

        assertEquals(expectedContent, result);
        verify(s3Client).getObject(argThat(req ->
                req.bucket().equals(bucketName) && req.key().equals(key)
        ));
    }
}
