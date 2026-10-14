package com.alixandremz.rede.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alixandremz.rede.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findAllByOrderByNomeAsc();
}
