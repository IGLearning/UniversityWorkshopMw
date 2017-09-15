package com.iggroup.universityworkshopmw.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@Builder
public class OpenPosition {
   private String id;
   private String marketId;
   private Double profitAndLoss;
   private Double openingPrice;
   private Integer buySize;
}
