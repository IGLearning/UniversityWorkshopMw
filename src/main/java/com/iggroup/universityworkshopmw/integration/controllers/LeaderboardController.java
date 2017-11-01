package com.iggroup.universityworkshopmw.integration.controllers;

import com.iggroup.universityworkshopmw.domain.model.Client;
import com.iggroup.universityworkshopmw.domain.services.ClientService;
import com.iggroup.universityworkshopmw.integration.dto.ClientDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.iggroup.universityworkshopmw.integration.transformers.ClientDtoTransformer.transformAllClients;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

   private  final ClientService clientService;

   @GetMapping("/all")
   public ResponseEntity<?> getAllClients() {
      try {
         Map<String, Client> allClients = clientService.getAllClients();
         Map<String, ClientDto> allClientsTransformed = transformAllClients(allClients);
         return new ResponseEntity<>(allClientsTransformed, OK);
      } catch (Exception e) {
         log.info("Exception when getting all client data", e);
         return new ResponseEntity<>("Something went wrong when getting all client data", INTERNAL_SERVER_ERROR);
      }
   }
}
