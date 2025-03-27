import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class) // Enables Mockito for JUnit 5
class ExternalServiceClientTest {

    @Mock
    private RestClient restClient; // Mock RestClient

    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private ExternalServiceClient externalServiceClient; // Inject Mocked RestClient

    @Test
    void testFetchData() {
        // Mock API Response
        ApiResponse mockResponse = new ApiResponse("Success");

        // Stubbing method calls step-by-step
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ApiResponse.class)).thenReturn(mockResponse);

        // Call method
        ApiResponse response = externalServiceClient.fetchData("testValue");

        // Verify response
        assertNotNull(response);
        assertEquals("Success", response.getMessage());
    }
}
