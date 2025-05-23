package com.shareApp.Authentication.services.impl;

import com.shareApp.Authentication.entities.User;
import com.shareApp.Utils.exceptions.ResourceNotFoundException;
import com.shareApp.Authentication.repositories.UserRepository;
import com.shareApp.Authentication.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }


    @Override
    public User getUserById(Long id){
        return userRepository.findById(String.valueOf(id))
                .orElseThrow(()-> new ResourceNotFoundException("User with this id not found"));
    }


}
