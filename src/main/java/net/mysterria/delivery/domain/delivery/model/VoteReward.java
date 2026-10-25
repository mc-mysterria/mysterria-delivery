package net.mysterria.delivery.domain.delivery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoteReward {

    private String purchaseId;
    private String command;
    private String player;
    private String amount;
    private String source;
    private String site;
    @JsonProperty("userId")
    private String minecraftUUID;
}
