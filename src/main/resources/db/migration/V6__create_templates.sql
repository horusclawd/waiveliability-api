CREATE TABLE templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL DEFAULT 'general',
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    usage_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE template_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES templates(id) ON DELETE CASCADE,
    field_type VARCHAR(50) NOT NULL,
    label VARCHAR(255) NOT NULL,
    placeholder VARCHAR(255),
    required BOOLEAN NOT NULL DEFAULT FALSE,
    field_order INT NOT NULL DEFAULT 0,
    options TEXT
);

-- Seed templates
INSERT INTO templates (id, name, description, category, is_premium) VALUES
  ('00000000-0000-0000-0000-000000000001', 'Basic Waiver', 'Simple liability waiver with name, email and signature', 'waiver', false),
  ('00000000-0000-0000-0000-000000000002', 'Activity Release', 'Activity participation release with emergency contact', 'waiver', false),
  ('00000000-0000-0000-0000-000000000003', 'NDA Agreement', 'Non-disclosure agreement with terms checkbox', 'legal', true),
  ('00000000-0000-0000-0000-000000000004', 'Photo Release', 'Photo and media release consent form', 'consent', false);

INSERT INTO template_fields (template_id, field_type, label, placeholder, required, field_order) VALUES
  -- Basic Waiver fields
  ('00000000-0000-0000-0000-000000000001', 'text', 'Full Name', 'Enter your full name', true, 0),
  ('00000000-0000-0000-0000-000000000001', 'email', 'Email Address', 'Enter your email', true, 1),
  ('00000000-0000-0000-0000-000000000001', 'text', 'Signature', 'Type your full name as signature', true, 2),
  -- Activity Release fields
  ('00000000-0000-0000-0000-000000000002', 'text', 'Participant Name', 'Full name', true, 0),
  ('00000000-0000-0000-0000-000000000002', 'email', 'Email', 'Email address', true, 1),
  ('00000000-0000-0000-0000-000000000002', 'text', 'Emergency Contact', 'Name and phone number', true, 2),
  ('00000000-0000-0000-0000-000000000002', 'checkbox', 'I agree to the terms and conditions', null, true, 3),
  -- NDA fields
  ('00000000-0000-0000-0000-000000000003', 'text', 'Full Name', 'Enter your full name', true, 0),
  ('00000000-0000-0000-0000-000000000003', 'text', 'Company Name', 'Your company or organization', false, 1),
  ('00000000-0000-0000-0000-000000000003', 'email', 'Email', 'Enter your email', true, 2),
  ('00000000-0000-0000-0000-000000000003', 'checkbox', 'I agree to the terms of this NDA', null, true, 3),
  -- Photo Release fields
  ('00000000-0000-0000-0000-000000000004', 'text', 'Full Name', 'Enter your full name', true, 0),
  ('00000000-0000-0000-0000-000000000004', 'email', 'Email', 'Enter your email', true, 1),
  ('00000000-0000-0000-0000-000000000004', 'checkbox', 'I grant permission to use my photo/likeness', null, true, 2),
  ('00000000-0000-0000-0000-000000000004', 'text', 'Signature', 'Type your full name', true, 3);
