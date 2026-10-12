package com.ocbs.service;

import com.ocbs.domain.*; // for static metamodels
import com.ocbs.domain.AvailabilitySlot;
import com.ocbs.repository.AvailabilitySlotRepository;
import com.ocbs.service.criteria.AvailabilitySlotCriteria;
import com.ocbs.service.dto.AvailabilitySlotDTO;
import com.ocbs.service.mapper.AvailabilitySlotMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link AvailabilitySlot} entities in the database.
 * The main input is a {@link AvailabilitySlotCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link AvailabilitySlotDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class AvailabilitySlotQueryService extends QueryService<AvailabilitySlot> {

    private static final Logger LOG = LoggerFactory.getLogger(AvailabilitySlotQueryService.class);

    private final AvailabilitySlotRepository availabilitySlotRepository;

    private final AvailabilitySlotMapper availabilitySlotMapper;

    public AvailabilitySlotQueryService(
        AvailabilitySlotRepository availabilitySlotRepository,
        AvailabilitySlotMapper availabilitySlotMapper
    ) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.availabilitySlotMapper = availabilitySlotMapper;
    }

    /**
     * Return a {@link Page} of {@link AvailabilitySlotDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<AvailabilitySlotDTO> findByCriteria(AvailabilitySlotCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<AvailabilitySlot> specification = createSpecification(criteria);
        return availabilitySlotRepository.findAll(specification, page).map(availabilitySlotMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(AvailabilitySlotCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<AvailabilitySlot> specification = createSpecification(criteria);
        return availabilitySlotRepository.count(specification);
    }

    /**
     * Function to convert {@link AvailabilitySlotCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<AvailabilitySlot> createSpecification(AvailabilitySlotCriteria criteria) {
        Specification<AvailabilitySlot> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(AvailabilitySlot_.doctor, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), AvailabilitySlot_.id),
                    buildRangeSpecification(criteria.getStartTime(), AvailabilitySlot_.startTime),
                    buildRangeSpecification(criteria.getEndTime(), AvailabilitySlot_.endTime),
                    buildSpecification(criteria.getConsultationMode(), AvailabilitySlot_.consultationMode),
                    buildSpecification(criteria.getStatus(), AvailabilitySlot_.status),
                    buildStringSpecification(criteria.getNotes(), AvailabilitySlot_.notes),
                    buildSpecification(criteria.getDoctorId(), root ->
                        root.join(AvailabilitySlot_.doctor, JoinType.LEFT).get(DoctorProfile_.id)
                    )
                )
            );
        }
        return specification;
    }
}
