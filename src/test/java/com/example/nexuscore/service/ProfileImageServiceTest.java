package com.example.nexuscore.service;

import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.image.ImageStorage;
import com.example.nexuscore.image.ImageStorageException;
import com.example.nexuscore.image.ProfileImageValidator;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.model.Store;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileImageServiceTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final CompanyService companies = mock(CompanyService.class);
    private final ImageStorage storage = mock(ImageStorage.class);
    private final ProfileImageValidator validator = mock(ProfileImageValidator.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final MockMultipartFile file = new MockMultipartFile("file", new byte[]{1});
    private final ProfileImageService service = new ProfileImageService(
            profiles, stores, companies, storage, validator, transactions);

    @BeforeEach
    void setupTransactions() {
        when(transactions.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
    }

    @ParameterizedTest
    @EnumSource(value = ProfileType.class, names = {"HOUSEHOLD", "STORE", "COMPANY"})
    void uploadsImageForEachSupportedProfileType(ProfileType type) {
        Profile profile = profile(12, type);
        prepareUpload(profile);

        var response = service.uploadOwn(12, file);

        assertEquals(12, response.profileId());
        assertEquals("https://res.cloudinary.com/demo/new.png", response.profileImageUrl());
        verify(profile).setProfileImageUrl(response.profileImageUrl());
        verify(profiles).flush();
    }

    @Test
    void removesOnlyUrlWithoutContactingCloudinary() {
        Profile profile = profile(12, ProfileType.HOUSEHOLD);
        when(profiles.findForImageUpdate(12)).thenReturn(Optional.of(profile));

        assertNull(service.removeOwn(12).profileImageUrl());

        verify(profile).setProfileImageUrl(null);
        verifyNoInteractions(storage, validator);
    }

    @Test
    void doesNotChangeCurrentImageWhenCloudinaryFails() {
        Profile profile = profile(12, ProfileType.COMPANY);
        prepareUpload(profile);
        when(storage.upload(any(), anyString()))
                .thenThrow(new ImageStorageException(HttpStatus.BAD_GATEWAY, "Falha no provedor"));

        assertThrows(ImageStorageException.class, () -> service.uploadOwn(12, file));

        verify(profile, never()).setProfileImageUrl(any());
        verify(profiles, never()).findForImageUpdate(any());
    }

    @Test
    void rollsBackDatabaseFailureWithoutDeletingCloudinaryAsset() {
        Profile profile = profile(12, ProfileType.COMPANY);
        prepareUpload(profile);
        doThrow(new DataIntegrityViolationException("Database failure")).when(profiles).flush();

        assertThrows(DataIntegrityViolationException.class, () -> service.uploadOwn(12, file));

        verify(transactions).rollback(any());
    }

    @Test
    void rejectsUploadWhenCloudinaryIsNotConfigured() {
        Profile profile = profile(12, ProfileType.COMPANY);
        when(profiles.findById(12)).thenReturn(Optional.of(profile));

        var exception = assertThrows(ImageStorageException.class, () -> service.uploadOwn(12, file));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        verifyNoInteractions(validator);
        verify(storage, never()).upload(any(), anyString());
    }

    @Test
    void rejectsInvalidFileBeforeCallingCloudinary() {
        Profile profile = profile(12, ProfileType.COMPANY);
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(storage.configured()).thenReturn(true);
        when(validator.validate(file)).thenThrow(new IllegalArgumentException("Imagem invalida"));

        assertThrows(IllegalArgumentException.class, () -> service.uploadOwn(12, file));

        verify(storage, never()).upload(any(), anyString());
        verify(profiles, never()).findForImageUpdate(any());
    }

    @Test
    void rejectsAdminProfile() {
        Profile profile = profile(12, ProfileType.ADMIN);
        when(profiles.findById(12)).thenReturn(Optional.of(profile));

        assertThrows(ForbiddenException.class, () -> service.uploadOwn(12, file));

        verifyNoInteractions(storage, validator);
    }

    @Test
    void rejectsBlockedProfile() {
        Profile profile = profile(12, ProfileType.HOUSEHOLD);
        when(profile.getStatus()).thenReturn(ProfileStatus.BLOCKED);
        when(profiles.findForImageUpdate(12)).thenReturn(Optional.of(profile));

        assertThrows(ForbiddenException.class, () -> service.removeOwn(12));

        verify(profile, never()).setProfileImageUrl(any());
    }

    @Test
    void companyCannotUploadImageToAnotherCompanyStore() {
        Company company = company();
        when(stores.findByIdAndCompanyId(77, company.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.uploadStore(12, 77, file));

        verifyNoInteractions(storage, validator);
    }

    @Test
    void companyCanUploadAndRemoveImageOfOwnedStore() {
        Company company = company();
        Store store = mock(Store.class);
        Profile storeProfile = profile(20, ProfileType.STORE);
        when(store.getProfile()).thenReturn(storeProfile);
        when(stores.findByIdAndCompanyId(77, company.getId())).thenReturn(Optional.of(store));
        prepareUpload(storeProfile);

        assertEquals(20, service.uploadStore(12, 77, file).profileId());
        assertNull(service.removeStore(12, 77).profileImageUrl());

        verify(storeProfile).setProfileImageUrl("https://res.cloudinary.com/demo/new.png");
        verify(storeProfile).setProfileImageUrl(null);
    }

    @Test
    void nonCompanyCannotManageStoreImage() {
        when(companies.companyOf(12)).thenThrow(new ForbiddenException("Somente COMPANY"));

        assertThrows(ForbiddenException.class, () -> service.removeStore(12, 77));

        verifyNoInteractions(stores, storage, validator);
    }

    private Profile profile(int id, ProfileType type) {
        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(id);
        when(profile.getType()).thenReturn(type);
        when(profile.getStatus()).thenReturn(ProfileStatus.ACTIVE);
        return profile;
    }

    private Company company() {
        Company company = mock(Company.class);
        Profile profile = profile(12, ProfileType.COMPANY);
        when(company.getId()).thenReturn(5);
        when(company.getProfile()).thenReturn(profile);
        when(companies.companyOf(12)).thenReturn(company);
        return company;
    }

    private void prepareUpload(Profile profile) {
        when(profiles.findById(profile.getId())).thenReturn(Optional.of(profile));
        when(profiles.findForImageUpdate(profile.getId())).thenReturn(Optional.of(profile));
        when(storage.configured()).thenReturn(true);
        when(validator.validate(file)).thenReturn(new byte[]{1});
        when(storage.upload(any(), anyString())).thenReturn("https://res.cloudinary.com/demo/new.png");
    }
}
