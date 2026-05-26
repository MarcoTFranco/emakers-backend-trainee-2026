package com.emakers.library_api.config;

import com.emakers.library_api.dto.BookRecordDto;
import com.emakers.library_api.dto.PersonRecordDto;
import com.emakers.library_api.models.BookModel;
import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.models.UserRole;
import com.emakers.library_api.repositores.BookRepository;
import com.emakers.library_api.repositores.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private BookRepository bookRepository;

    @Override
    public void run(String... args) throws Exception {
        seedUsers();
        seedBooks();
    }

    private void seedUsers() {
        if (personRepository.count() == 0) {
            PersonRecordDto adminDto = new PersonRecordDto("Super Admin", "000.000.000-00",
                    "35500-000", "admin@emakers.com", "admin123");
            PersonModel admin = new PersonModel(adminDto);
            admin.setRole(UserRole.ADMIN);

            PersonRecordDto leitorDto = new PersonRecordDto("Marco Túlio", "111.222.333-44",
                    "35500-130", "marco@ufla.br", "senha123");
            PersonModel leitor = new PersonModel(leitorDto);

            PersonRecordDto leitor2Dto = new PersonRecordDto("João Silva", "999.888.777-66",
                    "12345-678", "joao@exemplo.com", "senha123");
            PersonModel leitor2 = new PersonModel(leitor2Dto);

            personRepository.saveAll(List.of(admin, leitor, leitor2));
        }
    }

    private void seedBooks() {
        if (bookRepository.count() == 0) {
            BookRecordDto book1Dto = new BookRecordDto("Código Limpo", "Robert C. Martin", "2008/08/01");
            BookModel book1 = new BookModel(book1Dto);

            BookRecordDto book2Dto = new BookRecordDto("Arquitetura Limpa", "Robert C. Martin", "2017/09/10");
            BookModel book2 = new BookModel(book2Dto);

            BookRecordDto book3Dto = new BookRecordDto("O Programador Pragmático", "Andrew Hunt", "1999/10/20");
            BookModel book3 = new BookModel(book3Dto);

            BookRecordDto book4Dto = new BookRecordDto("Domain-Driven Design", "Eric Evans", "2003/08/20");
            BookModel book4 = new BookModel(book4Dto);

            bookRepository.saveAll(List.of(book1, book2, book3, book4));
        }
    }
}
