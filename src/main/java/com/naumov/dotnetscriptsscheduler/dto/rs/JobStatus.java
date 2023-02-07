package com.naumov.dotnetscriptsscheduler.dto.rs;

// PENDING --+--> RUNNING ---> FINISHED
//           +--> REJECTED
public enum JobStatus {
    PENDING,
    RUNNING,
    FINISHED,
    REJECTED
}