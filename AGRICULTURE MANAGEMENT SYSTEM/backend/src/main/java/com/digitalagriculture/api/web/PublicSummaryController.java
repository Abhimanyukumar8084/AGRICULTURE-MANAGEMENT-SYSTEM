package com.digitalagriculture.api.web;

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
@RequestMapping("/api/public")
public class PublicSummaryController {
    private final UserRepository users;
    private final CropRepository crops;
    private final FarmRecordRepository records;

    public PublicSummaryController(UserRepository users, CropRepository crops, FarmRecordRepository records) {
        this.users = users;
        this.crops = crops;
        this.records = records;
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return Map.of(
                "farmers", users.countByRole(AccountRole.FARMER),
                "buyers", users.countByRole(AccountRole.BUYER),
                "experts", users.countByRole(AccountRole.EXPERT),
                "activeCrops", crops.countByStatus("ACTIVE"),
                "activeListings", records.countByKindAndStatus(RecordKind.LISTING, "ACTIVE"));
    }
}
