package com.group5.cats.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.CommonCourseRepository;
import com.group5.cats.repository.TrainingProviderRepository;

@Service
public class TrainingProviderServiceImpl
        implements TrainingProviderService {

    private final TrainingProviderRepository trainingProviderRepository;
    private final CommonCourseRepository commonCourseRepository;

    public TrainingProviderServiceImpl(
            TrainingProviderRepository trainingProviderRepository,
            CommonCourseRepository commonCourseRepository) {

        this.trainingProviderRepository = trainingProviderRepository;
        this.commonCourseRepository = commonCourseRepository;
    }

    @Override
    public List<TrainingProvider> findAllProviders() {
        return trainingProviderRepository.findAllByOrderByNameAsc();
    }

    @Override
    public TrainingProvider findProviderById(Long id) {
        return trainingProviderRepository.findById(id).orElse(null);
    }

    @Override
    public String createProvider(String name) {

        if (name == null || name.isBlank()) {
            return "Provider name is required.";
        }

        String providerName = name.trim();

        if (providerName.length() > 255) {
            return "Provider name must not exceed 255 characters.";
        }

        if (trainingProviderRepository
                .findByNameIgnoreCase(providerName).isPresent()) {
            return "Provider name already exists.";
        }

        TrainingProvider provider = new TrainingProvider(providerName);

        trainingProviderRepository.save(provider);

        return null;
    }

    @Override
    @Transactional
    public String updateProvider(Long id, String name) {

        TrainingProvider provider =
                trainingProviderRepository.findById(id).orElse(null);

        if (provider == null) {
            return "Training provider not found.";
        }

        if (name == null || name.isBlank()) {
            return "Provider name is required.";
        }

        String providerName = name.trim();

        if (providerName.length() > 255) {
            return "Provider name must not exceed 255 characters.";
        }

        Optional<TrainingProvider> existing =
                trainingProviderRepository.findByNameIgnoreCase(
                        providerName
                );

        if (existing.isPresent()
                && !existing.get().getId().equals(id)) {
            return "Provider name already exists.";
        }

        provider.setName(providerName);

        trainingProviderRepository.save(provider);

        return null;
    }

    @Override
    @Transactional
    public String deleteProvider(Long id) {

        TrainingProvider provider =
                trainingProviderRepository.findById(id).orElse(null);

        if (provider == null) {
            return "Training provider not found.";
        }

        if (commonCourseRepository.existsByProvider_Id(id)) {
            return "This provider has commonly attended courses and cannot be deleted.";
        }

        trainingProviderRepository.delete(provider);

        return null;
    }
}