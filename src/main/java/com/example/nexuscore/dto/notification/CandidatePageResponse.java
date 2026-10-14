package com.example.nexuscore.dto.notification;

import java.util.List;

public record CandidatePageResponse<T>(List<T> content, boolean hasNext) {}
