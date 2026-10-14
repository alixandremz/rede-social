package com.alixandremz.rede.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alixandremz.rede.model.Seguidor;

public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {

    boolean existsBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);

    Optional<Seguidor> findBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);

    // quantos seguidores o usuário tem
    long countBySeguidoId(Long seguidoId);

    // quantos usuários ele segue
    long countBySeguidorId(Long seguidorId);

    List<Seguidor> findBySeguidoIdOrderByCriadoEmDesc(Long seguidoId);

    List<Seguidor> findBySeguidorIdOrderByCriadoEmDesc(Long seguidorId);
}
