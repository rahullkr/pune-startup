package com.punestartup.resume.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ResumeUploadedEventListener {

    private final ResumeParsingService parsingService;

    public ResumeUploadedEventListener(ResumeParsingService parsingService) {
        this.parsingService = parsingService;
    }

    @Async("resumeTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResumeUploaded(ResumeUploadedEvent event) {
        parsingService.parse(event.resumeId());
    }
}
