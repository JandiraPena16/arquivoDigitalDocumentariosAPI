package com.arquivodigital.config;

import com.arquivodigital.entity.Categoria;
import com.arquivodigital.entity.Role;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.repository.CategoriaRepository;
import com.arquivodigital.repository.UtilizadorRepository;
import com.arquivodigital.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UtilizadorRepository utilizadorRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageUtil fileStorageUtil;

    @Override
    public void run(String... args) {
        fileStorageUtil.inicializar();
        seedAdmin();
        seedCategorias();
    }

    private void seedAdmin() {
        if (!utilizadorRepository.existsByEmail("admin@arquivo.ao")) {
            Utilizador admin = Utilizador.builder()
                    .nome("Administrador")
                    .email("admin@arquivo.ao")
                    .password(passwordEncoder.encode("Admin@2024"))
                    .role(Role.ADMIN)
                    .ativo(true)
                    .build();
            utilizadorRepository.save(admin);
            log.info("Admin criado: admin@arquivo.ao / Admin@2024");
        }
    }

    private void seedCategorias() {
        List<String[]> categorias = List.of(
                new String[]{"História", "Documentários históricos e arquivos culturais"},
                new String[]{"Natureza", "Documentários sobre fauna, flora e meio ambiente"},
                new String[]{"Ciência e Tecnologia", "Avanços científicos e inovações tecnológicas"},
                new String[]{"Arte e Cultura", "Expressões artísticas e patrimónios culturais"},
                new String[]{"Política e Sociedade", "Análise política, social e geopolítica"},
                new String[]{"Economia", "Mercados, negócios e desenvolvimento económico"},
                new String[]{"Desporto", "Desportos, atletas e eventos desportivos"},
                new String[]{"Angola", "Documentários focados em Angola e cultura angolana"}
        );

        for (String[] cat : categorias) {
            if (!categoriaRepository.existsByNomeIgnoreCase(cat[0])) {
                categoriaRepository.save(Categoria.builder()
                        .nome(cat[0])
                        .descricao(cat[1])
                        .build());
            }
        }
        log.info("Categorias iniciais verificadas/criadas");
    }
}
