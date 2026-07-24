package com.substring.auth.auth_app.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.substring.auth.auth_app.entities.User;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
    
    http.csrf(AbstractHttpConfigurer::disable);
    http.authorizeHttpRequests(authorizeHttpRequests -> 
      authorizeHttpRequests
      .requestMatchers(HttpMethod.POST, "/api/v1/auth/register")
      .permitAll()
      .requestMatchers("/api/v1/auth/login")
      .permitAll()
      .anyRequest()
      .authenticated())
      .httpBasic(Customizer .withDefaults());
     
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();
  }

  // @Bean
  // public UserDetailsService users(){
  //   User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();

  //   UserDetails user1 = userBuilder.username("Aditya").password("abc").roles("ADMIN").build();
  //   UserDetails user2 = userBuilder.username("Shiva").password("xyz ").roles("ADMIN").build();
  //   UserDetails user3 = userBuilder.username("Durgesh").password("").roles("USER").build();

  //   return new InMemoryUserDetailsManager(user1, user2, user3);
  // }
  
}
