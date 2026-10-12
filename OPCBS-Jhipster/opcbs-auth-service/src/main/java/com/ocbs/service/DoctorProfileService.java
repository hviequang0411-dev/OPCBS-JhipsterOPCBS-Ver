package com.ocbs.service;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.service.dto.DoctorProfileDTO;
import com.ocbs.service.mapper.DoctorProfileMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.ocbs.domain.DoctorProfile}.
 */
@Service
@Transactional
public class DoctorProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorProfileService.class);

    private final DoctorProfileRepository doctorProfileRepository;

    private final DoctorProfileMapper doctorProfileMapper;

    public DoctorProfileService(DoctorProfileRepository doctorProfileRepository, DoctorProfileMapper doctorProfileMapper) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorProfileMapper = doctorProfileMapper;
    }

    /**
     * Save a doctorProfile.
     *
     * @param doctorProfileDTO the entity to save.
     * @return the persisted entity.
     */
    public DoctorProfileDTO save(DoctorProfileDTO doctorProfileDTO) {
        LOG.debug("Request to save DoctorProfile : {}", doctorProfileDTO);
        DoctorProfile doctorProfile = doctorProfileMapper.toEntity(doctorProfileDTO);
        doctorProfile = doctorProfileRepository.save(doctorProfile);
        return doctorProfileMapper.toDto(doctorProfile);
    }

    /**
     * Update a doctorProfile.
     *
     * @param doctorProfileDTO the entity to save.
     * @return the persisted entity.
     */
    public DoctorProfileDTO update(DoctorProfileDTO doctorProfileDTO) {
        LOG.debug("Request to update DoctorProfile : {}", doctorProfileDTO);
        DoctorProfile doctorProfile = doctorProfileMapper.toEntity(doctorProfileDTO);
        doctorProfile = doctorProfileRepository.save(doctorProfile);
        return doctorProfileMapper.toDto(doctorProfile);
    }

    /**
     * Partially update a doctorProfile.
     *
     * @param doctorProfileDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DoctorProfileDTO> partialUpdate(DoctorProfileDTO doctorProfileDTO) {
        LOG.debug("Request to partially update DoctorProfile : {}", doctorProfileDTO);

        return doctorProfileRepository
            .findById(doctorProfileDTO.getId())
            .map(existingDoctorProfile -> {
                doctorProfileMapper.partialUpdate(existingDoctorProfile, doctorProfileDTO);

                return existingDoctorProfile;
            })
            .map(doctorProfileRepository::save)
            .map(doctorProfileMapper::toDto);
    }

    /**
     * Get all the doctorProfiles with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<DoctorProfileDTO> findAllWithEagerRelationships(Pageable pageable) {
        return doctorProfileRepository.findAllWithEagerRelationships(pageable).map(doctorProfileMapper::toDto);
    }

    /**
     * Get one doctorProfile by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DoctorProfileDTO> findOne(Long id) {
        LOG.debug("Request to get DoctorProfile : {}", id);
        return doctorProfileRepository.findOneWithEagerRelationships(id).map(doctorProfileMapper::toDto);
    }

    /**
     * Delete the doctorProfile by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete DoctorProfile : {}", id);
        doctorProfileRepository.deleteById(id);
    }
}
