package com.emakers.library_api.service;

import com.emakers.library_api.dto.request.BookRecordDto;
import com.emakers.library_api.dto.response.BookResponseDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.repositores.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookService {

    BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public BookResponseDto saveBook(BookRecordDto bookRecordDto) {
        var bookModel = new BookModel(bookRecordDto);
        bookRepository.save(bookModel);
        return new BookResponseDto(bookModel);
    }

    public List<BookResponseDto> getAllBooks() {
        return bookRepository.findAll().stream().map(BookResponseDto::new).toList();
    }

    public Optional<BookResponseDto> getOneBook(UUID id) {
        Optional<BookModel> bookModel = bookRepository.findById(id);
        return bookModel.map(BookResponseDto::new);
    }

    public Optional<BookResponseDto> updateBook(UUID id, BookRecordDto bookRecordDto) {
        Optional<BookModel> bookModel = bookRepository.findById(id);
        if(bookModel.isEmpty()) {
            return Optional.empty();
        }
        var bookUpdate = bookModel.get();
        bookUpdate.updateBook(bookRecordDto);
        bookRepository.save(bookUpdate);
        return Optional.of(new BookResponseDto(bookUpdate));
    }

    public Boolean deleteBook(UUID id) {
        Optional<BookModel> bookModel = bookRepository.findById(id);
        if(bookModel.isEmpty()) {
            return false;
        }
        bookRepository.delete(bookModel.get());
        return true;
    }
}
