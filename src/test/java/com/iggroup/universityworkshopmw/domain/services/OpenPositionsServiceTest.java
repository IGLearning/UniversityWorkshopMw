package com.iggroup.universityworkshopmw.domain.services;

import com.iggroup.universityworkshopmw.domain.exceptions.InsufficientFundsException;
import com.iggroup.universityworkshopmw.domain.exceptions.NoAvailableDataException;
import com.iggroup.universityworkshopmw.domain.model.Client;
import com.iggroup.universityworkshopmw.domain.model.OpenPosition;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenPositionsServiceTest {

   private OpenPosition openPosition1, openPosition2, openPosition3, openPosition4, openPosition5, openPosition6;
   private OpenPositionsService openPositionsService;
   private ClientService clientService;

   @Before
   public void setUp() {
      clientService = mock(ClientService.class);
      openPositionsService = new OpenPositionsService(clientService);
      initialiseOpenPositions();
   }

   @Test
   public void openPositionsAreCorrectForClient() throws Exception{
      initialiseClientPositions();
      List<OpenPosition> openPositions = openPositionsService.getOpenPositionsForClient("client_2");

      assertOpenPosition(openPositions.get(1), openPosition2);
      assertOpenPosition(openPositions.get(0), openPosition3);
      assertOpenPosition(openPositions.get(2), openPosition5);
   }

   @Test(expected = NoAvailableDataException.class)
   public void getShouldReturnNullIfClientHasNoOpenPositions() throws Exception {
      assertNull(openPositionsService.getOpenPositionsForClient("client_1"));
   }

   @Test
   public void shouldAddOpenPositionsForClientWithNoPositions() throws Exception {
      when(clientService.getClientDataFromMap("client_1")).thenReturn(createClient("client_1"));

      openPositionsService.addOpenPositionForClient("client_1", openPosition1);

      List<OpenPosition> positions = asList(openPosition1);
      List<OpenPosition> clientPositions = openPositionsService.getOpenPositionsForClient("client_1");
      IntStream.range(0, clientPositions.size())
         .forEach(idx -> {
            try {
               assertOpenPosition(clientPositions.get(idx), positions.get(idx));
            } catch (Exception e) {
               e.printStackTrace();
            }
         });
   }

   @Test
   public void shouldAddNewPositionForClientWithExistingPositions() throws Exception {
      when(clientService.getClientDataFromMap("client_3")).thenReturn(createClient("client_3"));

      openPositionsService.addOpenPositionForClient("client_3", openPosition1);
      openPositionsService.addOpenPositionForClient("client_3", openPosition4);

      List<OpenPosition> clientPositions = openPositionsService.getOpenPositionsForClient("client_3");

      assertOpenPosition(clientPositions.get(0), openPosition1);
      assertOpenPosition(clientPositions.get(1), openPosition4);
   }

   @Test
   public void shouldUpdateProfitAndLossForAllClientsWhenPriceIncreases() throws Exception {
      initialiseClientPositions();
      openPositionsService.updateMarketPrice("market_2", 200.00);

      List<OpenPosition> client1Positions = openPositionsService.getOpenPositionsForClient("client_1");
      List<OpenPosition> client2Positions = openPositionsService.getOpenPositionsForClient("client_2");

      assertThat(client1Positions.get(1).getProfitAndLoss(), is(60.00));
      assertThat(client2Positions.get(1).getProfitAndLoss(), is(60.00));
   }

   @Test
   public void shouldUpdateProfitAndLossForAllClientsWhenPriceDecreases() throws Exception {
      initialiseClientPositions();
      openPositionsService.updateMarketPrice("market_2", 100.00);

      List<OpenPosition> client1Positions = openPositionsService.getOpenPositionsForClient("client_1");
      List<OpenPosition> client2Positions = openPositionsService.getOpenPositionsForClient("client_2");

      assertThat(client1Positions.get(1).getProfitAndLoss(), is(-40.00));
      assertThat(client2Positions.get(1).getProfitAndLoss(), is(-40.00));
   }

   @Test
   public void shouldUpdateProfitAndLossForMultiplePositionsOnTheSameMarket() throws Exception {
      initialiseClientPositions();
      openPositionsService.addOpenPositionForClient("client_1", openPosition6);

      openPositionsService.updateMarketPrice("market_1", 200.00);
      List<OpenPosition> openPositions = openPositionsService.getOpenPositionsForClient("client_1");

      assertThat(openPositions.get(0).getProfitAndLoss(), is(1200.00));
      assertThat(openPositions.get(2).getProfitAndLoss(), is(-600.00));
   }

   @Test
   public void shouldUpdateProfitAndLossForMultiplePositionsOnTheSameMarketForMultipleClients() throws Exception {
      initialiseClientPositions();
      openPositionsService.addOpenPositionForClient("client_1", openPosition6);
      openPositionsService.addOpenPositionForClient("client_2", openPosition6);
      openPositionsService.addOpenPositionForClient("client_2", openPosition1);

      openPositionsService.updateMarketPrice("market_1", 200.00);
      List<OpenPosition> client1Positions = openPositionsService.getOpenPositionsForClient("client_1");
      List<OpenPosition> client2Positions = openPositionsService.getOpenPositionsForClient("client_2");

      assertThat(client1Positions.get(0).getProfitAndLoss(), is(1200.00));
      assertThat(client1Positions.get(2).getProfitAndLoss(), is(-600.00));
      assertThat(client2Positions.get(3).getProfitAndLoss(), is(-600.00));
      assertThat(client2Positions.get(4).getProfitAndLoss(), is(1200.00));
   }

   @Test
   public void shouldCloseSpecifiedPosition() throws Exception {
      initialiseClientPositions();
      List<OpenPosition> clientPositions = openPositionsService.getOpenPositionsForClient("client_1");

      assertOpenPosition(clientPositions.get(0), openPosition1);
      assertOpenPosition(clientPositions.get(1), openPosition2);

      Double finalProfitAndLoss = openPositionsService.closeOpenPosition("client_1", clientPositions.get(0).getId(), 200.00);
      clientPositions = openPositionsService.getOpenPositionsForClient("client_1");

      assertThat(finalProfitAndLoss, is(1200.00));
      assertOpenPosition(clientPositions.get(0), openPosition2);
      assertFalse(clientPositions.contains(openPosition1));
   }

   @Test(expected = InsufficientFundsException.class)
   public void shouldThrowExceptionIfClientLacksFundsToTrade() throws Exception {
      when(clientService.getClientDataFromMap("client_1")).thenReturn(Client.builder()
         .id("client_1")
         .userName("username")
         .availableFunds(1)
         .build());

      openPositionsService.addOpenPositionForClient("client_1", openPosition1);
   }

   @Test(expected = NoAvailableDataException.class)
   public void shouldThrowExceptionWhenClosingPositionThatDoesntExist() throws Exception {
      openPositionsService.closeOpenPosition("client_1", "made_up_position", 10.0);
   }

   private void initialiseOpenPositions() {
      openPosition1 = OpenPosition.builder()
         .id("pos_1")
         .marketId("market_1")
         .profitAndLoss(1234.55)
         .openingPrice(100.00)
         .buySize(12)
         .build();

      openPosition2 = OpenPosition.builder()
         .id("pos_2")
         .marketId("market_2")
         .profitAndLoss(124.12)
         .openingPrice(140.00)
         .buySize(1)
         .build();

      openPosition3 = OpenPosition.builder()
         .id("pos_3")
         .marketId("market_3")
         .profitAndLoss(12.1)
         .openingPrice(50.5)
         .buySize(70)
         .build();

      openPosition4 = OpenPosition.builder()
         .id("pos_4")
         .marketId("market_1")
         .profitAndLoss(134.00)
         .openingPrice(10.5)
         .buySize(100)
         .build();

      openPosition5 = OpenPosition.builder()
         .id("pos_5")
         .marketId("market_4")
         .profitAndLoss(543.98)
         .openingPrice(234.42)
         .buySize(50)
         .build();

      openPosition6 = OpenPosition.builder()
         .id("pos_6")
         .marketId("market_1")
         .profitAndLoss(12.00)
         .openingPrice(250.00)
         .buySize(12)
         .build();
   }

   private void initialiseClientPositions() throws Exception {
      when(clientService.getClientDataFromMap("client_1")).thenReturn(createClient("client_1"));
      when(clientService.getClientDataFromMap("client_2")).thenReturn(createClient("client_2"));
      when(clientService.getClientDataFromMap("client_3")).thenReturn(createClient("client_3"));

      newArrayList(openPosition1, openPosition2).stream()
         .forEach(openPosition -> {
            try {
               openPositionsService.addOpenPositionForClient("client_1", openPosition);
            } catch (NoAvailableDataException e) {
               e.printStackTrace();
            }
         });

      newArrayList(openPosition3, openPosition2, openPosition5).stream()
         .forEach(openPosition -> {
            try {
               openPositionsService.addOpenPositionForClient("client_2", openPosition);
            } catch (NoAvailableDataException e) {
               e.printStackTrace();
            }
         });

      newArrayList(openPosition4).stream()
         .forEach(openPosition -> {
            try {
               openPositionsService.addOpenPositionForClient("client_3", openPosition);
            } catch (NoAvailableDataException e) {
               e.printStackTrace();
            }
         });
   }

   private Client createClient(String clientId) {
      return Client.builder()
         .id(clientId)
         .userName("username")
         .availableFunds(12345)
         .runningProfitAndLoss(500)
         .build();
   }

   private void assertOpenPosition(OpenPosition resultingOpenPosition, OpenPosition openPosition) {
      assertEquals(resultingOpenPosition.getProfitAndLoss(), openPosition.getProfitAndLoss());
      assertEquals(resultingOpenPosition.getBuySize(), openPosition.getBuySize());
      assertEquals(resultingOpenPosition.getMarketId(), openPosition.getMarketId());
      assertEquals(resultingOpenPosition.getOpeningPrice(), openPosition.getOpeningPrice());
   }

}
