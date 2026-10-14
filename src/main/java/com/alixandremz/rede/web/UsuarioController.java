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

import com.alixandremz.rede.dto.PaginaResponse;
import com.alixandremz.rede.dto.PostResumo;
import com.alixandremz.rede.dto.UsuarioDetalhe;
import com.alixandremz.rede.dto.UsuarioRequest;
import com.alixandremz.rede.dto.UsuarioResumo;
import com.alixandremz.rede.service.PostService;
import com.alixandremz.rede.service.UsuarioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/usuarios")
@Validated
public class UsuarioController {

    private final UsuarioService servico;
    private final PostService postServico;

    public UsuarioController(UsuarioService servico, PostService postServico) {
        this.servico = servico;
        this.postServico = postServico;
    }

    @GetMapping
    public List<UsuarioResumo> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public UsuarioDetalhe buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResumo> criar(@Valid @RequestBody UsuarioRequest pedido) {
        UsuarioResumo criado = servico.criar(pedido);
        return ResponseEntity.created(URI.create("/usuarios/" + criado.id())).body(criado);
    }

    @PostMapping("/{id}/seguindo/{alvoId}")
    public ResponseEntity<Void> seguir(@PathVariable Long id, @PathVariable Long alvoId) {
        servico.seguir(id, alvoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/seguindo/{alvoId}")
    public ResponseEntity<Void> deixarDeSeguir(@PathVariable Long id, @PathVariable Long alvoId) {
        servico.deixarDeSeguir(id, alvoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/seguidores")
    public List<UsuarioResumo> seguidores(@PathVariable Long id) {
        return servico.listarSeguidores(id);
    }

    @GetMapping("/{id}/seguindo")
    public List<UsuarioResumo> seguindo(@PathVariable Long id) {
        return servico.listarSeguindo(id);
    }

    @GetMapping("/{id}/feed")
    public PaginaResponse<PostResumo> feed(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "a página não pode ser negativa") int pagina,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "o tamanho mínimo é 1")
            @Max(value = 100, message = "o tamanho máximo é 100") int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm"));
        return postServico.feed(id, pageable);
    }
}
