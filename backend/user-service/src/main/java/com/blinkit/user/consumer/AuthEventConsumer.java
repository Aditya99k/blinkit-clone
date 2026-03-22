package com.blinkit.user.consumer;

import com.blinkit.user.entity.UserProfile;
import com.blinkit.user.event.UserDeletedEvent;
import com.blinkit.user.event.UserRegisteredEvent;
import com.blinkit.user.repository.UserProfileRepository;
import com.blinkit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventConsumer {

    private final UserProfileRepository profileRepo;
    private final UserService userService;

    @KafkaListener(topics = "user.registered", groupId = "user-service",
                   containerFactory = "userRegisteredListenerFactory")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered for userId={}", event.getUserId());

        if (profileRepo.findByUserId(event.getUserId()).isPresent()) {
            log.info("Profile already exists for userId={}, skipping", event.getUserId());
            return;
        }

        UserProfile profile = UserProfile.builder()
                .userId(event.getUserId())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .build();
        profileRepo.save(profile);
        log.info("Created profile for userId={}", event.getUserId());
    }

    @KafkaListener(topics = "user.deleted", groupId = "user-service",
                   containerFactory = "userDeletedListenerFactory")
    public void onUserDeleted(UserDeletedEvent event) {
        log.info("Received user.deleted for userId={}", event.getUserId());
        userService.deleteUserData(event.getUserId());
    }
}
