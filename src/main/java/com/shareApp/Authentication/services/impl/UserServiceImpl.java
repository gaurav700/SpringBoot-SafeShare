package com.shareApp.Authentication.services.impl;

import com.shareApp.Authentication.entities.User;
import com.shareApp.Payment.entitites.Payment;
import com.shareApp.Payment.repositories.PaymentRepository;
import com.shareApp.Utils.exceptions.ResourceNotFoundException;
import com.shareApp.Authentication.repositories.UserRepository;
import com.shareApp.Authentication.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }


    @Override
    public User getUserById(String id){
        return userRepository.findById(String.valueOf(id))
                .orElseThrow(()-> new ResourceNotFoundException("User with this id not found"));
    }

    @Override
    public List<Payment> getUserPaymentHistory(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Payment> getUserCompletedPayments(String userId) {
        return paymentRepository.findByUserIdAndStatus(userId, Payment.PaymentStatus.COMPLETED);
    }

    @Override
    public BigDecimal getTotalUserSpending(String userId) {
        List<Payment> completedPayments = getUserCompletedPayments(userId);
        return completedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Long getTotalUserStorage(String userId) {
        List<Payment> completedPayments = getUserCompletedPayments(userId);
        return completedPayments.stream()
                .mapToLong(Payment::getStorageGb)
                .sum();
    }

}


