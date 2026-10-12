package com.digitalagriculture.api.web;

import com.digitalagriculture.api.consultation.ConsultationRepository;
import com.digitalagriculture.api.crop.CropRepository;
import com.digitalagriculture.api.record.FarmRecordRepository;
import com.digitalagriculture.api.record.RecordKind;
import com.digitalagriculture.api.user.AccountRole;
import com.digitalagriculture.api.user.UserRepository;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminSummaryController {
    private final UserRepository users;
    private final CropRepository crops;
    private final FarmRecordRepository records;
    private final ConsultationRepository consultations;

    public AdminSummaryController(UserRepository users, CropRepository crops, FarmRecordRepository records,
                                  ConsultationRepository consultations) {
        this.users = users;
        this.crops = crops;
        this.records = records;
        this.consultations = consultations;
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return Map.of(
                "farmers", users.countByRole(AccountRole.FARMER),
                "buyers", users.countByRole(AccountRole.BUYER),
                "experts", users.countByRole(AccountRole.EXPERT),
                "admins", users.countByRole(AccountRole.ADMIN),
                "crops", crops.count(),
                "activeCrops", crops.countByStatus("ACTIVE"),
                "activities", records.countByKind(RecordKind.ACTIVITY),
                "listings", records.countByKind(RecordKind.LISTING),
                "feedback", records.countByKind(RecordKind.FEEDBACK),
                "consultations", consultations.count());
    }
}
