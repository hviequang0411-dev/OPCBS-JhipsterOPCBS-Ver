package com.ocbs.service;

import com.ocbs.domain.AvailabilitySlot;
import com.ocbs.repository.AvailabilitySlotRepository;
import com.ocbs.service.dto.AvailabilitySlotDTO;
import com.ocbs.service.mapper.AvailabilitySlotMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.ocbs.domain.AvailabilitySlot}.
 */
@Service
@Transactional
public class AvailabilitySlotService {

    private static final Logger LOG = LoggerFactory.getLogger(AvailabilitySlotService.class);

    private final AvailabilitySlotRepository availabilitySlotRepository;

    private final AvailabilitySlotMapper availabilitySlotMapper;

    public AvailabilitySlotService(AvailabilitySlotRepository availabilitySlotRepository, AvailabilitySlotMapper availabilitySlotMapper) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.availabilitySlotMapper = availabilitySlotMapper;
    }

    /**
     * Save a availabilitySlot.
     *
     * @param availabilitySlotDTO the entity to save.
     * @return the persisted entity.
     */
    public AvailabilitySlotDTO save(AvailabilitySlotDTO availabilitySlotDTO) {
        LOG.debug("Request to save AvailabilitySlot : {}", availabilitySlotDTO);
        AvailabilitySlot availabilitySlot = availabilitySlotMapper.toEntity(availabilitySlotDTO);
        availabilitySlot = availabilitySlotRepository.save(availabilitySlot);
        return availabilitySlotMapper.toDto(availabilitySlot);
    }

    /**
     * Update a availabilitySlot.
     *
     * @param availabilitySlotDTO the entity to save.
     * @return the persisted entity.
     */
    public AvailabilitySlotDTO update(AvailabilitySlotDTO availabilitySlotDTO) {
        LOG.debug("Request to update AvailabilitySlot : {}", availabilitySlotDTO);
        AvailabilitySlot availabilitySlot = availabilitySlotMapper.toEntity(availabilitySlotDTO);
        availabilitySlot = availabilitySlotRepository.save(availabilitySlot);
        return availabilitySlotMapper.toDto(availabilitySlot);
    }

    /**
     * Partially update a availabilitySlot.
     *
     * @param availabilitySlotDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AvailabilitySlotDTO> partialUpdate(AvailabilitySlotDTO availabilitySlotDTO) {
        LOG.debug("Request to partially update AvailabilitySlot : {}", availabilitySlotDTO);

        return availabilitySlotRepository
            .findById(availabilitySlotDTO.getId())
            .map(existingAvailabilitySlot -> {
                availabilitySlotMapper.partialUpdate(existingAvailabilitySlot, availabilitySlotDTO);

                return existingAvailabilitySlot;
            })
            .map(availabilitySlotRepository::save)
            .map(availabilitySlotMapper::toDto);
    }

    /**
     * Get all the availabilitySlots with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<AvailabilitySlotDTO> findAllWithEagerRelationships(Pageable pageable) {
        return availabilitySlotRepository.findAllWithEagerRelationships(pageable).map(availabilitySlotMapper::toDto);
    }

    /**
     * Get one availabilitySlot by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AvailabilitySlotDTO> findOne(Long id) {
        LOG.debug("Request to get AvailabilitySlot : {}", id);
        return availabilitySlotRepository.findOneWithEagerRelationships(id).map(availabilitySlotMapper::toDto);
    }

    /**
     * Delete the availabilitySlot by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete AvailabilitySlot : {}", id);
        availabilitySlotRepository.deleteById(id);
    }
}
