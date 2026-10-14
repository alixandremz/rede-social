package com.alixandremz.rede.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.alixandremz.rede.model.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    long countByAutorId(Long autorId);

    // feed: posts de quem o usuário segue (subconsulta)
    @Query("""
            select p from Post p
            where p.autor.id in (select s.seguido.id from Seguidor s where s.seguidor.id = :usuarioId)
            """)
    Page<Post> feedDeSeguidos(@Param("usuarioId") Long usuarioId, Pageable pageable);

    // posts mais curtidos (agregação com count + ordenação)
    @Query("""
            select p from Post p
            left join p.curtidas c
            group by p
            order by count(c) desc
            """)
    List<Post> maisCurtidos(Pageable pageable);
}
