package com.supplyhub.notifications;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    public record NotificationRequest(@NotBlank String type, @NotBlank String message, String referenceId) {
    }

    public record NotificationResponse(Long id, String type, String message, String referenceId, String createdAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getReferenceId(), n.getCreatedAt().toString());
        }
    }

    @GetMapping
    public List<NotificationResponse> all() {
        return repo.findAll().stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).map(NotificationResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(@Valid @RequestBody NotificationRequest r) {
        return NotificationResponse.from(repo.save(new Notification(r.type(), r.message(), r.referenceId())));
    }
}
