package net.mysterria.delivery.domain.delivery.model.source;

import io.javalin.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryStatus {
    DELIVERED(HttpStatus.OK, "Delivered"),
    ALREADY_DELIVERED(HttpStatus.OK, "Purchase was already delivered"),
    QUEUED(HttpStatus.ACCEPTED, "Player is offline, delivery queued until they join");

    private final HttpStatus httpStatus;
    private final String message;
}
