package com.example.nexuscore.model;

import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ProfileAddressValidationTest {

    @Test
    void requiresAddressForHouseholdAndStore() {
        for (ProfileType type : new ProfileType[] {ProfileType.HOUSEHOLD, ProfileType.STORE}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Profile(null, "user@example.com", "User", null, type, Set.of()));

            Profile profile = new Profile(mock(Address.class), "user@example.com", "User", null, type, Set.of());
            assertThrows(IllegalArgumentException.class, () -> profile.setAddress(null));
        }
    }

    @Test
    void allowsCompanyWithoutAddress() {
        assertDoesNotThrow(() -> new Profile(null, "company@example.com", "Company", null,
                ProfileType.COMPANY, Set.of()));
    }
}
