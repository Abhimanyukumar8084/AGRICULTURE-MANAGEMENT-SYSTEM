package com.digitalagriculture.api.web;

import com.digitalagriculture.api.crop.Crop;
import com.digitalagriculture.api.crop.CropRepository;
import com.digitalagriculture.api.web.dto.CropRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/crops")
public class CropController {
    private final CropRepository crops;

    public CropController(CropRepository crops) {
        this.crops = crops;
    }

    @GetMapping
    public List<Crop> list() {
        return crops.findAll(org.springframework.data.domain.Sort.by("name").ascending()).stream()
                .filter(crop -> crop.getStatus().equals("ACTIVE"))
                .toList();
    }

    @GetMapping("/{id}")
    public Crop get(@PathVariable UUID id) {
        return findCrop(id);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Crop> listAllForAdmin() {
        return crops.findAll(org.springframework.data.domain.Sort.by("name").ascending());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Crop create(@Valid @RequestBody CropRequest request) {
        validateStatus(request.status());
        return crops.save(toCrop(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Crop update(@PathVariable UUID id, @Valid @RequestBody CropRequest request) {
        validateStatus(request.status());
        Crop current = findCrop(id);
        current.updateFrom(toCrop(request));
        return crops.save(current);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        crops.delete(findCrop(id));
    }

    private Crop findCrop(UUID id) {
        return crops.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Crop not found."));
    }

    private Crop toCrop(CropRequest request) {
        return new Crop(request.name(), request.type(), request.season(), request.variety(), request.growingDuration(),
                request.waterRequirement(), request.soilType(), request.temperatureRange(), request.description(),
                request.averagePrice(), request.priceUnit(), request.msp(), request.status());
    }

    private void validateStatus(String status) {
        if (status != null && !List.of("ACTIVE", "INACTIVE").contains(status.toUpperCase(java.util.Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown crop status.");
        }
    }
}
