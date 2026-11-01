package com.healthbox.hms_backend.config;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.modules.auth.UserService;
import com.healthbox.hms_backend.modules.tenant.Hospital;
import com.healthbox.hms_backend.modules.tenant.HospitalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// DEV ONLY: provisions a ready-to-use demo hospital + doctor (ADMIN) login on first boot,
// so you can start testing immediately without going through self-registration/OTP.
// Safe to leave in for local dev — it's idempotent and skips itself once the demo hospital exists.
@Component
@Profile("dev")
public class DevTestUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevTestUserSeeder.class);
    private static final String DEMO_HOSPITAL_CODE = "DEMO01";

    private final HospitalRepository hospitalRepository;
    private final UserService userService;

    public DevTestUserSeeder(HospitalRepository hospitalRepository, UserService userService) {
        this.hospitalRepository = hospitalRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (hospitalRepository.existsByCode(DEMO_HOSPITAL_CODE)) {
            return; // already seeded
        }

        Hospital hospital = new Hospital();
        hospital.setName("Demo Clinic");
        hospital.setCode(DEMO_HOSPITAL_CODE);
        hospital = hospitalRepository.save(hospital);

        userService.saveUser("9999999999", "doctor", "doctor123", "doctor@demo.local", Role.ADMIN, hospital.getId());

        log.info("==============================================");
        log.info(" Test login ready — username: doctor / password: doctor123");
        log.info("==============================================");
    }
}
