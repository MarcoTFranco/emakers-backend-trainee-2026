package com.emakers.library_api.models;

import com.emakers.library_api.dto.request.BookRecordDto;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDate;
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

    private LocalDate publicationDate;

    public BookModel() {}

    public BookModel(BookRecordDto bookRecordDto) {
        this.title = bookRecordDto.title();
        this.author = bookRecordDto.author();
        this.publicationDate = bookRecordDto.publicationDate();
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }


    public void updateBook(BookRecordDto bookRecordDto) {
        this.title =  bookRecordDto.title();
        this.author = bookRecordDto.author();
        this.publicationDate = bookRecordDto.publicationDate();
    }
}
