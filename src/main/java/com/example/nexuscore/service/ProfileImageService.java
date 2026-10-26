package com.example.nexuscore.service;

import com.example.nexuscore.dto.profile.ProfileImageResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.image.ImageStorage;
import com.example.nexuscore.image.ImageStorageException;
import com.example.nexuscore.image.ProfileImageValidator;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class ProfileImageService {

    private static final Set<ProfileType> TYPES = Set.of(ProfileType.HOUSEHOLD, ProfileType.STORE, ProfileType.COMPANY);
    private final ProfileRepository profiles;
    private final StoreRepository stores;
    private final CompanyService companies;
    private final ImageStorage storage;
    private final ProfileImageValidator validator;
    private final TransactionTemplate transaction;

    public ProfileImageService(ProfileRepository profiles, StoreRepository stores, CompanyService companies,
            ImageStorage storage, ProfileImageValidator validator,
            PlatformTransactionManager transactionManager) {
        this.profiles = profiles;
        this.stores = stores;
        this.companies = companies;
        this.storage = storage;
        this.validator = validator;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public ProfileImageResponse uploadOwn(Integer profileId, MultipartFile file) {
        return upload(() -> profileId, file);
    }

    public ProfileImageResponse uploadStore(Integer companyProfileId, Integer storeId, MultipartFile file) {
        return upload(() -> storeProfileId(companyProfileId, storeId), file);
    }

    public ProfileImageResponse removeOwn(Integer profileId) {
        return remove(() -> profileId);
    }

    public ProfileImageResponse removeStore(Integer companyProfileId, Integer storeId) {
        return remove(() -> storeProfileId(companyProfileId, storeId));
    }

    private ProfileImageResponse upload(Supplier<Integer> target, MultipartFile file) {
        Integer profileId = transaction.execute(status -> {
            Integer id = target.get();
            validateProfile(profiles.findById(id)
                    .orElseThrow(() -> new NotFoundException("Perfil nao encontrado")));
            return id;
        });
        if (!storage.configured()) {
            throw new ImageStorageException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Upload de imagens indisponivel: configure as credenciais do Cloudinary no Core");
        }
        byte[] bytes = validator.validate(file);
        String publicId = "nexus/profiles/" + profileId + "/" + UUID.randomUUID();
        String url = storage.upload(bytes, publicId);
        return transaction.execute(status -> {
            Integer currentId = target.get();
            if (!profileId.equals(currentId)) {
                throw new ForbiddenException("O perfil da loja foi alterado durante o upload");
            }
            Profile profile = lockedProfile(profileId);
            profile.setProfileImageUrl(url);
            profiles.flush();
            return new ProfileImageResponse(profileId, url);
        });
    }

    private ProfileImageResponse remove(Supplier<Integer> target) {
        return transaction.execute(status -> {
            Profile profile = lockedProfile(target.get());
            profile.setProfileImageUrl(null);
            profiles.flush();
            return new ProfileImageResponse(profile.getId(), null);
        });
    }

    private Profile lockedProfile(Integer id) {
        Profile profile = profiles.findForImageUpdate(id)
                .orElseThrow(() -> new NotFoundException("Perfil nao encontrado"));
        validateProfile(profile);
        return profile;
    }

    private void validateProfile(Profile profile) {
        if (!TYPES.contains(profile.getType()) || profile.getStatus() != ProfileStatus.ACTIVE) {
            throw new ForbiddenException("O perfil deve estar ativo e ser HOUSEHOLD, STORE ou COMPANY");
        }
    }

    private Integer storeProfileId(Integer companyProfileId, Integer storeId) {
        Company company = companies.companyOf(companyProfileId);
        validateProfile(company.getProfile());
        return stores.findByIdAndCompanyId(storeId, company.getId())
                .orElseThrow(() -> new NotFoundException("Loja nao encontrada: " + storeId))
                .getProfile().getId();
    }
}
