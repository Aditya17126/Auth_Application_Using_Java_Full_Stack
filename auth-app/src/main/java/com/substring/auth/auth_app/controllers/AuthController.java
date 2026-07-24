package com.substring.auth.auth_app.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.substring.auth.auth_app.dtos.UserDto;
import com.substring.auth.auth_app.services.AuthService;

import lombok.AllArgsConstructor;

@RestController 
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
   
  private final  AuthService authService;
  
  //Register User
  @PostMapping("/register")
  public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto){
    System.out.println("AuthController file : " + userDto.toString());
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));
  }
  
 }
