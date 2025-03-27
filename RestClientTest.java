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
    private RestClient restClient; // Manually Mock RestClient (No Spring Context)

    @InjectMocks
    private ExternalServiceClient externalServiceClient; // Inject Mocked RestClient

    @Test
    void testFetchData() {
        // Mock API Response
        ApiResponse mockResponse = new ApiResponse("Success");

        // Mock RestClient Behavior
        when(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(ApiResponse.class))
                .thenReturn(mockResponse);

        // Call Method
        ApiResponse response = externalServiceClient.fetchData("testValue");

        // Verify Response
        assertNotNull(response);
        assertEquals("Success", response.getMessage());
    }
}
