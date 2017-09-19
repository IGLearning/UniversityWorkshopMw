package com.iggroup.universityworkshopmw.integration.controllers;

import com.iggroup.universityworkshopmw.domain.exceptions.NoAvailableDataException;
import com.iggroup.universityworkshopmw.domain.model.Client;
import com.iggroup.universityworkshopmw.domain.services.ClientService;
import com.iggroup.universityworkshopmw.integration.dto.ClientDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.iggroup.universityworkshopmw.TestHelper.APPLICATION_JSON_UTF8;
import static com.iggroup.universityworkshopmw.TestHelper.convertObjectToJsonBytes;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClientControllerTest {

   private MockMvc mockMvc;

   private ClientController clientController;
   private ClientService clientService;

   @Before
   public void setup() {
      clientService = mock(ClientService.class);
      clientController = new ClientController(clientService);
      mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
   }

   @Test
   public void createClient_returnsOkCodeAndClientIdAndProfitAndLoss() throws Exception {
      ClientDto clientDto = ClientDto.builder()
         .id(null)
         .userName("userName")
         .profitAndLoss(null)
         .build();
      Client clientAdded = Client.builder()
         .id("client_12345")
         .userName("username")
         .profitAndLoss(400)
         .build();
      when(clientService.storeNewClient(any(Client.class))).thenReturn(clientAdded);

      mockMvc.perform(post("/client/createClient")
            .contentType(APPLICATION_JSON_UTF8)
            .content(convertObjectToJsonBytes(clientDto))
      )
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$.id", is("client_12345")))
            .andExpect(jsonPath("$.profitAndLoss", is(400.0)));

      ArgumentCaptor<Client> clientArgumentCaptor = forClass(Client.class);
      verify(clientService, times(1)).storeNewClient(clientArgumentCaptor.capture());
      verifyNoMoreInteractions(clientService);

      Client client = clientArgumentCaptor.getValue();
      assertNull(client.getId());
      assertThat(client.getProfitAndLoss(), is(0.0));
      assertThat(client.getUserName(), is("userName"));
   }

   @Test
   public void createClient_handlesAnyException_returnsServerErrorAndInfoString() throws Exception {
      ClientDto clientDto = ClientDto.builder()
         .id(null)
         .userName("userName")
         .profitAndLoss(null)
         .build();
      when(clientService.storeNewClient(any(Client.class))).thenThrow(new RuntimeException("Server exception!"));

      MvcResult mvcResult = mockMvc.perform(post("/client/createClient")
            .contentType(APPLICATION_JSON_UTF8)
            .content(convertObjectToJsonBytes(clientDto))
      )
            .andExpect(status().isInternalServerError()).andReturn();

      final String content = mvcResult.getResponse().getContentAsString();
      assertEquals("Something went wrong when creating a new client", content);
   }

   @Test
   public void getClientProfitAndLoss_returnsOkCodeAndClientProfitAndLoss() throws Exception {
      when(clientService.getProfitAndLoss(anyString())).thenReturn(400.0);

      MvcResult mvcResult = mockMvc.perform(get("/client/profitAndLoss/client_12345"))
            .andExpect(status().isOk()).andReturn();

      final String content = mvcResult.getResponse().getContentAsString();
      assertEquals("400.0", content);

      ArgumentCaptor<String> clientIdCaptor = forClass(String.class);
      verify(clientService, times(1)).getProfitAndLoss(clientIdCaptor.capture());
      verifyNoMoreInteractions(clientService);

      String id = clientIdCaptor.getValue();
      assertThat(id, is("client_12345"));
   }

   @Test
   public void getClientProfitAndLoss_handlesAnyException_returnsServerErrorAndInfoString() throws Exception {
      when(clientService.getProfitAndLoss(anyString())).thenThrow(new RuntimeException("Server exception!"));

      MvcResult mvcResult = mockMvc.perform(get("/client/profitAndLoss/client_12345"))
            .andExpect(status().isInternalServerError()).andReturn();

      final String content = mvcResult.getResponse().getContentAsString();
      assertEquals("Something went wrong when retrieving profit and loss", content);
   }

   @Test
   public void getClientProfitAndLoss_handlesNoAvailableDataException_returnsServerErrorAndInfoString() throws Exception {
      when(clientService.getProfitAndLoss(anyString())).thenThrow(new NoAvailableDataException("No available data!"));

      MvcResult mvcResult = mockMvc.perform(get("/client/profitAndLoss/client_12345"))
            .andExpect(status().isNotFound()).andReturn();

      final String content = mvcResult.getResponse().getContentAsString();
      assertEquals("No available client data for clientId=client_12345", content);
   }
}