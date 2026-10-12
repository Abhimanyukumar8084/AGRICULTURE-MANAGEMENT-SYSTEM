package com.digitalagriculture.api.web;

import com.digitalagriculture.api.record.FarmRecord;
import com.digitalagriculture.api.record.FarmRecordRepository;
import com.digitalagriculture.api.record.RecordKind;
import com.digitalagriculture.api.user.AccountRole;
import com.digitalagriculture.api.user.User;
import com.digitalagriculture.api.web.dto.FarmRecordRequest;
import com.digitalagriculture.api.web.dto.RecordStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/records")
public class FarmRecordController {
    private final FarmRecordRepository records;

    public FarmRecordController(FarmRecordRepository records) {
        this.records = records;
    }

    @GetMapping
    public List<FarmRecord> list(@RequestParam(required = false) RecordKind kind, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (kind == RecordKind.LISTING && user.getRole() == AccountRole.BUYER) {
            return records.findByKindAndStatusOrderByCreatedAtDesc(RecordKind.LISTING, "ACTIVE");
        }
        if (user.getRole() == AccountRole.ADMIN) {
            return kind == null
                    ? records.findAll(org.springframework.data.domain.Sort.by("createdAt").descending())
                    : records.findByKindOrderByCreatedAtDesc(kind);
        }
        return kind == null
                ? records.findByOwnerIdOrderByCreatedAtDesc(user.getId())
                : records.findByOwnerIdAndKindOrderByCreatedAtDesc(user.getId(), kind);
    }

    @GetMapping("/marketplace")
    public List<FarmRecord> marketplace() {
        return records.findByKindAndStatusOrderByCreatedAtDesc(RecordKind.LISTING, "ACTIVE");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FarmRecord create(@Valid @RequestBody FarmRecordRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        validateKindAllowed(user, request.kind());
        validateStatus(request.status());
        return records.save(toRecord(user.getId(), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        FarmRecord record = records.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found."));
        if (user.getRole() != AccountRole.ADMIN && !record.getOwnerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only remove your own records.");
        }
        records.delete(record);
    }

    @PostMapping("/{id}/close")
    public FarmRecord closeListing(@PathVariable UUID id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        FarmRecord record = records.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found."));
        if (record.getKind() != RecordKind.LISTING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This record is not a market listing.");
        }
        if (user.getRole() != AccountRole.ADMIN && !record.getOwnerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own listings.");
        }
        record.setStatus("CLOSED");
        return records.save(record);
    }

    @PatchMapping("/{id}/status")
    public FarmRecord updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody RecordStatusRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        FarmRecord record = records.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found."));
        if (user.getRole() != AccountRole.ADMIN && !record.getOwnerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own records.");
        }
        if (record.getKind() != RecordKind.ACTIVITY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only farm activities support status updates here.");
        }
        String status = request.status().toUpperCase(Locale.ROOT);
        if (!List.of("PLANNED", "COMPLETED", "CANCELLED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown activity status.");
        }
        record.setStatus(status);
        return records.save(record);
    }

    private void validateKindAllowed(User user, RecordKind kind) {
        boolean allowed = switch (kind) {
            case CROP, ACTIVITY -> user.getRole() == AccountRole.FARMER || user.getRole() == AccountRole.ADMIN;
            case FEEDBACK -> true;
            case LISTING -> user.getRole() == AccountRole.FARMER || user.getRole() == AccountRole.BUYER || user.getRole() == AccountRole.ADMIN;
        };
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account cannot create this record type.");
    }

    private void validateStatus(String status) {
        if (status != null && !status.isBlank()
                && !List.of("ACTIVE", "PLANNED", "COMPLETED", "CANCELLED").contains(status.toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown record status.");
        }
    }

    private FarmRecord toRecord(UUID ownerId, FarmRecordRequest request) {
        return new FarmRecord(ownerId, request.kind(), request.title(), request.category(), request.description(),
                request.quantity(), request.unit(), request.price(), request.location(), request.status(), request.dueDate());
    }
}
