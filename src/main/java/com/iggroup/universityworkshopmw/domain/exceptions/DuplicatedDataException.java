package com.iggroup.universityworkshopmw.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Duplicated data found.")  // 400
public class DuplicatedDataException extends Exception {

   public DuplicatedDataException(String message) {
      super(message);
   }

}
