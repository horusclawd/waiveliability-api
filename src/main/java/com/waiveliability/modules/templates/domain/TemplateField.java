package com.waiveliability.modules.templates.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "template_fields")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @Column(name = "field_type")
    private String fieldType;

    private String label;

    private String placeholder;

    private boolean required;

    @Column(name = "field_order")
    private int fieldOrder;

    private String options;

    @Column(columnDefinition = "TEXT")
    private String content;
}
