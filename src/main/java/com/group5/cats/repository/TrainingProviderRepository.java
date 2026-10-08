package com.group5.cats.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.cats.model.TrainingProvider;

public interface TrainingProviderRepository
        extends JpaRepository<TrainingProvider, Long> {

    List<TrainingProvider> findAllByOrderByNameAsc();

    Optional<TrainingProvider> findByNameIgnoreCase(String name);
}