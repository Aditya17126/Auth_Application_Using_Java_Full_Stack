package com.substring.auth.auth_app.entities;

import java.time.Instant;
import java.util.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

//import jakarta.persistence.Entity;
//import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.*;

// import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder


@Entity
@Table(name="users")
public class User implements UserDetails{
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   @Column(name = "user_id")
   private UUID id;
   @Column(name = "user_email" , unique = true , length = 300)
   private String email;
   private String name;
   private String password;
   private String image; 
   private boolean enable = true;
   private Instant createdAt = Instant.now();
   private Instant updatedAt = Instant.now();

   @Enumerated(EnumType.STRING)
   private Provider provider = Provider.LOCAL;

   @ManyToMany(fetch = FetchType.EAGER)
   @JoinTable(
           name = "user_roles" ,
           joinColumns = @JoinColumn(name = "user_id") ,
           inverseJoinColumns = @JoinColumn(name = "role_id")
   )

   private Set<Role> roles = new HashSet<>();

   @PrePersist
   protected void onCreate(){
      Instant now = Instant.now();
      if(createdAt == null) createdAt = now;
      updatedAt = now;
   }

   @PreUpdate
   protected void onUpdate(){
      updatedAt = Instant.now();
   }

   @Override
   public Collection<? extends GrantedAuthority> getAuthorities() {
      return List.of();
   }

   @Override
   public String getUsername() {
      return "";
   }

   @Override
   public boolean isAccountNonExpired() {
      return UserDetails.super.isAccountNonExpired();
   }

   @Override
   public boolean isAccountNonLocked() {
      return UserDetails.super.isAccountNonLocked();
   }

   @Override
   public boolean isCredentialsNonExpired() {
      return UserDetails.super.isCredentialsNonExpired();
   }

   @Override
   public boolean isEnabled() {
      return UserDetails.super.isEnabled();
   }
}
