package com.healthbox.hms_backend.modules.billing;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

// Read-only: invoices are written only by ConsultationMaterializer after a paid checkout.
@Service
public class BillingService {
    private final BillingRepository billingRepo;
    private final CurrentUser currentUser;

    public BillingService(BillingRepository billingRepo, CurrentUser currentUser) {
        this.billingRepo = billingRepo;
        this.currentUser = currentUser;
    }

    public List<Billing> getAll() {
        AppUserPrincipal me = currentUser.get();
        return switch (me.getRole()) {
            case ADMIN -> billingRepo.findByHospitalId(me.getHospitalId());
            case PATIENT -> billingRepo.findByPatientPhno(me.getPhno());
            case SUPER_ADMIN -> throw new AccessDeniedException("Use /api/superadmin endpoints instead");
        };
    }

    public List<Billing> getByPatientPhno(String phno) {
        AppUserPrincipal me = currentUser.get();
        if (me.getRole() == Role.PATIENT && !me.getPhno().equals(phno)) {
            throw new AccessDeniedException("Not your own record");
        }
        List<Billing> billings = billingRepo.findByPatientPhno(phno);
        if (me.getRole() == Role.ADMIN) {
            return billings.stream().filter(billing -> me.getHospitalId().equals(billing.getHospitalId())).toList();
        }
        return billings;
    }
}
