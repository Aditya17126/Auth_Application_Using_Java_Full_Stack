package com.substring.auth.auth_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class SecurityConfig {
  
  @Bean
  public UserDetailsService users(){
    User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();

    UserDetails user1 = userBuilder.username("Aditya").password("abc").roles("ADMIN").build();
    UserDetails user2 = userBuilder.username("Shiva").password("xyz ").roles("ADMIN").build();
    UserDetails user3 = userBuilder.username("Durgesh").password("").roles("USER").build();

    return new InMemoryUserDetailsManager(user1, user2, user3);
  }
}
