import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.web.client.RestClient;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class) // Enable Mockito
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
    private ExternalServiceClient externalServiceClient; // Inject mock RestClient

    @Test
    void testFetchData() {
        // Mock API Response
        ApiResponse mockResponse = new ApiResponse("Success");

        // Fix: Correctly mock RestClient method chaining
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec); // Fix here
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ApiResponse.class)).thenReturn(mockResponse);

        // Call method
        ApiResponse response = externalServiceClient.fetchData("testValue");

        // Assertions
        assertNotNull(response);
        assertEquals("Success", response.getMessage());
    }
}
