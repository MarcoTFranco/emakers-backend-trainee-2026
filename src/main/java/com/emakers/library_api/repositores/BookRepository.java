package com.emakers.library_api.repositores;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Book;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {
}
