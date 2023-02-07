package com.naumov.dotnetscriptsscheduler.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class JobPayloadConfig implements Serializable {
    private String nugetConfigXml;
}
