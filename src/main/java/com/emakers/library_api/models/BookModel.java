package com.emakers.library_api.models;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "TB_BOOKS")
public class BookModel implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String title;

    private String author;

    private String date;

    public BookModel() {}

    public BookModel(UUID id, String title, String author, String date) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.date = date;
    }
}
