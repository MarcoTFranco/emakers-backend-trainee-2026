package com.emakers.library_api.config;

import com.emakers.library_api.models.PersonModel;
import com.emakers.library_api.models.UserRole;
import com.emakers.library_api.repositores.PersonRepository;
import com.emakers.library_api.validation.ValidCPFValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidCPFValidator cpfValidator = new ValidCPFValidator();

    @Value("${ADMIN_EMAIL:#{null}}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:#{null}}")
    private String adminPassword;

    @Value("${ADMIN_CPF:#{null}}")
    private String adminCpf;

    @Value("${ADMIN_NAME:Administrator}")
    private String adminName;

    @Value("${ADMIN_CEP:}")
    private String adminCep;

    public AdminInitializer(PersonRepository personRepository, PasswordEncoder passwordEncoder) {
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (personRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }

        if (adminEmail == null || adminPassword == null || adminCpf == null) {
            log.warn("============================================================");
            log.warn("  AVISO: Nenhum administrador encontrado no banco de dados.");
            log.warn("  Defina as variáveis de ambiente para criar o primeiro admin:");
            log.warn("    ADMIN_EMAIL=seu@email.com");
            log.warn("    ADMIN_PASSWORD=sua_senha_forte");
            log.warn("    ADMIN_CPF=000.000.000-00");
            log.warn("    ADMIN_NAME=Seu Nome  (opcional)");
            log.warn("    ADMIN_CEP=00000-000  (opcional)");
            log.warn("============================================================");
            return;
        }

        if (adminPassword.length() < 8) {
            log.error("ADMIN_PASSWORD precisa ter no mínimo 8 caracteres. Admin não criado.");
            return;
        }

        if (!cpfValidator.isValid(adminCpf, null)) {
            log.error("ADMIN_CPF '{}' é inválido. Admin não criado.", adminCpf);
            return;
        }

        String normalizedEmail = adminEmail.toLowerCase().trim();

        if (personRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Já existe um usuário com o email '{}'. Admin não criado.", normalizedEmail);
            return;
        }

        if (personRepository.existsByCpf(adminCpf)) {
            log.warn("Já existe um usuário com o CPF informado. Admin não criado.");
            return;
        }

        var admin = new PersonModel();
        admin.setName(adminName.trim());
        admin.setCpf(adminCpf);
        admin.setZipCode(adminCep);
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);

        personRepository.save(admin);
        log.info("Primeiro administrador criado com sucesso: {}", normalizedEmail);
    }
}
