package com.iggroup.universityworkshopmw.domain.services;

import com.iggroup.universityworkshopmw.domain.helpers.Helper;
import com.iggroup.universityworkshopmw.domain.exceptions.NoAvailableDataException;
import com.iggroup.universityworkshopmw.domain.model.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ClientService {

   private Map<String, Client> clientIdToClientModelMap = new ConcurrentHashMap<>();
   private final String ID_PREFIX = "client_";
   private final double INITIAL_FUNDS = 10000;

   public Client storeNewClient(Client client) {
      String uniqueId = Helper.createUniqueId(ID_PREFIX);
      Client enrichedClient = Client.builder()
         .id(uniqueId)
         .userName(client.getUserName())
         .availableFunds(INITIAL_FUNDS)
         .runningProfitAndLoss(0)
         .build();
      clientIdToClientModelMap.put(uniqueId, enrichedClient);
      log.info("Added new client={}", enrichedClient);
      return enrichedClient;
   }

   public Client getClientData(String clientId) throws NoAvailableDataException {
      Client client = getClientDataFromMap(clientId);
      log.info("Retrieving client data={} for clientId={}", client, clientId);
      return client;
   }

   public void updateAvailableFunds(String clientId, double updatedAvailableFunds) throws NoAvailableDataException {
      Client client = getClientDataFromMap(clientId);
      log.info("Updating updatedAvailableFunds for clientId={}, oldAvailableFunds={}, updatedAvailableFunds={}", clientId, client.getAvailableFunds(), updatedAvailableFunds);
      client.setAvailableFunds(updatedAvailableFunds);
      clientIdToClientModelMap.put(clientId, client);
   }

//   public void updateRunningProfitAndLoss(String clientId, double updatedProfitAndLoss) throws NoAvailableDataException {
//      Client client = getClientDataFromMap(clientId);
//      log.info("Updating updatedProfitAndLoss for clientId={}, oldAvailableFunds={}, updatedProfitAndLoss={}", clientId, client.getAvailableFunds(), updatedProfitAndLoss);
//      client.setAvailableFunds(updatedProfitAndLoss);
//      clientIdToClientModelMap.put(clientId, client);
//   }

   Client getClientDataFromMap(String clientId) throws NoAvailableDataException {
      if (clientIdToClientModelMap.containsKey(clientId)) {
         return clientIdToClientModelMap.get(clientId);
      } else {
         throw new NoAvailableDataException("No available client data in clientIdToClientModelMap for clientId=" + clientId);
      }
   }
}
