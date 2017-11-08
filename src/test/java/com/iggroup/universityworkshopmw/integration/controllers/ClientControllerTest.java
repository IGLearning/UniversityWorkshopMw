package com.iggroup.universityworkshopmw.integration.controllers;

import com.iggroup.universityworkshopmw.domain.exceptions.NoAvailableDataException;
import com.iggroup.universityworkshopmw.domain.model.Client;
import com.iggroup.universityworkshopmw.domain.services.ClientService;
import com.iggroup.universityworkshopmw.integration.dto.ClientDto;
import com.iggroup.universityworkshopmw.integration.dto.CreateClientDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.iggroup.universityworkshopmw.TestHelper.APPLICATION_JSON_UTF8;
import static com.iggroup.universityworkshopmw.TestHelper.convertObjectToJsonBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
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

   private ClientController clientController;
   private ClientService clientService;

   @Before
   public void setup() {
      clientService = mock(ClientService.class);
      clientController = new ClientController(clientService);
   }

   @Test
   public void createClient_returnsOkCodeAndClientIdAndFunds() throws Exception {
      CreateClientDto createClientDto = CreateClientDto.builder()
            .userName("userName")
            .build();
      Client clientAdded = Client.builder()
            .id("client_12345")
            .userName("username")
            .availableFunds(400.0)
            .runningProfitAndLoss(0)
            .build();
      when(clientService.storeNewClient(any(Client.class))).thenReturn(clientAdded);

      final ResponseEntity<?> responseEntity = clientController.createClient(createClientDto);
      final ClientDto body = (ClientDto) responseEntity.getBody();
      assertThat(body.getId()).isEqualTo("client_12345");
      assertThat(body.getAvailableFunds()).isEqualTo(400.0);
      assertThat(body.getRunningProfitAndLoss()).isEqualTo(0);

      ArgumentCaptor<Client> clientArgumentCaptor = forClass(Client.class);
      verify(clientService, times(1)).storeNewClient(clientArgumentCaptor.capture());
      verifyNoMoreInteractions(clientService);

      Client client = clientArgumentCaptor.getValue();
      assertThat(client.getId()).isNull();
      assertThat(client.getAvailableFunds()).isEqualTo(0.0);
      assertThat(client.getRunningProfitAndLoss()).isEqualTo(0.0);
      assertThat(client.getUserName()).isEqualTo("userName");
   }

   @Test
   public void createClient_handlesAnyException_returnsServerErrorAndInfoString() throws Exception {
      CreateClientDto createClientDto = CreateClientDto.builder()
            .userName("userName")
            .build();
      when(clientService.storeNewClient(any(Client.class))).thenThrow(new RuntimeException("Server exception!"));

      final ResponseEntity<?> responseEntity = clientController.createClient(createClientDto);
      assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

      assertThat("Something went wrong when creating a new client").isEqualTo(responseEntity.getBody());
   }

   @Test
   public void getClient_returnsOkCodeAndClientData() throws Exception {
      final String clientId = "client_12345";
      Client retrievedClient = Client.builder()
            .id("client_12345")
            .userName("username")
            .availableFunds(400.0)
            .runningProfitAndLoss(0)
            .build();
      when(clientService.getClientData(anyString())).thenReturn(retrievedClient);

      final ResponseEntity<?> responseEntity = clientController.getClient(clientId);
      final ClientDto body = (ClientDto) responseEntity.getBody();
      assertThat(body.getId()).isEqualTo("client_12345");
      assertThat(body.getUserName()).isEqualTo("username");
      assertThat(body.getAvailableFunds()).isEqualTo(400.0);
      assertThat(body.getRunningProfitAndLoss()).isEqualTo(0);
      assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

      ArgumentCaptor<String> clientIdCaptor = forClass(String.class);
      verify(clientService, times(1)).getClientData(clientIdCaptor.capture());
      verifyNoMoreInteractions(clientService);

      String id = clientIdCaptor.getValue();
      assertThat(id).isEqualTo("client_12345");
   }

   @Test
   public void getClient_handlesAnyException_returnsServerErrorAndInfoString() throws Exception {
      final String clientId = "client_12345";
      when(clientService.getClientData(anyString())).thenThrow(new RuntimeException("Server exception!"));

      final ResponseEntity<?> responseEntity = clientController.getClient(clientId);
      assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat("Something went wrong when retrieving client data").isEqualTo(responseEntity.getBody());
   }

   @Test
   public void getClient_handlesNoAvailableDataException_returnsServerErrorAndInfoString() throws Exception {
      when(clientService.getClientData(anyString())).thenThrow(new NoAvailableDataException("No available data!"));

      final ResponseEntity<?> responseEntity = clientController.getClient("unknownId");
      assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat("No available client data for clientId=unknownId").isEqualTo(responseEntity.getBody());
   }
}