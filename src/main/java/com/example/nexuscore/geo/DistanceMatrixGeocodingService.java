package com.example.nexuscore.geo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DistanceMatrixGeocodingService implements GeocodingService {

    private static final String BASE_URL = "https://api-v2.distancematrix.ai";

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public DistanceMatrixGeocodingService(GeocodingProperties properties) {
        this(properties, RestClient.builder().requestFactory(requestFactory()));
    }

    DistanceMatrixGeocodingService(GeocodingProperties properties, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
        this.apiKey = properties.apiKey();
    }

    @Override
    public Optional<GeoPoint> geocode(String street, String number, String neighborhood, String city, String state) {
        String address = "%s, %s, %s, Brasil".formatted(street.trim(), neighborhood.trim(), city.trim());
        try {
            GeocodingResponse response = restClient.get()
                    .uri(builder -> builder.path("/maps/api/geocode/json")
                            .queryParam("address", "{address}")
                            .queryParam("key", "{key}")
                            .build(Map.of("address", address, "key", apiKey)))
                    .retrieve()
                    .body(GeocodingResponse.class);
            if (response == null || !"OK".equals(response.status())
                    || response.results() == null || response.results().isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.results().get(0))
                    .map(GeocodingResult::geometry)
                    .map(Geometry::location)
                    .filter(location -> validCoordinate(location.lat(), 90) && validCoordinate(location.lng(), 180))
                    .map(location -> new GeoPoint(location.lat(), location.lng()));
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    private boolean validCoordinate(BigDecimal value, int limit) {
        return value != null && value.abs().compareTo(BigDecimal.valueOf(limit)) <= 0;
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeocodingResponse(String status, @JsonAlias("result") List<GeocodingResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeocodingResult(Geometry geometry) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Geometry(Location location) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Location(BigDecimal lat, BigDecimal lng) {}
}
