CREATE TABLE subscriptions (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID        NOT NULL REFERENCES tenants(id) UNIQUE,
    stripe_customer_id     VARCHAR(100),
    stripe_subscription_id VARCHAR(100),
    status                 VARCHAR(50),   -- active, canceled, past_due, trialing
    current_period_end     TIMESTAMPTZ,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_tenant ON subscriptions(tenant_id);
