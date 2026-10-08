package com.group5.cats.service;

import java.util.List;

import com.group5.cats.model.TrainingProvider;

public interface TrainingProviderService {

    List<TrainingProvider> findAllProviders();

    TrainingProvider findProviderById(Long id);

    String createProvider(String name);

    String updateProvider(Long id, String name);

    String deleteProvider(Long id);
}