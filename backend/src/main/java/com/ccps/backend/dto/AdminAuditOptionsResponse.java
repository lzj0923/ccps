package com.ccps.backend.dto;

import java.util.List;

public record AdminAuditOptionsResponse(List<Actor> actors, List<String> actions) {
    public record Actor(Long id, String name) { }
}
