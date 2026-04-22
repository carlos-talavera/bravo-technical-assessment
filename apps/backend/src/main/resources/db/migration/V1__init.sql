-- Tabla principal de solicitudes de crédito
CREATE TABLE credit_applications (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    country             VARCHAR(5)      NOT NULL,
    full_name           VARCHAR(255)    NOT NULL,
    document_id         VARCHAR(50)     NOT NULL,
    requested_amount    DECIMAL(15, 2)  NOT NULL,
    monthly_income      DECIMAL(15, 2)  NOT NULL,
    status              VARCHAR(30)     NOT NULL CHECK (
        status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED')
    ),
    bank_account_number VARCHAR(100),
    bank_total_debt     DECIMAL(15, 2),
    bank_credit_score   INTEGER,
    bank_name           VARCHAR(100),
    bank_currency       VARCHAR(3),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Cola de trabajos asíncronos — consumida con SELECT FOR UPDATE SKIP LOCKED
CREATE TABLE job_queue (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID            NOT NULL REFERENCES credit_applications(id),
    job_type        VARCHAR(50)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    attempts        INTEGER         NOT NULL DEFAULT 0,
    payload         JSONB,
    error_message   TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ
);

-- Historial de cambios de estado para auditoría
CREATE TYPE audit_source AS ENUM (
  'API',
  'WORKER',
  'WEBHOOK',
  'SYSTEM'
);

CREATE TABLE application_status_history (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID        NOT NULL REFERENCES credit_applications(id),
    previous_status VARCHAR(30) NOT NULL CHECK (
        previous_status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED')
    ),
    new_status      VARCHAR(30) NOT NULL CHECK (
        new_status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED')
    ),
    changed_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    source          audit_source NOT NULL DEFAULT 'SYSTEM',
    reason          TEXT,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Usuarios para autenticación JWT
CREATE TYPE user_role AS ENUM (
  'ADMIN',
  'USER',
  'ANALYST',
  'RISK_MANAGER'
);

CREATE TABLE app_users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          user_role  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Trigger: encola una tarea de evaluación de riesgo al crear una solicitud
CREATE OR REPLACE FUNCTION fn_enqueue_risk_evaluation()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO job_queue (application_id, job_type)
    VALUES (NEW.id, 'RISK_EVALUATION');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enqueue_on_insert
    AFTER INSERT ON credit_applications
    FOR EACH ROW EXECUTE FUNCTION fn_enqueue_risk_evaluation();

-- Trigger: registra en historial cada cambio de estado
CREATE OR REPLACE FUNCTION fn_record_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO application_status_history (application_id, previous_status, new_status)
        VALUES (NEW.id, OLD.status, NEW.status);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_status_change_audit
    AFTER UPDATE ON credit_applications
    FOR EACH ROW EXECUTE FUNCTION fn_record_status_change();

-- Índices para consultas frecuentes
CREATE INDEX idx_applications_country        ON credit_applications (country);
CREATE INDEX idx_applications_status         ON credit_applications (status);
CREATE INDEX idx_applications_country_status ON credit_applications (country, status);
CREATE INDEX idx_applications_created_at     ON credit_applications (created_at DESC);
CREATE INDEX idx_job_queue_pending           ON job_queue (status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_status_history_application  ON application_status_history (application_id, changed_at DESC);

-- Usuario admin por defecto (contraseña: admin123 — reemplazar antes de producción)
INSERT INTO app_users (username, password_hash, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');
