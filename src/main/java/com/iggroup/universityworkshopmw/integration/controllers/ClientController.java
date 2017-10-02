package com.iggroup.universityworkshopmw.integration.controllers;

import com.iggroup.universityworkshopmw.domain.exceptions.DuplicatedDataException;
import com.iggroup.universityworkshopmw.domain.exceptions.NoAvailableDataException;
import com.iggroup.universityworkshopmw.domain.model.Client;
import com.iggroup.universityworkshopmw.domain.services.ClientService;
import com.iggroup.universityworkshopmw.integration.dto.ClientDto;
import com.iggroup.universityworkshopmw.integration.transformers.ClientDtoTransformer;
import com.iggroup.universityworkshopmw.integration.transformers.ClientTransformer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Api(value = "/client", description = "Operations relating to the client")
@Slf4j
@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {

   private final ClientService clientService;

   @ApiOperation(value = "Create a new client",
      notes = "Creates a single client",
      response = ClientDto.class)
   @ApiResponses(value = {
      @ApiResponse(code = HTTP_OK,
         message = "Successfully created a client"),
      @ApiResponse(code = HTTP_BAD_REQUEST,
         message = "Couldn't recognise request"),
      @ApiResponse(code = HTTP_BAD_GATEWAY,
         message = "Couldn't create client")
   })
   @PostMapping("/createClient")
   public ResponseEntity<?> createClient(@RequestBody ClientDto clientDto) {
      try {
         Client clientTransformed = ClientTransformer.transform(clientDto);
         ClientDto responseBody = ClientDtoTransformer.transform(clientService.storeNewClient(clientTransformed));
         return new ResponseEntity<>(responseBody, OK);

      } catch (DuplicatedDataException e) {
         String userName = clientDto.getUserName();
         log.info("Duplicated username={}, exceptionMessage={}", userName, e);
         return new ResponseEntity<>("Username=" + userName + " is already used. Please create another one", HttpStatus.BAD_REQUEST);

      } catch (Exception e) {
         log.info("Exception when creating new client, exceptionMessage={}", e);
         return new ResponseEntity<>("Something went wrong when creating a new client", INTERNAL_SERVER_ERROR);
      }
   }

   @ApiOperation(value = "Get funds",
      notes = "Returns the funds for a single client based on client Id")
   @ApiResponses(value = {
      @ApiResponse(code = HTTP_OK,
         message = "Successfully retrieved funds"),
      @ApiResponse(code = HTTP_BAD_REQUEST,
         message = "Couldn't find funds for client"),
      @ApiResponse(code = HTTP_BAD_GATEWAY,
         message = "Couldn't retrieve funds for client")
   })
   @GetMapping("/funds/{clientId}")
   public ResponseEntity<?> getClientFunds(@PathVariable("clientId") String clientId) {
      try {
         double funds = clientService.getFunds(clientId);
         return new ResponseEntity<>(funds, OK);

      } catch (NoAvailableDataException e) {
         log.info("No available client data in clientIdToClientModelMap for clientId={}", clientId);
         return new ResponseEntity<>("No available client data for clientId=" + clientId, NOT_FOUND);

      } catch (Exception e) {
         log.info("Exception when retrieving funds, exceptionMessage={}", e);
         return new ResponseEntity<>("Something went wrong when retrieving funds", INTERNAL_SERVER_ERROR);
      }
   }
}
