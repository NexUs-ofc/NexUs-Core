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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Empresas e lojas")
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

    @Operation(summary = "Consultar empresa",
            description = "Retorna os dados da empresa vinculada ao perfil autenticado.")
    @GetMapping("/me")
    public CompanyResponse getCompany() {
        return companyService.get(currentProfile.profileId().intValue());
    }

    @Operation(summary = "Alterar plano",
            description = "Seleciona um plano ativo para a empresa do perfil autenticado.")
    @PatchMapping("/me/plan")
    public CompanyResponse changePlan(@Valid @RequestBody PlanChangeRequest request) {
        return companyService.changePlan(currentProfile.profileId().intValue(), request);
    }

    @Operation(summary = "Listar lojas da empresa",
            description = "Lista somente as lojas pertencentes à empresa autenticada.")
    @GetMapping("/me/stores")
    public List<StoreResponse> listStores() {
        return storeService.listCompany(currentProfile.profileId().intValue());
    }

    @Operation(summary = "Cadastrar loja",
            description = "Cria loja, perfil e endereço. Exige perfil COMPANY e CNPJ e e-mail únicos.")
    @PostMapping("/me/stores")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse createStore(@Valid @RequestBody StoreCreateRequest request) {
        return storeService.createCompanyStore(currentProfile.profileId().intValue(), request);
    }

    @Operation(summary = "Consultar loja da empresa",
            description = "Consulta uma loja pelo id dentro da empresa autenticada.")
    @GetMapping("/me/stores/{id}")
    public StoreResponse getStore(@PathVariable Integer id) {
        return storeService.getCompany(currentProfile.profileId().intValue(), id);
    }

    @Operation(summary = "Atualizar loja",
            description = "Altera somente os campos informados. Nome em branco não é permitido.")
    @PatchMapping("/me/stores/{id}")
    public StoreResponse updateStore(@PathVariable Integer id,
                                     @Valid @RequestBody StoreUpdateRequest request) {
        return storeService.updateCompanyStore(currentProfile.profileId().intValue(), id, request);
    }

    @Operation(summary = "Excluir loja",
            description = "Exclui a loja, o perfil e o endereço associados dentro de uma transação.")
    @DeleteMapping("/me/stores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStore(@PathVariable Integer id) {
        storeService.removeCompanyStore(currentProfile.profileId().intValue(), id);
    }

    @Operation(summary = "Enviar imagem da loja",
            description = "Recebe o campo file em multipart/form-data; limite de 5 MB.")
    @PutMapping(value = "/me/stores/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileImageResponse uploadStoreImage(@PathVariable Integer id,
                                                @RequestParam("file") MultipartFile file) {
        return images.uploadStore(currentProfile.profileId().intValue(), id, file);
    }

    @Operation(summary = "Remover imagem da loja",
            description = "Remove a imagem da loja pertencente à empresa autenticada.")
    @DeleteMapping("/me/stores/{id}/image")
    public ProfileImageResponse removeStoreImage(@PathVariable Integer id) {
        return images.removeStore(currentProfile.profileId().intValue(), id);
    }

    @Operation(summary = "Listar pagamentos",
            description = "Lista pagamentos da empresa autenticada, com filtro opcional de status.")
    @GetMapping("/me/payments")
    public List<PaymentResponse> listPayments(@RequestParam(required = false) PaymentStatus status) {
        return paymentService.list(currentProfile.profileId().intValue(), status);
    }

    @Operation(summary = "Consultar pagamento",
            description = "Consulta um pagamento pertencente à empresa autenticada.")
    @GetMapping("/me/payments/{id}")
    public PaymentResponse getPayment(@PathVariable Integer id) {
        return paymentService.get(currentProfile.profileId().intValue(), id);
    }

    @Operation(summary = "Registrar pagamento",
            description = "Marca como pago um pagamento elegível da empresa autenticada.")
    @PostMapping("/me/payments/{id}/pay")
    public PaymentResponse pay(@PathVariable Integer id) {
        return paymentService.pay(currentProfile.profileId().intValue(), id);
    }
}
