package net.mysterria.delivery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.mysterria.delivery.model.source.ServiceType;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PurchaseRequest {
    
    @JsonProperty("purchaseId")
    private String purchaseId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("minecraftUuid")
    private String minecraftUuid;
    
    @JsonProperty("nickname")
    private String nickname;
    
    @JsonProperty("serviceId")
    private Integer serviceId;
    
    @JsonProperty("serviceName")
    private String serviceName;
    
    @JsonProperty("serviceType")
    private ServiceType serviceType;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("expiresAt")
    private String expiresAt;
    
    @JsonProperty("quantity")
    private Integer quantity = 1;
}