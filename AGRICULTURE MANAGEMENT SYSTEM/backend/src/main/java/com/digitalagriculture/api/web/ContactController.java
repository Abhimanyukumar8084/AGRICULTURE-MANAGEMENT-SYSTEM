package com.digitalagriculture.api.web;

import com.digitalagriculture.api.contact.ContactMessage;
import com.digitalagriculture.api.contact.ContactMessageRepository;
import com.digitalagriculture.api.web.dto.ContactRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final ContactMessageRepository messages;

    public ContactController(ContactMessageRepository messages) {
        this.messages = messages;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submit(@Valid @RequestBody ContactRequest request) {
        ContactMessage saved = messages.save(new ContactMessage(
                request.name().trim(),
                request.email().trim().toLowerCase(java.util.Locale.ROOT),
                request.topic().trim(),
                request.message().trim()));
        return Map.of("id", saved.getId(), "message", "Your message has been received.", "createdAt", saved.getCreatedAt());
    }
}
