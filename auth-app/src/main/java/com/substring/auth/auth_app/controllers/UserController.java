package com.substring.auth.auth_app.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.substring.auth.auth_app.dtos.UserDto;
import com.substring.auth.auth_app.services.UserService;

import lombok.AllArgsConstructor;

import org.springframework.boot.autoconfigure.graphql.GraphQlProperties.Http;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
   
   private final UserService userService;

   //Create User api
  @PostMapping
   public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto){
       return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
   }

   // Get All user Api
   @GetMapping
   public ResponseEntity<Iterable<UserDto>> getAllUsers(){
      return ResponseEntity.ok(userService.getAllUsers());
   }

   //get user by email
   @GetMapping("/email/{emailId}")
   public ResponseEntity<UserDto> getUserByEmail(@PathVariable("emailId") String email){
     return ResponseEntity.ok(userService.getUserByEmail(email));
   }
}
