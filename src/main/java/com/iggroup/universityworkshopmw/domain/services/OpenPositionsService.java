package com.iggroup.universityworkshopmw.domain.services;

import com.iggroup.universityworkshopmw.domain.exceptions.InsufficientFundsException;
import com.iggroup.universityworkshopmw.domain.exceptions.NoAvailableDataException;
import com.iggroup.universityworkshopmw.domain.model.Client;
import com.iggroup.universityworkshopmw.domain.model.OpenPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.collect.Lists.newArrayList;
import static com.iggroup.universityworkshopmw.domain.helpers.Helper.createUniqueId;

@Slf4j
@Component
public class OpenPositionsService {
   private final ClientService clientService;
   private ConcurrentMap<String, List<OpenPosition>> clientPositionStore;

   public OpenPositionsService(ClientService clientService) {
      this.clientService = clientService;
      clientPositionStore = new ConcurrentHashMap<>();

   }

   public List<OpenPosition> getOpenPositionsForClient(String clientId) throws NoAvailableDataException {
      List<OpenPosition> openPositions = getPositionDataFromMap(clientId);

      if (openPositions != null) {
         return openPositions;
      }
      throw new NoAvailableDataException("No open positions exist for client: " + clientId);
   }

   //Keep order: 1)Check funds 2)Add OpenPosition 3)Update client funds
   public OpenPosition addOpenPositionForClient(String clientId, OpenPosition newOpenPosition) throws NoAvailableDataException, InsufficientFundsException {
      double positionPrice = getPositionOpeningPrice(newOpenPosition);
      List<OpenPosition> openPositionsForClient = clientPositionStore.get(clientId);

      double clientAvailableFunds = checkClientAvailableFunds(clientId, positionPrice);
      OpenPosition openPositionWithId = updateStoreWithNewPosition(clientId, newOpenPosition, openPositionsForClient);

      double newAvailableFunds = calculateNewAvailableFunds(clientAvailableFunds, positionPrice);
      clientService.updateAvailableFunds(clientId, newAvailableFunds);

      return openPositionWithId;
   }

   public Double closeOpenPosition(String clientId, String openPositionToClose, double closingPrice) throws NoAvailableDataException {
      List<OpenPosition> openPositions = getPositionDataFromMap(clientId);

      OpenPosition position = openPositions.stream()
         .filter(pos -> pos.getId().equals(openPositionToClose))
         .findFirst()
         .orElseThrow(() -> new NoAvailableDataException("No position exists with id: " + openPositionToClose));

      double openingPositionPrice = getPositionOpeningPrice(position);
      Double closingProfitAndLoss = calculateNewProfitAndLoss(closingPrice, position.getOpeningPrice(), position.getBuySize());

      openPositions.remove(position);
      if (openPositions.isEmpty()) {
         clientPositionStore.remove(clientId);
      }

      calculateAndUpdateRunningProfitAndLoss(clientId, clientPositionStore.get(clientId));
      updateClientFunds(clientId, closingProfitAndLoss, openingPositionPrice);
      return closingProfitAndLoss;
   }

   public void updateMarketPrice(String marketId, Double newValue) {
      clientPositionStore.keySet().stream()
         .forEach(clientId -> {
            List<OpenPosition> openPositions = clientPositionStore.get(clientId);
            openPositions.stream()
               .filter(openPosition -> openPosition.getMarketId().equals(marketId))
               .forEach(openPosition -> updateProfitAndLoss(newValue, openPosition, openPositions));

            if (openPositions.stream().anyMatch(openPosition -> openPosition.getMarketId().equals(marketId))) {
               calculateAndUpdateRunningProfitAndLoss(clientId, openPositions);
            }
         });
   }

   private void calculateAndUpdateRunningProfitAndLoss(String clientId, List<OpenPosition> openpositionsForClient) {
      double sumOfPositionProfitAndLoss;
      if (openpositionsForClient == null) {
         sumOfPositionProfitAndLoss = 0;
      } else {
         sumOfPositionProfitAndLoss = openpositionsForClient.stream().map(OpenPosition::getProfitAndLoss).mapToDouble(d -> d).sum();
      }
      try {
         clientService.updateRunningProfitAndLoss(clientId, sumOfPositionProfitAndLoss);
      } catch (NoAvailableDataException e) {
         log.error("Could not update running profit and loss and available funds for client, when updating market price, as no available data for clientId={}. Exception={}", clientId, e.getMessage());
      }
   }

   private double getPositionOpeningPrice(OpenPosition newOpenPosition) {
      return newOpenPosition.getBuySize() * newOpenPosition.getOpeningPrice();
   }

   private double calculateNewAvailableFunds(double clientAvailableFunds, double positionPrice) {
      return (clientAvailableFunds - positionPrice);
   }

   private void updateProfitAndLoss(Double newValue, OpenPosition openPosition, List<OpenPosition> openPositions) {
      Double newProfitAndLoss = calculateNewProfitAndLoss(newValue, openPosition.getOpeningPrice(), openPosition.getBuySize());
      openPositions.set(openPositions.indexOf(openPosition), createNewPosition(openPosition, newProfitAndLoss, false));
   }

   private OpenPosition createNewPosition(OpenPosition openPosition, Double profitAndLoss, boolean generateId) {
      String id = checkForDuplicateOpenPositionId(openPosition, generateId);

      return OpenPosition.builder()
         .id(id)
         .marketId(openPosition.getMarketId())
         .buySize(openPosition.getBuySize())
         .openingPrice(openPosition.getOpeningPrice())
         .profitAndLoss(profitAndLoss)
         .build();
   }

   private String checkForDuplicateOpenPositionId(OpenPosition openPosition, boolean generateId) {
      String id;
      do {
         id = generateId ? createUniqueId("opid_") : openPosition.getId();
      } while (generateId && clientPositionStore.containsKey(id));
      return id;
   }

   private Double calculateNewProfitAndLoss(double newValue, double openingPrice, int buySize) {
      return (double) Math.round(((newValue - openingPrice) * buySize) * 100) / 100;
   }

   private double checkClientAvailableFunds(String clientId, double positionPrice) throws NoAvailableDataException, InsufficientFundsException {
      Client client = clientService.getClientData(clientId);

      if ((client.getAvailableFunds()) < positionPrice) {
         throw new InsufficientFundsException("Client: " + clientId + " lacks sufficient funds to place that trade");
      }
      return client.getAvailableFunds();
   }

   private OpenPosition updateStoreWithNewPosition(String clientId, OpenPosition newOpenPosition, List<OpenPosition> openPositionsForClient) {
      OpenPosition openPosition = createNewPosition(newOpenPosition, newOpenPosition.getProfitAndLoss(), true);
      if (openPositionsForClient != null) {
         openPositionsForClient.add(openPosition);
      } else {
         clientPositionStore.put(clientId, newArrayList(openPosition));
      }
      return openPosition;
   }

   private List<OpenPosition> getPositionDataFromMap(String clientId) throws NoAvailableDataException {
      if (clientPositionStore.containsKey(clientId)) {
         return clientPositionStore.get(clientId);
      }
      throw new NoAvailableDataException("No positions available for client: " + clientId);
   }

   private void updateClientFunds(String clientId, double closingProfitAndLoss, double openingPositionPrice) throws NoAvailableDataException {
      Client client = clientService.getClientData(clientId);
      double updatedFunds = client.getAvailableFunds() + openingPositionPrice + closingProfitAndLoss;
      clientService.updateAvailableFunds(clientId, updatedFunds);
   }
}
