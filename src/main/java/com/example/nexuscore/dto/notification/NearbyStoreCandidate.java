package com.example.nexuscore.dto.notification;

import java.util.List;

public record NearbyStoreCandidate(Integer profileId, List<NearbyStoreOption> stores) {}
