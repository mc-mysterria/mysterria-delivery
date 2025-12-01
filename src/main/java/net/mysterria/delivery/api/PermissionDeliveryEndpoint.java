package net.mysterria.delivery.api;

import dev.ua.ikeepcalm.catwalk.bridge.annotations.BridgeEventHandler;
import dev.ua.ikeepcalm.catwalk.bridge.annotations.BridgeRequestBody;
import dev.ua.ikeepcalm.catwalk.bridge.source.BridgeApiResponse;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import net.mysterria.delivery.manager.DeliveryManager;
import net.mysterria.delivery.model.DeliveryResponse;
import net.mysterria.delivery.model.PurchaseRequest;

import java.util.concurrent.CompletableFuture;

public class PermissionDeliveryEndpoint {

    private final DeliveryManager deliveryManager;

    public PermissionDeliveryEndpoint(DeliveryManager deliveryManager) {
        this.deliveryManager = deliveryManager;
    }

    @OpenApi(
            path = "/delivery/permission",
            methods = HttpMethod.POST,
            summary = "Deliver permission purchase",
            description = "Processes permission purchases and grants LuckPerms permissions",
            tags = {"Delivery"},
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = PurchaseRequest.class),
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", description = "Permission granted",
                            content = @OpenApiContent(from = DeliveryResponse.class)),
                    @OpenApiResponse(status = "400", description = "Invalid request"),
                    @OpenApiResponse(status = "500", description = "Internal server error")
            }
    )
    @BridgeEventHandler(requiresAuth = true, description = "Deliver permission", logRequests = true)
    public CompletableFuture<BridgeApiResponse<DeliveryResponse>> deliverPermission(@BridgeRequestBody PurchaseRequest request) {
        return deliveryManager.processPermissionDelivery(request)
                .thenApply(BridgeApiResponse::success)
                .exceptionally(e -> BridgeApiResponse.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
