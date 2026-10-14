package com.alixandremz.rede.web;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alixandremz.rede.dto.CurtidaRequest;
import com.alixandremz.rede.dto.PaginaResponse;
import com.alixandremz.rede.dto.PostRequest;
import com.alixandremz.rede.dto.PostResumo;
import com.alixandremz.rede.service.PostService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/posts")
@Validated
public class PostController {

    private final PostService servico;

    public PostController(PostService servico) {
        this.servico = servico;
    }

    @GetMapping
    public PaginaResponse<PostResumo> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "a página não pode ser negativa") int pagina,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "o tamanho mínimo é 1")
            @Max(value = 100, message = "o tamanho máximo é 100") int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm"));
        return servico.listar(pageable);
    }

    @GetMapping("/populares")
    public List<PostResumo> populares() {
        return servico.populares();
    }

    @GetMapping("/{id}")
    public PostResumo buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @PostMapping
    public ResponseEntity<PostResumo> criar(@Valid @RequestBody PostRequest pedido) {
        PostResumo criado = servico.criar(pedido);
        return ResponseEntity.created(URI.create("/posts/" + criado.id())).body(criado);
    }

    @PostMapping("/{id}/curtidas")
    public ResponseEntity<Void> curtir(@PathVariable Long id, @Valid @RequestBody CurtidaRequest pedido) {
        servico.curtir(id, pedido.usuarioId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/curtidas/{usuarioId}")
    public ResponseEntity<Void> descurtir(@PathVariable Long id, @PathVariable Long usuarioId) {
        servico.descurtir(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
