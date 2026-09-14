package com.example.nexuscore.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "profile")
public class Profile {

    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "profile_type_enum")
    private ProfileType type;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "profile_status_enum")
    private ProfileStatus status;

    @Column(name = "phone", length = 16)
    @CollectionTable(name = "profile_phone", joinColumns = @JoinColumn(name = "profile_id"))
    @ElementCollection
    private Set<String> phones = new LinkedHashSet<>();

    protected Profile() {}

    public Integer getId() {
        return id;
    }
    public Address getAddress() {
        return address;
    }
    public String getEmail() {
        return email;
    }
    public String getName() {
        return name;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public ProfileType getType() {
        return type;
    }
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    public ProfileStatus getStatus() {
        return status;
    }
    public Set<String> getPhones() {return phones;}

    public void setName(String name) {this.name = name;}

    public void setProfileImageUrl(String profileImageUrl) {this.profileImageUrl = profileImageUrl;}

    public void updatePhones(Set<String> phones) {
        this.phones.clear();
        this.phones.addAll(phones);
    }
}
