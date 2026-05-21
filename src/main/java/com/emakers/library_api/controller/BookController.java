package com.emakers.library_api.controller;

import com.emakers.library_api.dto.BookRecordDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.repositores.BookRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController()
public class BookController {

    @Autowired
    BookRepository bookRepository;

    @PostMapping("/books")
    public ResponseEntity<BookModel> saveBook(@RequestBody @Valid BookRecordDto bookRecordDto) {
        var bookModel = new BookModel(bookRecordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookRepository.save(bookModel));
    }

    @GetMapping("/books")
    public ResponseEntity<List<BookModel>> getAllBooks() {
        return ResponseEntity.status(HttpStatus.OK).body(bookRepository.findAll());
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<Object> getOneBook(@PathVariable(value="id")UUID id){
        Optional<BookModel> bookModel = bookRepository.findById(id);
        if(bookModel.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book Not Found");
        }
        return ResponseEntity.status(HttpStatus.OK).body(bookModel.get());
    }

    @PutMapping("/books/{id}")
    public ResponseEntity<Object> updateBook(@PathVariable(value="id")UUID id,
                                             @RequestBody @Valid BookRecordDto bookRecordDto){
        Optional<BookModel> bookModel = bookRepository.findById(id);
        if(bookModel.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book Not Found");
        }
        var bookUpdate = bookModel.get();
        bookUpdate.updateBook(bookRecordDto);
        return ResponseEntity.status(HttpStatus.OK).body(bookRepository.save(bookUpdate));
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<Object> deleteBook(@PathVariable(value="id")UUID id){
        Optional<BookModel> bookModel = bookRepository.findById(id);
        if(bookModel.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book Not Found");
        }
        bookRepository.delete(bookModel.get());
        return ResponseEntity.status(HttpStatus.OK).body("Book Deleted");
    }

}
