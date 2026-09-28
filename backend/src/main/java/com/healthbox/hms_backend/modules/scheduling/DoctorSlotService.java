package com.healthbox.hms_backend.modules.scheduling;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import com.healthbox.hms_backend.modules.tenant.Hospital;
import com.healthbox.hms_backend.modules.tenant.HospitalRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorSlotService {

    private final DoctorSlotRepository repo;
    private final CurrentUser currentUser;
    private final HospitalRepository hospitalRepository;

    public DoctorSlotService(DoctorSlotRepository repo, CurrentUser currentUser, HospitalRepository hospitalRepository) {
        this.repo = repo;
        this.currentUser = currentUser;
        this.hospitalRepository = hospitalRepository;
    }

    public DoctorSlot create(DoctorSlot s) {
        AppUserPrincipal me = currentUser.get();
        if (me.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the doctor manages their own schedule");
        }
        if (s.getHospitalId() == null) {
            throw new IllegalArgumentException("Clinic is required");
        }
        Hospital clinic = hospitalRepository.findById(s.getHospitalId())
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found"));
        if (!clinic.getId().equals(me.getHospitalId())) {
            throw new AccessDeniedException("Clinic does not belong to this doctor");
        }
        s.setHospitalId(clinic.getId());
        s.setDoctorPhno(me.getPhno());
        return repo.save(s);
    }

    public List<Hospital> getClinics() {
        AppUserPrincipal me = currentUser.get();
        return hospitalRepository.findById(me.getHospitalId()).stream().toList();
    }

    public List<DoctorSlot> getForDoctor(String doctorPhno) {
        return repo.findByHospitalIdAndDoctorPhno(currentUser.get().getHospitalId(), doctorPhno);
    }

    public List<DoctorSlot> getAll() {
        return repo.findByHospitalId(currentUser.get().getHospitalId());
    }

    public void delete(Long id) {
        DoctorSlot s = repo.findById(id).orElseThrow(() -> new RuntimeException("Slot not found"));
        AppUserPrincipal me = currentUser.get();
        if (!s.getHospitalId().equals(me.getHospitalId())) throw new AccessDeniedException("Cross-tenant access denied");
        if (me.getRole() == Role.ADMIN && !me.getPhno().equals(s.getDoctorPhno())) throw new AccessDeniedException("Not your slot");
        repo.deleteById(id);
    }
}
