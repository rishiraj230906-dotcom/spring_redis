package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bots")
public class Bot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "persona_description", length = 1000)
    private String personaDescription;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPersonaDescription() {
		return personaDescription;
	}

	public void setPersonaDescription(String personaDescription) {
		this.personaDescription = personaDescription;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Bot(Long id, String name, String personaDescription, LocalDateTime createdAt) {
		super();
		this.id = id;
		this.name = name;
		this.personaDescription = personaDescription;
		this.createdAt = createdAt;
	}

	public Bot() {
		super();
	}
    
}
