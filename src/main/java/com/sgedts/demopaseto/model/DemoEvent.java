package com.sgedts.demopaseto.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemoEvent {

    private final String id;
    private final String type;
    private final String at;
    private final Map<String, Object> extra;

    public DemoEvent(String type, Map<String, Object> payload) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.at = Instant.now().toString();
        this.extra = new LinkedHashMap<>(payload);
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getAt() { return at; }

    @JsonAnyGetter
    public Map<String, Object> getExtra() { return extra; }
}
