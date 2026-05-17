package com.emakers.library_api.controller;

import com.emakers.library_api.repositores.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController {

    @Autowired
    BookRepository bookRepository;


}
