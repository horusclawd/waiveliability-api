CREATE TABLE forms (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE form_fields (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id      UUID        NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    field_type   VARCHAR(50) NOT NULL,
    label        VARCHAR(255) NOT NULL,
    placeholder  VARCHAR(255),
    required     BOOLEAN     NOT NULL DEFAULT false,
    field_order  INTEGER     NOT NULL,
    options      JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_forms_tenant_id ON forms(tenant_id);
CREATE INDEX idx_form_fields_form_id ON form_fields(form_id);
