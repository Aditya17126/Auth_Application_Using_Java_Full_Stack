package com.substring.auth.auth_app.services.Impl;

import org.springframework.stereotype.Service;
import com.substring.auth.auth_app.dtos.UserDto;
import com.substring.auth.auth_app.services.AuthService;
import com.substring.auth.auth_app.services.UserService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService{
   
  private final UserService userService;
  @Override
  public UserDto registerUser(UserDto userDto) {
    // Logic 
    //Verify email
    // Verify Passowrd lenght , letters , 
    UserDto userDto1 = userService.createUser(userDto);

    return userDto1;
  }
  
}
