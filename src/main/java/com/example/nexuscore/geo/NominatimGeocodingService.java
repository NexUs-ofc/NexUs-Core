package com.example.nexuscore.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Geocodificacao via Nominatim (OpenStreetMap) - gratuito, sem necessidade de API key/billing.
 * Sujeito ao rate limit de 1 requisicao/segundo da politica de uso justo do provedor
 * (https://operations.osmfoundation.org/policies/nominatim/), por isso e usado apenas para o
 * ponto de origem de uma busca ou no cadastro pontual de uma loja - nunca em lote.
 */
@Service
public class NominatimGeocodingService implements GeocodingService {

    private static final String BASE_URL = "https://nominatim.openstreetmap.org";

    private final RestClient restClient;

    public NominatimGeocodingService(GeocodingProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", properties.userAgent())
                .build();
    }

    @Override
    public Optional<GeoPoint> geocode(String street, String number, String neighborhood, String city, String state) {
        String query = "%s %s, %s, %s - %s, Brasil".formatted(street, number, neighborhood, city, state);
        NominatimResult[] results = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .queryParam("q", query)
                        .build())
                .retrieve()
                .body(NominatimResult[].class);
        if (results == null || results.length == 0) {
            return Optional.empty();
        }
        NominatimResult result = results[0];
        return Optional.of(new GeoPoint(new BigDecimal(result.lat()), new BigDecimal(result.lon())));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResult(String lat, String lon) {}
}
