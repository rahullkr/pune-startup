package com.punestartup.common.web;

import com.punestartup.common.exception.InvalidParameterException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageRequests {

    private PageRequests() {
    }

    public static PageRequest of(int page, int size, Sort sort, int maxSize) {
        if (page < 0) {
            throw InvalidParameterException.of("page", "must be 0 or greater");
        }
        if (size < 1 || size > maxSize) {
            throw InvalidParameterException.of("size", "must be between 1 and %d".formatted(maxSize));
        }
        return PageRequest.of(page, size, sort == null ? Sort.unsorted() : sort);
    }
}
