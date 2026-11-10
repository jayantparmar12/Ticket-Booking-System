package com.jayant.backend.repository;

import com.jayant.backend.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByShowTimeAfterOrderByShowTimeAsc(LocalDateTime time);
}