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

   public OpenPosition addOpenPositionForClient(String clientId, OpenPosition newOpenPosition) throws NoAvailableDataException, InsufficientFundsException {
      double positionPrice = newOpenPosition.getBuySize() * newOpenPosition.getOpeningPrice();
      List<OpenPosition> openPositionsForClient = clientPositionStore.get(clientId);

      double clientAvailableFunds = checkClientAvailableFunds(clientId, positionPrice);
      OpenPosition openPositionWithId = updateStoreWithNewPosition(clientId, newOpenPosition, openPositionsForClient);

//      clientService.updateFunds(clientId, clientAvailableFunds);
      // TODO: 27/09/2017 Update available funds and profit and loss?
      return openPositionWithId;
   }

   public Double closeOpenPosition(String clientId, String openPositionToClose, Double closingPrice) throws NoAvailableDataException {
      List<OpenPosition> openPositions = getPositionDataFromMap(clientId);

      OpenPosition position = openPositions.stream()
         .filter(pos -> pos.getId().equals(openPositionToClose))
         .findFirst()
         .orElseThrow(() -> new NoAvailableDataException("No position exists with id: " + openPositionToClose));

      Double closingProfitAndLoss = calculateNewProfitAndLoss(closingPrice, position.getOpeningPrice(), position.getBuySize());

      openPositions.remove(position);
      if (openPositions.isEmpty()) {
         clientPositionStore.remove(clientId);
      }

      updateClientFunds(clientId, closingProfitAndLoss);
      return closingProfitAndLoss;
   }

   public void updateMarketPrice(String marketId, Double newValue) {
      clientPositionStore.keySet().stream()
         .forEach(client -> {
            List<OpenPosition> openPositions = clientPositionStore.get(client);
            openPositions.stream()
               .filter(openPosition -> openPosition.getMarketId().equals(marketId))
               .forEach(openPosition -> updateProfitAndLoss(newValue, openPosition, openPositions));
         });
   }

   private void updateProfitAndLoss(Double newValue, OpenPosition openPosition, List<OpenPosition> openPositions) {
      Double newProfitAndLoss = calculateNewProfitAndLoss(newValue, openPosition.getOpeningPrice(), openPosition.getBuySize());
      openPositions.set(openPositions.indexOf(openPosition), createNewPosition(openPosition, newProfitAndLoss, false));
   }

   private OpenPosition createNewPosition(OpenPosition openPosition, Double profitAndLoss, boolean generateId) {
      String id = generateId ? createUniqueId("opid_") : openPosition.getId();

      return OpenPosition.builder()
         .id(id)
         .marketId(openPosition.getMarketId())
         .buySize(openPosition.getBuySize())
         .openingPrice(openPosition.getOpeningPrice())
         .profitAndLoss(profitAndLoss)
         .build();
   }

   private Double calculateNewProfitAndLoss(Double newValue, Double openingPrice, Integer buySize) {
      return (newValue - openingPrice) * buySize;
   }

   private double checkClientAvailableFunds(String clientId, double positionPrice) throws NoAvailableDataException, InsufficientFundsException {
      Client client = clientService.getClientDataFromMap(clientId);

      if (client.getAvailableFunds() < positionPrice) {
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

   private void updateClientFunds(String clientId, double closingProfitAndLoss) throws NoAvailableDataException {
      Client client = clientService.getClientDataFromMap(clientId);
      double updatedFunds = client.getAvailableFunds() + closingProfitAndLoss;
      clientService.updateAvailableFunds(clientId, updatedFunds);
      // TODO: 27/09/2017 Need to probably update available and running profit and losS?
   }
}
