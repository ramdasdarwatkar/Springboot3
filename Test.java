import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlagComponentTest {

    @Mock
    private VoGhdrrordRepository voGhdrrordRepository; // Mocked repository

    @Spy
    @InjectMocks
    private FlagComponent flagComponent; // Spying to override method references

    @BeforeEach
    void setUp() {
        // Mock the actual method references in methodMap
        doReturn(true).when(flagComponent).hasMnCRoleAccess(any());
        doReturn(false).when(flagComponent).isRelationshipOwner(any());

        // Manually override the methodMap with spy-controlled methods
        flagComponent.methodMap.put(APIConstants.MNC_ROLE_ACCESS, flagComponent::hasMnCRoleAccess);
        flagComponent.methodMap.put(APIConstants.IS_RELATIONSHIP_OWNER, flagComponent::isRelationshipOwner);
    }

    @Test
    void testInvokeMethod_ValidFlag_ReturnsTrue() {
        UserEntitlement userEntitlement = new UserEntitlement();

        Boolean result = flagComponent.invokeMethod(APIConstants.MNC_ROLE_ACCESS, userEntitlement);

        assertEquals(true, result);
        verify(flagComponent).hasMnCRoleAccess(userEntitlement); // Verify the method was called
    }

    @Test
    void testInvokeMethod_ValidFlag_ReturnsFalse() {
        UserEntitlement userEntitlement = new UserEntitlement();

        Boolean result = flagComponent.invokeMethod(APIConstants.IS_RELATIONSHIP_OWNER, userEntitlement);

        assertEquals(false, result);
        verify(flagComponent).isRelationshipOwner(userEntitlement);
    }

    @Test
    void testInvokeMethod_InvalidFlag() {
        UserEntitlement userEntitlement = new UserEntitlement();

        Boolean result = flagComponent.invokeMethod("INVALID_FLAG", userEntitlement);

        assertEquals(false, result);
    }
}
