package com.example.nexuscore.controller;

import com.example.nexuscore.dto.company.CompanyResponse;
import com.example.nexuscore.dto.company.PlanChangeRequest;
import com.example.nexuscore.dto.payment.PaymentResponse;
import com.example.nexuscore.dto.profile.ProfileImageResponse;
import com.example.nexuscore.dto.store.StoreCreateRequest;
import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.dto.store.StoreUpdateRequest;
import com.example.nexuscore.model.PaymentStatus;
import com.example.nexuscore.service.CompanyService;
import com.example.nexuscore.service.PaymentService;
import com.example.nexuscore.service.ProfileImageService;
import com.example.nexuscore.service.StoreService;
import com.example.nexuscore.util.CurrentProfileResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final StoreService storeService;
    private final PaymentService paymentService;
    private final CurrentProfileResolver currentProfile;
    private final ProfileImageService images;

    public CompanyController(CompanyService companyService, StoreService storeService,
                             PaymentService paymentService, CurrentProfileResolver currentProfile,
                             ProfileImageService images) {
        this.companyService = companyService;
        this.storeService = storeService;
        this.paymentService = paymentService;
        this.currentProfile = currentProfile;
        this.images = images;
    }

    @PatchMapping("/me/plan")
    public CompanyResponse changePlan(@Valid @RequestBody PlanChangeRequest request) {
        return companyService.changePlan(currentProfile.profileId().intValue(), request);
    }

    @GetMapping("/me/stores")
    public List<StoreResponse> listStores() {
        return storeService.listCompany(currentProfile.profileId().intValue());
    }

    @PostMapping("/me/stores")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse createStore(@Valid @RequestBody StoreCreateRequest request) {
        return storeService.createCompanyStore(currentProfile.profileId().intValue(), request);
    }

    @GetMapping("/me/stores/{id}")
    public StoreResponse getStore(@PathVariable Integer id) {
        return storeService.getCompany(currentProfile.profileId().intValue(), id);
    }

    @PatchMapping("/me/stores/{id}")
    public StoreResponse updateStore(@PathVariable Integer id,
                                     @Valid @RequestBody StoreUpdateRequest request) {
        return storeService.updateCompanyStore(currentProfile.profileId().intValue(), id, request);
    }

    @DeleteMapping("/me/stores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStore(@PathVariable Integer id) {
        storeService.removeCompanyStore(currentProfile.profileId().intValue(), id);
    }

    @PutMapping(value = "/me/stores/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileImageResponse uploadStoreImage(@PathVariable Integer id,
                                                @RequestParam("file") MultipartFile file) {
        return images.uploadStore(currentProfile.profileId().intValue(), id, file);
    }

    @DeleteMapping("/me/stores/{id}/image")
    public ProfileImageResponse removeStoreImage(@PathVariable Integer id) {
        return images.removeStore(currentProfile.profileId().intValue(), id);
    }

    @GetMapping("/me/payments")
    public List<PaymentResponse> listPayments(@RequestParam(required = false) PaymentStatus status) {
        return paymentService.list(currentProfile.profileId().intValue(), status);
    }

    @GetMapping("/me/payments/{id}")
    public PaymentResponse getPayment(@PathVariable Integer id) {
        return paymentService.get(currentProfile.profileId().intValue(), id);
    }

    @PostMapping("/me/payments/{id}/pay")
    public PaymentResponse pay(@PathVariable Integer id) {
        return paymentService.pay(currentProfile.profileId().intValue(), id);
    }
}
