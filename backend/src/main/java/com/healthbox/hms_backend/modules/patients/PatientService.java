package com.healthbox.hms_backend.modules.patients;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

// Read-only: a Patient profile is only ever written by ConsultationMaterializer,
// once a consultation payment is verified. No direct create/delete here by design.
@Service
public class PatientService {

    private final PatientRepository repo;
    private final CurrentUser currentUser;

    public PatientService(PatientRepository repo, CurrentUser currentUser) {
        this.repo = repo;
        this.currentUser = currentUser;
    }

    public List<Patient> getAll() {
        AppUserPrincipal me = currentUser.get();
        return switch (me.getRole()) {
            case ADMIN -> repo.findByHospitalId(me.getHospitalId());
            case PATIENT -> repo.findByPhno(me.getPhno()).map(List::of).orElse(List.of());
            case SUPER_ADMIN -> throw new AccessDeniedException("Use /api/superadmin endpoints instead");
        };
    }

    public Patient getByPhno(String phno) {
        Patient p = repo.findByPhno(phno).orElseThrow(() -> new RuntimeException("Patient not found"));
        assertAccessible(p);
        return p;
    }

    private void assertAccessible(Patient p) {
        AppUserPrincipal me = currentUser.get();
        if (me.getRole() == Role.SUPER_ADMIN) throw new AccessDeniedException("Use /api/superadmin endpoints instead");
        if (!p.getHospitalId().equals(me.getHospitalId())) throw new AccessDeniedException("Cross-tenant access denied");
        if (me.getRole() == Role.PATIENT && !me.getPhno().equals(p.getPhno())) throw new AccessDeniedException("Not your own record");
    }
}
