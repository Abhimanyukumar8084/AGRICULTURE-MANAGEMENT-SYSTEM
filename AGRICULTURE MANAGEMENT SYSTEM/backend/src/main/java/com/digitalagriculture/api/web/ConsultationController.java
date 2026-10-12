package com.digitalagriculture.api.web;

import com.digitalagriculture.api.consultation.Consultation;
import com.digitalagriculture.api.consultation.ConsultationRepository;
import com.digitalagriculture.api.consultation.ConsultationStatus;
import com.digitalagriculture.api.user.AccountRole;
import com.digitalagriculture.api.user.User;
import com.digitalagriculture.api.user.UserRepository;
import com.digitalagriculture.api.web.dto.ConsultationRequest;
import com.digitalagriculture.api.web.dto.ConsultationStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {
    private final ConsultationRepository consultations;
    private final UserRepository users;

    public ConsultationController(ConsultationRepository consultations, UserRepository users) {
        this.consultations = consultations;
        this.users = users;
    }

    @GetMapping
    public List<Consultation> list(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return switch (user.getRole()) {
            case FARMER -> consultations.findByFarmerIdOrderByCreatedAtDesc(user.getId());
            case EXPERT -> {
                List<Consultation> visible = new ArrayList<>(consultations.findByExpertIdOrderByCreatedAtDesc(user.getId()));
                visible.addAll(consultations.findByExpertIdIsNullAndStatusOrderByCreatedAtDesc(ConsultationStatus.PENDING));
                yield visible;
            }
            case ADMIN -> consultations.findAll(org.springframework.data.domain.Sort.by("createdAt").descending());
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account cannot access consultations.");
        };
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Consultation create(@Valid @RequestBody ConsultationRequest request, Authentication authentication) {
        User farmer = (User) authentication.getPrincipal();
        if (farmer.getRole() != AccountRole.FARMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only farmer accounts can request consultations.");
        }
        if (request.expertId() != null) {
            User expert = users.findById(request.expertId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expert not found."));
            if (expert.getRole() != AccountRole.EXPERT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected account is not an expert.");
            }
        }
        return consultations.save(new Consultation(farmer.getId(), request.expertId(),
                request.topic(), request.crop(), request.question()));
    }

    @PatchMapping("/{id}/status")
    public Consultation updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ConsultationStatusRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() != AccountRole.EXPERT && user.getRole() != AccountRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an expert or administrator can update consultation status.");
        }
        Consultation consultation = consultations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found."));
        if (user.getRole() == AccountRole.EXPERT && consultation.getExpertId() != null
                && !user.getId().equals(consultation.getExpertId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This consultation is assigned to a different expert.");
        }
        if (user.getRole() == AccountRole.EXPERT && request.status() != ConsultationStatus.ACCEPTED
                && !user.getId().equals(consultation.getExpertId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accept the open request before updating it.");
        }
        if (!isAllowedTransition(consultation.getStatus(), request.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This consultation status transition is not allowed.");
        }
        if (user.getRole() == AccountRole.EXPERT && request.status() == ConsultationStatus.ACCEPTED) {
            consultation.setExpertId(user.getId());
        }
        consultation.setStatus(request.status());
        return consultations.save(consultation);
    }

    private boolean isAllowedTransition(ConsultationStatus current, ConsultationStatus next) {
        return switch (current) {
            case PENDING -> next == ConsultationStatus.ACCEPTED || next == ConsultationStatus.CANCELLED;
            case ACCEPTED -> next == ConsultationStatus.COMPLETED || next == ConsultationStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
