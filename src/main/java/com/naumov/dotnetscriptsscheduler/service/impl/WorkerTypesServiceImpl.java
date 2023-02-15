package com.naumov.dotnetscriptsscheduler.service.impl;

import com.naumov.dotnetscriptsscheduler.service.WorkerTypesService;

import java.util.List;
import java.util.Set;

/**
 * Simple in-memory worker types service implementation.
 */
public class WorkerTypesServiceImpl implements WorkerTypesService {
    private final List<String> workerTypes;

    public WorkerTypesServiceImpl(Set<String> workerTypes) {
        if (workerTypes.isEmpty()) throw new IllegalStateException("No worker types");
        this.workerTypes = List.copyOf(workerTypes);
    }

    @Override
    public boolean workerExists(String workerType) {
        return workerTypes.contains(workerType);
    }

    @Override
    public List<String> getAllWorkerTypes() {
        return workerTypes;
    }

    @Override
    public String getDefaultWorkerType() {
        return workerTypes.get(0);
    }
}
