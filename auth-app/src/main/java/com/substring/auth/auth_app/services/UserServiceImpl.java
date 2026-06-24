package com.substring.auth.auth_app.services;

import com.substring.auth.auth_app.dtos.UserDto;
import com.substring.auth.auth_app.entities.Provider;
import com.substring.auth.auth_app.entities.User;
import com.substring.auth.auth_app.exceptions.ResourceNotFoundException;
import com.substring.auth.auth_app.repositories.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements  UserService{
    
  private final UserRepository userRepository;
  private final ModelMapper modelMapper;
    @Override
    @Transactional // =>
    public UserDto createUser(UserDto userDto) {

        if(userDto.getEmail() == null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email is alrady exists !!");
        }

        User user = modelMapper.map(userDto , User.class);
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
        // Role assign here to new user for authorization
        //TODO:
        User savedUser = userRepository.save(user);

        return modelMapper.map(savedUser , UserDto.class);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        
       User user = userRepository
        .findByEmail(email)
        .orElseThrow( () -> new ResourceNotFoundException("User not found with the give email Id"));
        return modelMapper.map(user , UserDto.class);
    }

    @Override
    public UserDto updateUSer(UserDto userDto, String userId) {
        return null;
    }

    @Override
    public void deleteUser(String userId) {

    }

    @Override
    public UserDto getUserById(String userId) {
        return null;
    }

    @Override
    @Transactional()
    public Iterable<UserDto> getAllUsers() {
        return userRepository
        .findAll()
        .stream()
        .map(user  -> modelMapper.map(user, UserDto.class))
        .toList();
    }
}
