package com.ccps.backend.dto;

import java.util.List;

public record OwnerPropertyServicesResponse(Long ownerUnitId, List<String> services) {
}
