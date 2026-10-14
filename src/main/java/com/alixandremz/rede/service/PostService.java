package com.alixandremz.rede.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alixandremz.rede.dto.PaginaResponse;
import com.alixandremz.rede.dto.PostRequest;
import com.alixandremz.rede.dto.PostResumo;
import com.alixandremz.rede.model.Curtida;
import com.alixandremz.rede.model.Post;
import com.alixandremz.rede.model.Usuario;
import com.alixandremz.rede.repository.CurtidaRepository;
import com.alixandremz.rede.repository.PostRepository;
import com.alixandremz.rede.repository.UsuarioRepository;

@Service
public class PostService {

    private static final int TAMANHO_POPULARES = 10;

    private final PostRepository repositorio;
    private final UsuarioRepository usuarioRepositorio;
    private final CurtidaRepository curtidaRepositorio;

    public PostService(PostRepository repositorio, UsuarioRepository usuarioRepositorio,
            CurtidaRepository curtidaRepositorio) {
        this.repositorio = repositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.curtidaRepositorio = curtidaRepositorio;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PostResumo> listar(Pageable pageable) {
        return PaginaResponse.de(repositorio.findAll(pageable).map(PostResumo::de));
    }

    @Transactional(readOnly = true)
    public List<PostResumo> populares() {
        return repositorio.maisCurtidos(PageRequest.of(0, TAMANHO_POPULARES))
                .stream().map(PostResumo::de).toList();
    }

    @Transactional(readOnly = true)
    public PostResumo buscar(Long id) {
        return PostResumo.de(obter(id));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PostResumo> feed(Long usuarioId, Pageable pageable) {
        if (!usuarioRepositorio.existsById(usuarioId)) {
            throw new NaoEncontradoException("usuário não encontrado: id " + usuarioId);
        }
        return PaginaResponse.de(repositorio.feedDeSeguidos(usuarioId, pageable).map(PostResumo::de));
    }

    @Transactional
    public PostResumo criar(PostRequest pedido) {
        Usuario autor = usuarioRepositorio.findById(pedido.autorId())
                .orElseThrow(() -> new NaoEncontradoException("usuário não encontrado: id " + pedido.autorId()));
        Post post = new Post();
        post.setAutor(autor);
        post.setConteudo(pedido.conteudo().trim());
        return PostResumo.de(repositorio.save(post));
    }

    @Transactional
    public void curtir(Long postId, Long usuarioId) {
        Post post = obter(postId);
        Usuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("usuário não encontrado: id " + usuarioId));
        if (curtidaRepositorio.existsByPostIdAndUsuarioId(postId, usuarioId)) {
            throw new ConflitoException("usuário já curtiu este post");
        }
        Curtida curtida = new Curtida();
        curtida.setPost(post);
        curtida.setUsuario(usuario);
        curtidaRepositorio.save(curtida);
    }

    @Transactional
    public void descurtir(Long postId, Long usuarioId) {
        Curtida curtida = curtidaRepositorio.findByPostIdAndUsuarioId(postId, usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("este usuário não curtiu o post"));
        curtidaRepositorio.delete(curtida);
    }

    Post obter(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new NaoEncontradoException("post não encontrado: id " + id));
    }
}
