package com.waiveliability.modules.templates.domain;

import com.waiveliability.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String description;

    private String category;

    @Column(name = "is_premium")
    private boolean isPremium;

    @Column(name = "usage_count")
    private long usageCount;

    @OneToMany(mappedBy = "template", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("fieldOrder ASC")
    @Builder.Default
    private List<TemplateField> fields = new ArrayList<>();
}
