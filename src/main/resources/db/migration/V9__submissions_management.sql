-- GIN index for full-text search on form_data JSONB
CREATE INDEX IF NOT EXISTS idx_submissions_form_data_gin ON submissions USING GIN (form_data);

-- Index for common filter queries
CREATE INDEX IF NOT EXISTS idx_submissions_tenant_status ON submissions (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_submissions_tenant_form ON submissions (tenant_id, form_id);
CREATE INDEX IF NOT EXISTS idx_submissions_submitted_at ON submissions (submitted_at DESC);
