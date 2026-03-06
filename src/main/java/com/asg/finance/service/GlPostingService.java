package com.asg.finance.service;

public interface GlPostingService {
    String performGlPosting(String docId, Long transactionPoid, String docRef);
}
