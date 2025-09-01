package net.mysterria.delivery.api;

import dev.ua.ikeepcalm.catwalk.bridge.annotations.BridgeEventHandler;
import dev.ua.ikeepcalm.catwalk.bridge.annotations.BridgeRequestBody;
import dev.ua.ikeepcalm.catwalk.bridge.source.BridgeApiResponse;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import net.mysterria.delivery.manager.DeliveryManager;
import net.mysterria.delivery.model.DeliveryResponse;
import net.mysterria.delivery.model.PurchaseRequest;
import net.mysterria.delivery.model.source.ServiceType;

import java.util.concurrent.CompletableFuture;

public class DiscordRoleDeliveryEndpoint {

    private final DeliveryManager deliveryManager;

    public DiscordRoleDeliveryEndpoint(DeliveryManager deliveryManager) {
        this.deliveryManager = deliveryManager;
    }

    @OpenApi(
            path = "/delivery/discord-role",
            methods = HttpMethod.POST,
            summary = "Process Discord role purchase",
            description = "Logs Discord role purchases and announces them",
            tags = {"Delivery"},
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = PurchaseRequest.class),
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", description = "Discord role logged",
                            content = @OpenApiContent(from = DeliveryResponse.class)),
                    @OpenApiResponse(status = "400", description = "Invalid request"),
                    @OpenApiResponse(status = "500", description = "Internal server error")
            }
    )
    @BridgeEventHandler(requiresAuth = true, description = "Process Discord role", logRequests = true)
    public CompletableFuture<BridgeApiResponse<DeliveryResponse>> processDiscordRole(@BridgeRequestBody PurchaseRequest request) {
        if (request.getServiceType() != ServiceType.DISCORD_ROLE) {
            return CompletableFuture.completedFuture(
                    BridgeApiResponse.error("Invalid service type for Discord role", HttpStatus.BAD_REQUEST)
            );
        }

        return deliveryManager.processPurchase(request)
                .thenApply(BridgeApiResponse::success)
                .exceptionally(e -> BridgeApiResponse.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
}