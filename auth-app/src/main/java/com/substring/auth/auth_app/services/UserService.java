package com.substring.auth.auth_app.services;

import com.substring.auth.auth_app.dtos.UserDto;

public interface UserService {

    //Create user
    UserDto createUser(UserDto userDto);

    //get user by email
    UserDto getUserByEmail(String email);

    //update user
    UserDto updateUser(UserDto userDto , String userId);

    //Delete user
    void deleteUser(String userId);

    //get user by id
    UserDto getUserById(String userId);

    //get all users
    Iterable<UserDto> getAllUsers();

    

}
