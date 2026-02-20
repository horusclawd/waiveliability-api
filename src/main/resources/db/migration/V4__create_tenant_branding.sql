CREATE TABLE tenant_branding (
    tenant_id       UUID        PRIMARY KEY REFERENCES tenants(id),
    primary_color   VARCHAR(7),
    bg_color        VARCHAR(7),
    font_family     VARCHAR(100),
    logo_s3_key     VARCHAR(500),
    hide_powered_by BOOLEAN     NOT NULL DEFAULT false,
    custom_domain   VARCHAR(255),
    domain_verified BOOLEAN     NOT NULL DEFAULT false
);
