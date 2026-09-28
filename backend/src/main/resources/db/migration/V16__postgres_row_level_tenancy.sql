-- Database-enforced tenant isolation for the shared-schema modular monolith.
-- The application sets these LOCAL transaction settings from the verified JWT.
DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'patients', 'appointments', 'prescriptions', 'billing',
        'pharmacy_records', 'doctor_slots', 'charge_items', 'payment_orders'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', table_name);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I USING (
                current_setting(''app.is_super_admin'', true) = ''true''
                OR hospital_id = NULLIF(current_setting(''app.tenant_id'', true), '''')::bigint
            ) WITH CHECK (
                current_setting(''app.is_super_admin'', true) = ''true''
                OR hospital_id = NULLIF(current_setting(''app.tenant_id'', true), '''')::bigint
            )', table_name
        );
    END LOOP;
END $$;

ALTER TABLE consultations ENABLE ROW LEVEL SECURITY;
ALTER TABLE consultations FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON consultations;
CREATE POLICY tenant_isolation ON consultations
USING (
    current_setting('app.is_super_admin', true) = 'true'
    OR EXISTS (
        SELECT 1 FROM patients
        WHERE patients.patient_id = consultations.patient_id
          AND patients.hospital_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint
    )
)
WITH CHECK (
    current_setting('app.is_super_admin', true) = 'true'
    OR EXISTS (
        SELECT 1 FROM patients
        WHERE patients.patient_id = consultations.patient_id
          AND patients.hospital_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint
    )
);

ALTER TABLE prescription_medications ENABLE ROW LEVEL SECURITY;
ALTER TABLE prescription_medications FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON prescription_medications;
CREATE POLICY tenant_isolation ON prescription_medications
USING (
    current_setting('app.is_super_admin', true) = 'true'
    OR EXISTS (
        SELECT 1 FROM prescriptions
        WHERE prescriptions.id = prescription_medications.prescription_id
          AND prescriptions.hospital_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint
    )
)
WITH CHECK (
    current_setting('app.is_super_admin', true) = 'true'
    OR EXISTS (
        SELECT 1 FROM prescriptions
        WHERE prescriptions.id = prescription_medications.prescription_id
          AND prescriptions.hospital_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint
    )
);
