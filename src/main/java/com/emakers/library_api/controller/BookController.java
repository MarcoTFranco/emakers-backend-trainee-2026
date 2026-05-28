package com.emakers.library_api.controller;

import com.emakers.library_api.dto.request.BookRecordDto;
import com.emakers.library_api.dto.response.BookResponseDto;
import com.emakers.library_api.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController()
@RequestMapping("/books")
@Tag(name = "Books", description = "Endpoints para gerenciamento de livros")
public class BookController {

    BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(summary = "Cadastra um novo livro", description = "Salva as informações de um novo livro" +
            " no banco de dados.")
    @PostMapping()
    public ResponseEntity<BookResponseDto> saveBook(@RequestBody @Valid BookRecordDto bookRecordDto) {
        var bookResponseDto = bookService.saveBook(bookRecordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookResponseDto);
    }

    @Operation(summary = "Lista todos os livros", description = "Retorna uma lista contendo todos os" +
            " livros cadastrados.")
    @GetMapping()
    public ResponseEntity<List<BookResponseDto>> getAllBooks() {
        var bookResponseDtos = bookService.getAllBooks();
        return ResponseEntity.status(HttpStatus.OK).body(bookResponseDtos);
    }

    @Operation(summary = "Busca um livro pelo ID", description = "Retorna os detalhes de um livro específico" +
            " utilizando o seu UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<Object> getOneBook(@PathVariable UUID id){
        Optional<BookResponseDto> bookResponseDto = bookService.getOneBook(id);
        return bookResponseDto.<ResponseEntity<Object>>map(responseDto -> ResponseEntity
                .status(HttpStatus.OK).body(responseDto)).orElseGet(() -> ResponseEntity
                .status(HttpStatus.NOT_FOUND).body("Book Not Found"));
    }

    @Operation(summary = "Atualiza um livro", description = "Atualiza as informações de um livro existente com base" +
            " no ID fornecido.")
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateBook(@PathVariable UUID id,
                                             @RequestBody @Valid BookRecordDto bookRecordDto){
        Optional<BookResponseDto> bookResponseDto = bookService.updateBook(id, bookRecordDto);
        return bookResponseDto.<ResponseEntity<Object>>map(responseDto -> ResponseEntity
                .status(HttpStatus.OK).body(responseDto)).orElseGet(() -> ResponseEntity
                .status(HttpStatus.NOT_FOUND).body("Book Not Found"));
    }

    @Operation(summary = "Deleta um livro", description = "Remove permanentemente um livro do banco de dados pelo seu ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteBook(@PathVariable UUID id){
        boolean deleted = bookService.deleteBook(id);
        if(!deleted){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book Not Found");
        }
        return ResponseEntity.status(HttpStatus.OK).body("Book Deleted");
    }
}
