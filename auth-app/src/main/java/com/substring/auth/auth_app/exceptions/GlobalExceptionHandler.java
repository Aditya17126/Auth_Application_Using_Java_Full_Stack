package com.substring.auth.auth_app.exceptions;

import com.substring.auth.auth_app.dtos.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
   
  // resource not found exception handler

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception){
   ErrorResponse internalServerError =  new ErrorResponse(exception.getMessage(), HttpStatus.NOT_FOUND , 404);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(ResourceNotFoundException exception){
    ErrorResponse internalServerError = new ErrorResponse(exception.getMessage() , HttpStatus.BAD_REQUEST , 400);
     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(internalServerError);
  }
}
 