package com.iggroup.universityworkshopmw.domain.services;

import com.iggroup.universityworkshopmw.domain.exceptions.DuplicatedDataException;
import com.iggroup.universityworkshopmw.domain.exceptions.NoAvailableDataException;
import com.iggroup.universityworkshopmw.domain.helpers.Helper;
import com.iggroup.universityworkshopmw.domain.model.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.iggroup.universityworkshopmw.domain.helpers.Helper.roundToTwoDecimalPlaces;

@Slf4j
@Component
public class ClientService {

   private Map<String, Client> clientIdToClientModelMap = new ConcurrentHashMap<>();
   private final String ID_PREFIX = "client_";
   private final double INITIAL_FUNDS = 10000;

   public Client storeNewClient(Client client) throws DuplicatedDataException {
      checkIfDuplicateUsername(client);

      String uniqueId = checkIfDuplicateClientId();

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
      client.setAvailableFunds(updatedAvailableFunds);
      clientIdToClientModelMap.put(client.getId(), client);
   }

   public Map<String, Client> getAllClients() {
      return clientIdToClientModelMap;
   }

   void updateRunningProfitAndLoss(String clientId, double sumOfPositionProfitAndLoss) throws NoAvailableDataException {
      Client client = getClientDataFromMap(clientId);
      double oldProfitAndLoss = client.getRunningProfitAndLoss();

      client.setRunningProfitAndLoss(roundToTwoDecimalPlaces(sumOfPositionProfitAndLoss));
      clientIdToClientModelMap.put(clientId, client);

      double newAvailableFunds = (client.getAvailableFunds() - oldProfitAndLoss) + sumOfPositionProfitAndLoss;
      updateAvailableFunds(clientId, roundToTwoDecimalPlaces(newAvailableFunds));
   }

   private Client getClientDataFromMap(String clientId) throws NoAvailableDataException {
      if (clientIdToClientModelMap.containsKey(clientId)) {
         return clientIdToClientModelMap.get(clientId);
      } else {
         throw new NoAvailableDataException("No available client data in clientIdToClientModelMap for clientId=" + clientId);
      }
   }

   private String checkIfDuplicateClientId() {
      String uniqueId;
      do {
         uniqueId = Helper.createUniqueId(ID_PREFIX);
      } while (clientIdToClientModelMap.containsKey(uniqueId));
      return uniqueId;
   }

   private void checkIfDuplicateUsername(Client client) throws DuplicatedDataException {
      Optional<Client> duplicatedUsername = clientIdToClientModelMap.values()
            .stream()
            .filter(c -> c.getUserName().equals(client.getUserName()))
            .findFirst();
      if (duplicatedUsername.isPresent()) {
         throw new DuplicatedDataException("Duplicated username found: " + client.getUserName());
      }
   }

}
