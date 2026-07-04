package com.geeknito.LMS_backend.entity.media;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String name;

    @Column(length = 1000, nullable = false)
    private String url;

    private Long size;

    @Column(length = 200)
    private String mimeType;

    @Column(length = 100)
    private String storageProvider;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;
}
