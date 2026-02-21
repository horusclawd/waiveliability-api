package com.waiveliability.modules.submissions.domain;

import com.waiveliability.modules.forms.domain.Form;
import com.waiveliability.modules.identity.domain.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    private String submitterName;
    private String submitterEmail;

    @Column(columnDefinition = "jsonb")
    private String formData;

    private String signatureS3Key;

    @Column(nullable = true)
    private String pdfS3Key;

    private String status;

    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        submittedAt = now;
        createdAt = now;
        updatedAt = now;
        if (status == null) status = "pending";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
