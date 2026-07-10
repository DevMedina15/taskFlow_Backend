package com.taskflow.api.shared.dto;

import java.util.List;

public record PageResponseDto <T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasMore
){
}
