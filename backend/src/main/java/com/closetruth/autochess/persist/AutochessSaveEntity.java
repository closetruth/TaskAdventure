package com.closetruth.autochess.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "autochess_save")
public class AutochessSaveEntity {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Lob
    @Column(nullable = false)
    private String payload;

    protected AutochessSaveEntity() {
    }

    public AutochessSaveEntity(String payload) {
        this.id = SINGLETON_ID;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
