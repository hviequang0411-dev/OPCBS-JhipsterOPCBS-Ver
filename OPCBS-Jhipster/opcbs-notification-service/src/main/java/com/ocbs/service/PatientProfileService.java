package com.ocbs.service;

import com.ocbs.domain.PatientProfile;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.service.dto.PatientProfileDTO;
import com.ocbs.service.mapper.PatientProfileMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.ocbs.domain.PatientProfile}.
 */
@Service
@Transactional
public class PatientProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(PatientProfileService.class);

    private final PatientProfileRepository patientProfileRepository;

    private final PatientProfileMapper patientProfileMapper;

    public PatientProfileService(PatientProfileRepository patientProfileRepository, PatientProfileMapper patientProfileMapper) {
        this.patientProfileRepository = patientProfileRepository;
        this.patientProfileMapper = patientProfileMapper;
    }

    /**
     * Save a patientProfile.
     *
     * @param patientProfileDTO the entity to save.
     * @return the persisted entity.
     */
    public PatientProfileDTO save(PatientProfileDTO patientProfileDTO) {
        LOG.debug("Request to save PatientProfile : {}", patientProfileDTO);
        PatientProfile patientProfile = patientProfileMapper.toEntity(patientProfileDTO);
        patientProfile = patientProfileRepository.save(patientProfile);
        return patientProfileMapper.toDto(patientProfile);
    }

    /**
     * Update a patientProfile.
     *
     * @param patientProfileDTO the entity to save.
     * @return the persisted entity.
     */
    public PatientProfileDTO update(PatientProfileDTO patientProfileDTO) {
        LOG.debug("Request to update PatientProfile : {}", patientProfileDTO);
        PatientProfile patientProfile = patientProfileMapper.toEntity(patientProfileDTO);
        patientProfile = patientProfileRepository.save(patientProfile);
        return patientProfileMapper.toDto(patientProfile);
    }

    /**
     * Partially update a patientProfile.
     *
     * @param patientProfileDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PatientProfileDTO> partialUpdate(PatientProfileDTO patientProfileDTO) {
        LOG.debug("Request to partially update PatientProfile : {}", patientProfileDTO);

        return patientProfileRepository
            .findById(patientProfileDTO.getId())
            .map(existingPatientProfile -> {
                patientProfileMapper.partialUpdate(existingPatientProfile, patientProfileDTO);

                return existingPatientProfile;
            })
            .map(patientProfileRepository::save)
            .map(patientProfileMapper::toDto);
    }

    /**
     * Get all the patientProfiles.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<PatientProfileDTO> findAll() {
        LOG.debug("Request to get all PatientProfiles");
        return patientProfileRepository
            .findAll()
            .stream()
            .map(patientProfileMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the patientProfiles with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<PatientProfileDTO> findAllWithEagerRelationships() {
        return patientProfileRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(patientProfileMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one patientProfile by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PatientProfileDTO> findOne(Long id) {
        LOG.debug("Request to get PatientProfile : {}", id);
        return patientProfileRepository.findOneWithEagerRelationships(id).map(patientProfileMapper::toDto);
    }

    /**
     * Delete the patientProfile by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete PatientProfile : {}", id);
        patientProfileRepository.deleteById(id);
    }
}
