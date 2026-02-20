package com.waiveliability.modules.forms.domain;

import com.waiveliability.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "form_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormField extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @Column(name = "field_type", nullable = false, length = 50)
    private String fieldType;

    @Column(nullable = false)
    private String label;

    @Column(length = 255)
    private String placeholder;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "field_order", nullable = false)
    private int fieldOrder;

    @Column(columnDefinition = "jsonb")
    private String options;
}
