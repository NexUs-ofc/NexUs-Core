package com.example.nexuscore.service;

import com.example.nexuscore.model.ActivityRecord;
import com.example.nexuscore.repository.ActivityRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class ActivityRecordService {

    private final ActivityRecordRepository repository;

    public ActivityRecordService(ActivityRecordRepository repository) {
        this.repository = repository;
    }

    public void record(Integer accountId, String eventType, String details) {
        repository.save(new ActivityRecord(accountId, eventType, details));
    }
}
