package com.substring.auth.auth_app.services.Impl;

import com.substring.auth.auth_app.dtos.UserDto;
import com.substring.auth.auth_app.entities.Provider;
import com.substring.auth.auth_app.entities.User;
import com.substring.auth.auth_app.exceptions.ResourceNotFoundException;
import com.substring.auth.auth_app.helpers.UserHelper;
import com.substring.auth.auth_app.repositories.UserRepository;
import com.substring.auth.auth_app.services.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.UUID;

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
    public UserDto updateUser(UserDto userDto, String userId) {
       UUID uId = UserHelper.parseUUID(userId);
       User existingUser = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));

        // We are not going to chanege email id for this project
        if(userDto.getName() != null ){
            existingUser.setName(userDto.getName());
        }
        if(userDto.getImage() != null){
            existingUser.setImage(userDto.getImage());
        }
        if(userDto.getProvider() != null){
            existingUser.setProvider(userDto.getProvider());
        }
        //TODO change password updation logic ...
        if(userDto.getPassword() != null){
            existingUser.setPassword(userDto.getPassword());
        }
        existingUser.setEnable(userDto.isEnable());
        existingUser.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(existingUser);

        return modelMapper.map(updatedUser , UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
       UUID uId = UserHelper.parseUUID(userId);
       User user = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User not found with the given id"));
       userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(UserHelper.parseUUID(userId)).orElseThrow(() -> new ResourceNotFoundException("User not found with the given id"));

        return modelMapper.map(user , UserDto.class);
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
