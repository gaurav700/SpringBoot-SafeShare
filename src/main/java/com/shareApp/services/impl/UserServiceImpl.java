package com.shareApp.services.impl;

import com.shareApp.entities.User;
import com.shareApp.exceptions.ResourceNotFoundException;
import com.shareApp.repositories.UserRepository;
import com.shareApp.services.UserService;
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
        return userRepository.findByEmail(username).orElse(null);
    }

    @Override
    public User getUserById(Long id){
        return userRepository.findById(String.valueOf(id))
                .orElseThrow(()-> new ResourceNotFoundException("User with this id not found"));
    }


}
