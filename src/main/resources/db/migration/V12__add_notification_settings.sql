-- Flyway V12: Add notification settings to tenants table

-- Add notifications_enabled column
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS notifications_enabled BOOLEAN NOT NULL DEFAULT false;

-- Add notification_email column
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS notification_email VARCHAR(255);

-- Create index for notification_email
CREATE INDEX IF NOT EXISTS idx_tenants_notification_email ON tenants(notification_email);
