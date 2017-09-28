package com.iggroup.universityworkshopmw.integration;

import com.iggroup.universityworkshopmw.domain.model.Client;
import com.iggroup.universityworkshopmw.domain.services.ClientService;
import com.iggroup.universityworkshopmw.domain.services.OpenPositionsService;
import com.iggroup.universityworkshopmw.integration.controllers.OpenPositionsController;
import com.iggroup.universityworkshopmw.integration.dto.OpenPositionDto;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.iggroup.universityworkshopmw.TestHelper.APPLICATION_JSON_UTF8;
import static com.iggroup.universityworkshopmw.TestHelper.convertObjectToJsonBytes;
import static com.iggroup.universityworkshopmw.integration.transformers.OpenPositionTransformer.transform;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class OpenPositionFlowIntegrationTest {

   private MockMvc mockMvc;

   private OpenPositionsController openPositionsController;
   private ClientService clientService = spy(ClientService.class);
   private OpenPositionsService openPositionsService = spy(new OpenPositionsService(clientService));

   @Before
   public void setup() {
      openPositionsController = new OpenPositionsController(openPositionsService);
      mockMvc = standaloneSetup(openPositionsController).build();
   }

   @Test
   public void openPositionFlow() throws Exception {
      Client client = clientService.storeNewClient(createClient());
      String clientId = client.getId();

      //Open a position
      OpenPositionDto openPositionDto = createOpenPositionDto();
      MvcResult addOPResponse = mockMvc.perform(post("/openPositions/" + clientId)
            .contentType(APPLICATION_JSON_UTF8)
            .content(convertObjectToJsonBytes(openPositionDto))
      )
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andReturn();

      String openPositionId = JsonPath.read(addOPResponse.getResponse().getContentAsString(), "$.openPositionId");

      verify(openPositionsService, times(1)).addOpenPositionForClient(clientId, transform(openPositionDto));
      verify(clientService, times(1)).updateAvailableFunds(clientId, 9000.00);


      //Get open positions
      MvcResult mvcResult = mockMvc.perform(get("/openPositions/" + clientId)
            .contentType(APPLICATION_JSON_UTF8)
      )
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andReturn();

      String contentFromGetResponse = mvcResult.getResponse().getContentAsString();

      verify(openPositionsService, times(1)).getOpenPositionsForClient(clientId);
      assertThat(contentFromGetResponse).isEqualTo("[{\"id\":\"" + openPositionId + "\",\"marketId\":\"market_1\",\"profitAndLoss\":1234.0,\"openingPrice\":100.0,\"buySize\":10}]");

      //Delete a position
      MvcResult deleteOPResponse = mockMvc.perform(post("/openPositions/" + clientId + "/" + openPositionId + "/600")
            .contentType(APPLICATION_JSON_UTF8)
      )
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andReturn();

      String content = deleteOPResponse.getResponse().getContentAsString();

      assertThat(content).isEqualTo("5000.0");
      verify(openPositionsService, times(1)).closeOpenPosition(clientId, openPositionId, 600.0);
      verify(clientService, times(1)).updateAvailableFunds(clientId, 15000.0);

      //Verify client funds
      Client clientData = clientService.getClientData(clientId);
      assertThat(clientData.getRunningProfitAndLoss()).isEqualTo(0);
      assertThat(clientData.getAvailableFunds()).isEqualTo(15000);
   }

   private Client createClient() {
      return Client.builder()
            .id("client_1")
            .availableFunds(10000)
            .runningProfitAndLoss(10000)
            .userName("username")
            .build();
   }

   private OpenPositionDto createOpenPositionDto() {
      return OpenPositionDto.builder()
            .id("id1")
            .marketId("market_1")
            .profitAndLoss(1234.00)
            .openingPrice(100.00)
            .buySize(10)
            .build();
   }

}
