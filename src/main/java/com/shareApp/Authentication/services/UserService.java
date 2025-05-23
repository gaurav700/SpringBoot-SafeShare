package com.shareApp.Authentication.services;

import com.shareApp.Authentication.entities.User;
import com.shareApp.Payment.entitites.Payment;

import java.math.BigDecimal;
import java.util.List;

public interface UserService {
    User getUserById(String userId);

    List<Payment> getUserPaymentHistory(String userId);

    public Long getTotalUserStorage(String userId);

    public BigDecimal getTotalUserSpending(String userId);

    public List<Payment> getUserCompletedPayments(String userId);
}


