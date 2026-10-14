package com.alixandremz.rede.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alixandremz.rede.model.Curtida;

public interface CurtidaRepository extends JpaRepository<Curtida, Long> {

    boolean existsByPostIdAndUsuarioId(Long postId, Long usuarioId);

    Optional<Curtida> findByPostIdAndUsuarioId(Long postId, Long usuarioId);

    long countByPostId(Long postId);
}
