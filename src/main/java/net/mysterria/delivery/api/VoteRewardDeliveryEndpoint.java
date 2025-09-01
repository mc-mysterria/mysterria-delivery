package net.mysterria.delivery.api;

import dev.ua.ikeepcalm.catwalk.bridge.annotations.BridgeEventHandler;
import dev.ua.ikeepcalm.catwalk.bridge.annotations.BridgeRequestBody;
import dev.ua.ikeepcalm.catwalk.bridge.source.BridgeApiResponse;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import net.mysterria.delivery.manager.DeliveryManager;
import net.mysterria.delivery.model.DeliveryResponse;
import net.mysterria.delivery.model.VoteReward;

import java.util.concurrent.CompletableFuture;

public class VoteRewardDeliveryEndpoint {

    private final DeliveryManager deliveryManager;

    public VoteRewardDeliveryEndpoint(DeliveryManager deliveryManager) {
        this.deliveryManager = deliveryManager;
    }

    @OpenApi(
            path = "/delivery/vote",
            methods = HttpMethod.POST,
            summary = "Deliver vote reward purchase",
            description = "Processes votes and delivers rewards to the player",
            tags = {"Delivery"},
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = VoteReward.class),
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", description = "Delivery processed",
                            content = @OpenApiContent(from = DeliveryResponse.class)),
                    @OpenApiResponse(status = "400", description = "Invalid request"),
                    @OpenApiResponse(status = "500", description = "Internal server error")
            }
    )
    @BridgeEventHandler(requiresAuth = true, description = "Deliver item purchase", logRequests = true)
    public CompletableFuture<BridgeApiResponse<DeliveryResponse>> deliverItem(@BridgeRequestBody VoteReward request) {
        if (request.getCommand() == null) {
            return CompletableFuture.completedFuture(
                    BridgeApiResponse.error("Invalid service type for item delivery", HttpStatus.BAD_REQUEST)
            );
        }

        return deliveryManager.processPurchase(request)
                .thenApply(BridgeApiResponse::success)
                .exceptionally(e -> BridgeApiResponse.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
}