package com.emakers.library_api.repositores;

import com.emakers.library_api.models.BookModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<BookModel, UUID> {
    Optional<BookModel> findByTitle(String title);
}
