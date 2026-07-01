package com.substring.auth.auth_app.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.substring.auth.auth_app.dtos.UserDto;
import com.substring.auth.auth_app.services.UserService;

import lombok.AllArgsConstructor;

import org.springframework.boot.autoconfigure.graphql.GraphQlProperties.Http;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
   
   private final UserService userService;

   //Create User api
  @PostMapping
   public ResponseEntity<UserDto> createUser(@RequestBody  UserDto userDto){
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

   //Delete User
   // /api/v1/users/{userId}
   @DeleteMapping("/{userId}")
   public void deleteUser(@PathVariable("userId") String userId){
      userService.deleteUser(userId);
   }
   
   //Update User
   // /api/v1/users/{userId}
   @PutMapping("/{userId}")
   public ResponseEntity<UserDto> updateUser(@RequestBody UserDto userDto , @PathVariable("userId")String userId ){
       return ResponseEntity.ok(userService.updateUser(userDto , userId));
   }

   // get user by id
   // api/v1/user/{userId}
   @GetMapping("/{userId}")
   public ResponseEntity<UserDto> getUserById(@PathVariable("userId") String userId){
      return ResponseEntity.ok(userService.getUserById(userId));
   }
   

}
