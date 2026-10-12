package com.digitalagriculture.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersFarmerAndExpertAccountsPublicly() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Asha","lastName":"Devi","email":"asha@example.com","phone":"+919876543210","password":"A-strong-password-123","accountType":"farmer"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("farmer"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Asha","lastName":"Expert","email":"expert@example.com","phone":"+919876543211","password":"A-strong-password-123","accountType":"expert"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("expert"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void adminCanProvisionFarmerWithProfileAndInactiveStatus() throws Exception {
        mvc.perform(post("/api/admin/farmers")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Mina Devi","phone":"9876543210","email":"mina@example.com","username":"mina.farm",
                                "password":"A-strong-password-123","gender":"Female","state":"Rajasthan","district":"Jaipur",
                                "village":"Sanganer","pinCode":"302029","mainCrop":"Wheat","farmingType":"Organic",
                                "landArea":2.5,"landUnit":"Acres","status":"PENDING"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("farmer"))
                .andExpect(jsonPath("$.username").value("mina.farm"))
                .andExpect(jsonPath("$.district").value("Jaipur"))
                .andExpect(jsonPath("$.landArea").value(2.5))
                .andExpect(jsonPath("$.accountStatus").value("PENDING"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(post("/api/admin/farmers")
                        .with(user("farmer").roles("FARMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsDuplicateEmailsCaseInsensitively() throws Exception {
        String body = """
                {"firstName":"Asha","lastName":"Devi","email":"unique@example.com","phone":"+919876543210","password":"A-strong-password-123","accountType":"buyer"}
                """;
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("unique@example.com", "UNIQUE@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void forgotPasswordFlowGeneratesResetTokenAndAllowsLoginWithNewPassword() throws Exception {
        String email = "reset-" + java.util.UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Riya","lastName":"Patel","email":"%s","phone":"+919876543220","password":"A-strong-password-123","accountType":"farmer"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult forgot = mvc.perform(post("/api/auth/password/forgot")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists for that email, a password reset link has been created."))
                .andExpect(jsonPath("$.resetToken").isNotEmpty())
                .andReturn();

        String resetToken = objectMapper.readTree(forgot.getResponse().getContentAsString()).get("resetToken").asText();

        mvc.perform(post("/api/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"Brand-new-password-123"}
                                """.formatted(resetToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password has been updated successfully."));

        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Brand-new-password-123","accountType":"user"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void contactSubmissionValidatesAndPersists() throws Exception {
        mvc.perform(post("/api/contact")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Asha Devi","email":"asha@example.com","topic":"Feedback","message":"Please add crop price alerts."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Your message has been received."));
    }

    @Test
    void loginCreatesASessionAndAllowsTheAuthenticatedAccountToReadItsProfile() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Kiran","lastName":"Singh","email":"kiran@example.com","phone":"+919876543212","password":"A-strong-password-123","accountType":"farmer"}
                                """))
                .andExpect(status().isCreated());

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"kiran@example.com","password":"A-strong-password-123","accountType":"user"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("farmer"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("kiran@example.com"));
        mvc.perform(post("/api/weather/refresh").session(session).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Save device location")));
        mvc.perform(get("/api/weather/current").session(session))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/auth/me/location")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latitude":26.9124,"longitude":75.7873,"location":"Jaipur","state":"Rajasthan","district":"Jaipur","village":"Jaipur"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(26.9124))
                .andExpect(jsonPath("$.longitude").value(75.7873))
                .andExpect(jsonPath("$.location").value("Jaipur"));
        mvc.perform(patch("/api/auth/me")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Kiran","lastName":"Singh","state":"Rajasthan","district":"Jaipur",
                                "notifyWeather":false,"notifyMarket":true,"notifyActivities":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Rajasthan"))
                .andExpect(jsonPath("$.district").value("Jaipur"))
                .andExpect(jsonPath("$.notifyWeather").value(false))
                .andExpect(jsonPath("$.notifyMarket").value(true));
    }

    @Test
    void marketPricesComeFromDatabaseAndRefreshFailsClearlyWithoutProviderKey() throws Exception {
        mvc.perform(get("/api/market/prices").with(user("buyer").roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(post("/api/market/prices/refresh")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("DATA_GOV_API_KEY")));
    }

    @Test
    void publicSummaryReturnsDatabaseBackedTotalsWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/public/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmers").isNumber())
                .andExpect(jsonPath("$.activeCrops").isNumber())
                .andExpect(jsonPath("$.activeListings").isNumber());
    }

    @Test
    void adminPlatformSettingsReadAndWriteThroughTheDatabase() throws Exception {
        mvc.perform(get("/api/admin/settings").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform_name").isNotEmpty())
                .andExpect(jsonPath("$.time_zone").isNotEmpty());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/settings")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"platformName":"Agri Platform","defaultLanguage":"Hindi","timeZone":"Asia/Kolkata",
                                "notifyNewFarmer":false,"notifyFeedback":true,"notifyMarketPrices":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform_name").value("Agri Platform"))
                .andExpect(jsonPath("$.notify_market_prices").value("true"));
    }

    @Test
    void browserCsrfBootstrapReturnsUsableSessionToken() throws Exception {
        MvcResult csrf = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn();
        String token = objectMapper.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession(false);
        mvc.perform(post("/api/contact")
                        .session(session)
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Asha Devi","email":"asha@example.com","topic":"Feedback","message":"Please add crop price alerts."}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void farmerCanCreateAndReadOnlyTheirOwnCropRecords() throws Exception {
        String email = "farm-" + java.util.UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Mina","lastName":"Farmer","email":"%s","phone":"+919876543210","password":"A-strong-password-123","accountType":"farmer"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"A-strong-password-123","accountType":"user"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(post("/api/records")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"CROP","title":"Wheat","category":"Sharbati","quantity":4.5,"unit":"Acres","status":"ACTIVE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Wheat"));
        mvc.perform(get("/api/records?kind=CROP").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Wheat"));
    }

    @Test
    void developmentCorsAcceptsNonDefaultLiveServerPorts() throws Exception {
        mvc.perform(options("/api/auth/register")
                        .header("Origin", "http://127.0.0.1:5508")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5508"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void farmerCanUpdateProfileAndCompleteAnActivity() throws Exception {
        String email = "profile-" + java.util.UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Asha","lastName":"Devi","email":"%s","phone":"+919876543210","password":"A-strong-password-123","accountType":"farmer"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"A-strong-password-123","accountType":"user"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(patch("/api/auth/me")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Asha","lastName":"Devi","phone":"+919876543210","location":"Jaipur","farmName":"Green Acres","farmArea":"12 Acres","soilType":"Loamy Soil","irrigationMethod":"Drip Irrigation"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmName").value("Green Acres"));
        MvcResult activity = mvc.perform(post("/api/records")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"ACTIVITY","title":"Irrigate wheat field","category":"Irrigation","status":"PLANNED"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(activity.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(patch("/api/records/" + id + "/status")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void administratorSummaryIsRoleProtectedAndReturnsDatabaseCounts() throws Exception {
        mvc.perform(get("/api/admin/summary"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/summary").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmers").isNumber())
                .andExpect(jsonPath("$.crops").isNumber())
                .andExpect(jsonPath("$.consultations").isNumber());
    }
}
