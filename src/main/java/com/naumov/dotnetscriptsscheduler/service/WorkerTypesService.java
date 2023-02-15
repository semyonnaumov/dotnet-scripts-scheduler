package com.naumov.dotnetscriptsscheduler.service;

import java.util.List;

/**
 * Simple service, used to provide available worker types.
 */
public interface WorkerTypesService {

    /**
     * Returns the set of all worker type names.
     *
     * @return list with types
     */
    List<String> getAllWorkerTypes();

    /**
     * Checks whether the {@code workerType} exists among all available worker types.
     *
     * @param workerType worker type to check
     * @return check result
     */
    boolean workerExists(String workerType);

    /**
     * Returns default worker type.
     *
     * @return default worker type
     */
    String getDefaultWorkerType();
}
