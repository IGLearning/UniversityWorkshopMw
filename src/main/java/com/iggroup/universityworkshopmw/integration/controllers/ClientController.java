package com.iggroup.universityworkshopmw.integration.controllers;

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

      } catch (Exception e) {
         log.info("Exception when creating new client, exceptionMessage={}", e);
         return new ResponseEntity<>("Something went wrong when creating a new client", INTERNAL_SERVER_ERROR);
      }
   }

   @ApiOperation(value = "Get a client",
         notes = "Gets data for a single client",
         response = ClientDto.class)
   @ApiResponses(value = {
         @ApiResponse(code = HTTP_OK,
               message = "Successfully retrieved client data"),
         @ApiResponse(code = HTTP_BAD_REQUEST,
               message = "Couldn't recognise request"),
         @ApiResponse(code = HTTP_BAD_GATEWAY,
               message = "Couldn't get client data")
   })
   @GetMapping("/{clientId}")
   public ResponseEntity<?> getClient(@PathVariable("clientId") String clientId) {
      try {
         ClientDto responseBody = ClientDtoTransformer.transform(clientService.getClientData(clientId));
         return new ResponseEntity<>(responseBody, OK);

      } catch (NoAvailableDataException e) {
         log.info("No available client data in clientIdToClientModelMap for clientId={}", clientId);
         return new ResponseEntity<>("No available client data for clientId=" + clientId, NOT_FOUND);

      } catch (Exception e) {
         log.info("Exception when retrieving client data, exceptionMessage={}", e);
         return new ResponseEntity<>("Something went wrong when retrieving client data", INTERNAL_SERVER_ERROR);
      }
   }

}
