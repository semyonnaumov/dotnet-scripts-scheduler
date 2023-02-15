package com.naumov.dotnetscriptsscheduler.kafka;

public interface Reporter<T> {
    void report(T object);
}
