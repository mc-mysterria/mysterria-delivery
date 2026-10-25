package net.mysterria.delivery.domain.delivery.endpoint;

import dev.ua.ikeepcalm.catwalk.bridge.annotations.BridgeEventHandler;
import dev.ua.ikeepcalm.catwalk.bridge.annotations.BridgeRequestBody;
import dev.ua.ikeepcalm.catwalk.bridge.source.BridgeApiResponse;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import lombok.RequiredArgsConstructor;
import net.mysterria.delivery.domain.delivery.model.DeliveryResult;
import net.mysterria.delivery.domain.delivery.model.PurchaseRequest;
import net.mysterria.delivery.domain.delivery.model.VoteReward;
import net.mysterria.delivery.domain.delivery.service.DeliveryService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class DeliveryEndpoints {

    private final DeliveryService deliveryService;
    private final Logger logger;

    @OpenApi(path = "/delivery/item", methods = HttpMethod.POST, tags = "Delivery",
            summary = "Deliver an item purchase",
            description = "Runs the purchase's metadata.commands for the player, or queues them until the player joins",
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = PurchaseRequest.class), required = true),
            responses = {
                    @OpenApiResponse(status = "200", description = "Delivered, or already delivered earlier", content = @OpenApiContent(from = DeliveryResult.class)),
                    @OpenApiResponse(status = "202", description = "Player offline, queued", content = @OpenApiContent(from = DeliveryResult.class)),
                    @OpenApiResponse(status = "400", description = "Invalid purchase"),
                    @OpenApiResponse(status = "500", description = "Delivery failed, safe to retry")
            })
    @BridgeEventHandler(scopes = "delivery", logRequests = true, mainThread = true)
    public BridgeApiResponse<DeliveryResult> deliverItem(@BridgeRequestBody PurchaseRequest request) {
        return respond(() -> deliveryService.deliverItem(request));
    }

    @OpenApi(path = "/delivery/vote", methods = HttpMethod.POST, tags = "Delivery",
            summary = "Deliver a vote reward",
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = VoteReward.class), required = true),
            responses = {
                    @OpenApiResponse(status = "200", description = "Delivered, or already delivered earlier", content = @OpenApiContent(from = DeliveryResult.class)),
                    @OpenApiResponse(status = "202", description = "Player offline, queued", content = @OpenApiContent(from = DeliveryResult.class)),
                    @OpenApiResponse(status = "400", description = "Invalid vote reward"),
                    @OpenApiResponse(status = "500", description = "Delivery failed, safe to retry")
            })
    @BridgeEventHandler(scopes = "delivery", logRequests = true, mainThread = true)
    public BridgeApiResponse<DeliveryResult> deliverVote(@BridgeRequestBody VoteReward reward) {
        return respond(() -> deliveryService.deliverVote(reward));
    }

    @OpenApi(path = "/delivery/subscription", methods = HttpMethod.POST, tags = "Delivery",
            summary = "Deliver a subscription",
            description = "Adds the LuckPerms group from metadata.group; a renewal extends the running subscription",
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = PurchaseRequest.class), required = true),
            responses = {
                    @OpenApiResponse(status = "200", description = "Granted, or already granted earlier", content = @OpenApiContent(from = DeliveryResult.class)),
                    @OpenApiResponse(status = "400", description = "Invalid purchase"),
                    @OpenApiResponse(status = "500", description = "LuckPerms failed, safe to retry")
            })
    @BridgeEventHandler(scopes = "delivery", logRequests = true, mainThread = true)
    public CompletableFuture<BridgeApiResponse<DeliveryResult>> deliverSubscription(@BridgeRequestBody PurchaseRequest request) {
        return respondAsync(() -> deliveryService.deliverSubscription(request));
    }

    @OpenApi(path = "/delivery/permission", methods = HttpMethod.POST, tags = "Delivery",
            summary = "Deliver permissions",
            description = "Adds the LuckPerms permissions from metadata.permissions; a renewal extends the running ones",
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = PurchaseRequest.class), required = true),
            responses = {
                    @OpenApiResponse(status = "200", description = "Granted, or already granted earlier", content = @OpenApiContent(from = DeliveryResult.class)),
                    @OpenApiResponse(status = "400", description = "Invalid purchase"),
                    @OpenApiResponse(status = "500", description = "LuckPerms failed, safe to retry")
            })
    @BridgeEventHandler(scopes = "delivery", logRequests = true, mainThread = true)
    public CompletableFuture<BridgeApiResponse<DeliveryResult>> deliverPermission(@BridgeRequestBody PurchaseRequest request) {
        return respondAsync(() -> deliveryService.deliverPermissions(request));
    }

    private BridgeApiResponse<DeliveryResult> respond(Supplier<DeliveryResult> delivery) {
        try {
            return success(delivery.get());
        } catch (RuntimeException e) {
            return failure(e);
        }
    }

    private CompletableFuture<BridgeApiResponse<DeliveryResult>> respondAsync(Supplier<CompletableFuture<DeliveryResult>> delivery) {
        try {
            return delivery.get().handle((result, error) -> error == null ? success(result) : failure(error));
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(failure(e));
        }
    }

    private static BridgeApiResponse<DeliveryResult> success(DeliveryResult result) {
        return new BridgeApiResponse<>(true, result.getStatus().getMessage(), result, result.getStatus().getHttpStatus());
    }

    private BridgeApiResponse<DeliveryResult> failure(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (cause instanceof IllegalArgumentException) {
            return BridgeApiResponse.error(cause.getMessage(), HttpStatus.BAD_REQUEST);
        }
        logger.log(Level.SEVERE, "Delivery failed", cause);
        return BridgeApiResponse.error("Delivery failed, it is safe to retry", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
