package com.emakers.library_api.models;

import com.emakers.library_api.dto.request.BookRecordDto;
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

    private String publicationDate;

    public BookModel() {}

    public BookModel(BookRecordDto bookRecordDto) {
        this.title = bookRecordDto.title();
        this.author = bookRecordDto.author();
        this.publicationDate = bookRecordDto.publicationDate();
    }

    public UUID getId() {
        return id;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    private void setAuthor(String author) {
        this.author = author;
    }

    private void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public void updateBook(BookRecordDto bookRecordDto) {
        setTitle(bookRecordDto.title());
        setAuthor(bookRecordDto.author());
        setPublicationDate(bookRecordDto.publicationDate());
    }
}
