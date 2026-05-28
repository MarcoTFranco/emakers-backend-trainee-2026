package com.emakers.library_api.controller;

import com.emakers.library_api.dto.request.BookRecordDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.repositores.BookRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController()
@Tag(name = "Books", description = "Endpoints para gerenciamento de livros")
public class BookController {

    @Autowired
    BookRepository bookRepository;

    @Operation(summary = "Cadastra um novo livro", description = "Salva as informações de um novo livro" +
            " no banco de dados.")
    @PostMapping("/books")
    public ResponseEntity<BookModel> saveBook(@RequestBody @Valid BookRecordDto bookRecordDto) {
        var bookModel = new BookModel(bookRecordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookRepository.save(bookModel));
    }

    @Operation(summary = "Lista todos os livros", description = "Retorna uma lista contendo todos os" +
            " livros cadastrados.")
    @GetMapping("/books")
    public ResponseEntity<List<BookModel>> getAllBooks() {
        return ResponseEntity.status(HttpStatus.OK).body(bookRepository.findAll());
    }

    @Operation(summary = "Busca um livro pelo ID", description = "Retorna os detalhes de um livro específico" +
            " utilizando o seu UUID.")
    @GetMapping("/books/{id}")
    public ResponseEntity<Object> getOneBook(@PathVariable(value="id")UUID id){
        Optional<BookModel> bookModel = bookRepository.findById(id);
        if(bookModel.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book Not Found");
        }
        return ResponseEntity.status(HttpStatus.OK).body(bookModel.get());
    }

    @Operation(summary = "Atualiza um livro", description = "Atualiza as informações de um livro existente com base" +
            " no ID fornecido.")
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

    @Operation(summary = "Deleta um livro", description = "Remove permanentemente um livro do banco de dados pelo seu ID.")
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
