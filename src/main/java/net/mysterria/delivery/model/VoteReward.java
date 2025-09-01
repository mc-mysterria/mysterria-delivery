package net.mysterria.delivery.model;

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

    @JsonProperty("purchaseId")
    private String purchaseId;

    @JsonProperty("command")
    private String command;

    @JsonProperty("player")
    private String player;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("source")
    private String source;

    @JsonProperty("site")
    private String site;

    @JsonProperty("userId")
    private String minecraftUUID;

}