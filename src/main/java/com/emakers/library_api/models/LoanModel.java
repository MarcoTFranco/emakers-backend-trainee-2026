package com.emakers.library_api.models;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "TB_LOANS")
public class LoanModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private BookModel book;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private PersonModel person;

    private Boolean active;

    public LoanModel() {
    }

    public LoanModel(PersonModel person, BookModel book) {
        this.person = person;
        this.book = book;
    }

    public UUID getId() {
        return id;
    }

    public BookModel getBook() {
        return book;
    }

    public PersonModel getPerson() {
        return person;
    }

    public void setBook(BookModel book) {
        this.book = book;
    }

    public void setPerson(PersonModel person) {
        this.person = person;
    }

    public void deleteThisLoan() {
        this.active = false;
    }

}
