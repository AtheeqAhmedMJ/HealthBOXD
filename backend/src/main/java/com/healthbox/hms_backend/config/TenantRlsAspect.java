package com.healthbox.hms_backend.config;

import com.healthbox.hms_backend.security.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantRlsAspect {
    private final TransactionTemplate transactions;
    private final EntityManager entityManager;

    public TenantRlsAspect(TransactionTemplate transactions, EntityManager entityManager) {
        this.transactions = transactions;
        this.entityManager = entityManager;
    }

    @Around("execution(public * com.healthbox.hms_backend.modules..*Service.*(..))")
    public Object runWithTenant(ProceedingJoinPoint point) {
        return transactions.execute(status -> {
            applyTenantSetting();
            try {
                return point.proceed();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Throwable exception) {
                throw new IllegalStateException("Module operation failed", exception);
            }
        });
    }

    private void applyTenantSetting() {
        entityManager.createNativeQuery("select set_config('app.tenant_id', :tenant, true)")
                .setParameter("tenant", TenantContext.tenantId() == null ? "" : TenantContext.tenantId().toString())
                .getSingleResult();
        entityManager.createNativeQuery("select set_config('app.is_super_admin', :superAdmin, true)")
                .setParameter("superAdmin", Boolean.toString(TenantContext.isSuperAdmin()))
                .getSingleResult();
    }
}
