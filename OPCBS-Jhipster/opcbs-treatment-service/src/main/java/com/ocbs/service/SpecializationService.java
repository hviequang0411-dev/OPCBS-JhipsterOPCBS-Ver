package com.ocbs.service;

import com.ocbs.domain.Specialization;
import com.ocbs.repository.SpecializationRepository;
import com.ocbs.service.dto.SpecializationDTO;
import com.ocbs.service.mapper.SpecializationMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.ocbs.domain.Specialization}.
 */
@Service
@Transactional
public class SpecializationService {

    private static final Logger LOG = LoggerFactory.getLogger(SpecializationService.class);

    private final SpecializationRepository specializationRepository;

    private final SpecializationMapper specializationMapper;

    public SpecializationService(SpecializationRepository specializationRepository, SpecializationMapper specializationMapper) {
        this.specializationRepository = specializationRepository;
        this.specializationMapper = specializationMapper;
    }

    /**
     * Save a specialization.
     *
     * @param specializationDTO the entity to save.
     * @return the persisted entity.
     */
    public SpecializationDTO save(SpecializationDTO specializationDTO) {
        LOG.debug("Request to save Specialization : {}", specializationDTO);
        Specialization specialization = specializationMapper.toEntity(specializationDTO);
        specialization = specializationRepository.save(specialization);
        return specializationMapper.toDto(specialization);
    }

    /**
     * Update a specialization.
     *
     * @param specializationDTO the entity to save.
     * @return the persisted entity.
     */
    public SpecializationDTO update(SpecializationDTO specializationDTO) {
        LOG.debug("Request to update Specialization : {}", specializationDTO);
        Specialization specialization = specializationMapper.toEntity(specializationDTO);
        specialization = specializationRepository.save(specialization);
        return specializationMapper.toDto(specialization);
    }

    /**
     * Partially update a specialization.
     *
     * @param specializationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<SpecializationDTO> partialUpdate(SpecializationDTO specializationDTO) {
        LOG.debug("Request to partially update Specialization : {}", specializationDTO);

        return specializationRepository
            .findById(specializationDTO.getId())
            .map(existingSpecialization -> {
                specializationMapper.partialUpdate(existingSpecialization, specializationDTO);

                return existingSpecialization;
            })
            .map(specializationRepository::save)
            .map(specializationMapper::toDto);
    }

    /**
     * Get all the specializations.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<SpecializationDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Specializations");
        return specializationRepository.findAll(pageable).map(specializationMapper::toDto);
    }

    /**
     * Get one specialization by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<SpecializationDTO> findOne(Long id) {
        LOG.debug("Request to get Specialization : {}", id);
        return specializationRepository.findById(id).map(specializationMapper::toDto);
    }

    /**
     * Delete the specialization by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Specialization : {}", id);
        specializationRepository.deleteById(id);
    }
}
