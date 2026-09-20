package com.example.nexuscore.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "profile")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "address_id")
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

    protected Profile() {
    }

    public Profile(Address address, String email, String name, String profileImageUrl,
                   ProfileType type, Set<String> phones) {
        this.address = address;
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.type = type;
        this.status = ProfileStatus.ACTIVE;
        setPhones(phones);
    }

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

    public Set<String> getPhones() {
        return phones;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setPhones(Set<String> phones) {
        this.phones.clear();
        if (phones != null) {
            this.phones.addAll(phones);
        }
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
