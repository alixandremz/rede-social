package com.alixandremz.rede.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alixandremz.rede.dto.UsuarioDetalhe;
import com.alixandremz.rede.dto.UsuarioRequest;
import com.alixandremz.rede.dto.UsuarioResumo;
import com.alixandremz.rede.model.Seguidor;
import com.alixandremz.rede.model.Usuario;
import com.alixandremz.rede.repository.PostRepository;
import com.alixandremz.rede.repository.SeguidorRepository;
import com.alixandremz.rede.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final SeguidorRepository seguidorRepositorio;
    private final PostRepository postRepositorio;

    public UsuarioService(UsuarioRepository repositorio, SeguidorRepository seguidorRepositorio,
            PostRepository postRepositorio) {
        this.repositorio = repositorio;
        this.seguidorRepositorio = seguidorRepositorio;
        this.postRepositorio = postRepositorio;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumo> listar() {
        return repositorio.findAllByOrderByNomeAsc().stream().map(UsuarioResumo::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDetalhe buscar(Long id) {
        Usuario usuario = obter(id);
        long seguidores = seguidorRepositorio.countBySeguidoId(id);
        long seguindo = seguidorRepositorio.countBySeguidorId(id);
        long posts = postRepositorio.countByAutorId(id);
        return UsuarioDetalhe.de(usuario, seguidores, seguindo, posts);
    }

    @Transactional
    public UsuarioResumo criar(UsuarioRequest pedido) {
        Usuario usuario = new Usuario();
        usuario.setNome(pedido.nome().trim());
        return UsuarioResumo.de(repositorio.save(usuario));
    }

    @Transactional
    public void seguir(Long seguidorId, Long seguidoId) {
        if (seguidorId.equals(seguidoId)) {
            throw new ConflitoException("um usuário não pode seguir a si mesmo");
        }
        Usuario seguidor = obter(seguidorId);
        Usuario seguido = obter(seguidoId);
        if (seguidorRepositorio.existsBySeguidorIdAndSeguidoId(seguidorId, seguidoId)) {
            throw new ConflitoException("já segue este usuário");
        }
        Seguidor relacao = new Seguidor();
        relacao.setSeguidor(seguidor);
        relacao.setSeguido(seguido);
        seguidorRepositorio.save(relacao);
    }

    @Transactional
    public void deixarDeSeguir(Long seguidorId, Long seguidoId) {
        Seguidor relacao = seguidorRepositorio.findBySeguidorIdAndSeguidoId(seguidorId, seguidoId)
                .orElseThrow(() -> new NaoEncontradoException("este usuário não segue o outro"));
        seguidorRepositorio.delete(relacao);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumo> listarSeguidores(Long id) {
        obter(id);
        return seguidorRepositorio.findBySeguidoIdOrderByCriadoEmDesc(id)
                .stream().map(s -> UsuarioResumo.de(s.getSeguidor())).toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumo> listarSeguindo(Long id) {
        obter(id);
        return seguidorRepositorio.findBySeguidorIdOrderByCriadoEmDesc(id)
                .stream().map(s -> UsuarioResumo.de(s.getSeguido())).toList();
    }

    public Usuario obter(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new NaoEncontradoException("usuário não encontrado: id " + id));
    }
}
