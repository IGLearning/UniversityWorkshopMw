package com.iggroup.universityworkshopmw.domain.services;

import com.iggroup.universityworkshopmw.domain.exceptions.NoAvailableDataException;
import com.iggroup.universityworkshopmw.domain.model.Client;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.core.Is.is;

public class ClientServiceTest {

   private ClientService clientService;

   @Before
   public void setUp() {
      clientService = new ClientService();
   }

   @Test
   public void storeNewClient_assignsUniqueIdPerAddition() {
      Client client1 = createClient("", "userName1", 0, 0);
      Client client2 = createClient("", "userName2", 0, 0);

      Client returnClient1 = clientService.storeNewClient(client1);
      Client returnClient2 = clientService.storeNewClient(client2);

      assertThat(returnClient1.getId()).isNotNull();
      assertThat(returnClient2.getId()).isNotNull();
      assertThat(returnClient1.getId()).isNotEqualTo(returnClient2.getId());
   }

   @Test
   public void getClientData_getsDataForClientId() throws NoAvailableDataException {
      final Client expected = createClient("", "userName1", 0, 0);
      Client returnClient1 = clientService.storeNewClient(expected);
      String clientId = returnClient1.getId();

      Client actual = clientService.getClientData(clientId);

      assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
   }

   @Test(expected = NoAvailableDataException.class)
   public void getClientData_handlesMapContainingNoClientDataForClientId() throws NoAvailableDataException {
      String clientId = "randomIdNotInMap";

      clientService.getClientData(clientId);
   }

   @Test
   public void updateAvailableFunds_updatesAvailableFunds() throws NoAvailableDataException {
      Client returnClient1 = clientService.storeNewClient(createClient("", "userName1", 0, 0));
      String clientId = returnClient1.getId();
      double initialProfitAndLoss = returnClient1.getAvailableFunds();
      double fundsUpdate = initialProfitAndLoss - 200;

      clientService.updateAvailableFunds(clientId, fundsUpdate);

      double returnedFunds = clientService.getClientData(clientId).getAvailableFunds();
      assertThat(fundsUpdate).isEqualTo(returnedFunds);
   }

   @Test(expected = NoAvailableDataException.class)
   public void updateAvailableFunds_handlesMapContainingNoClientDataForClientId() throws NoAvailableDataException {
      String clientId = "randomIdNotInMap";

      clientService.updateAvailableFunds(clientId, 900);
   }

   private Client createClient(String clientId, String userName, double funds, double profitAndLoss) {
      return Client.builder()
         .id(clientId)
         .userName(userName)
         .availableFunds(funds)
         .runningProfitAndLoss(funds)
         .build();
   }
}