package com.ocbs.service;

import com.ocbs.domain.*; // for static metamodels
import com.ocbs.domain.DoctorProfile;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.service.criteria.DoctorProfileCriteria;
import com.ocbs.service.dto.DoctorProfileDTO;
import com.ocbs.service.mapper.DoctorProfileMapper;
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
 * Service for executing complex queries for {@link DoctorProfile} entities in the database.
 * The main input is a {@link DoctorProfileCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DoctorProfileDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DoctorProfileQueryService extends QueryService<DoctorProfile> {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorProfileQueryService.class);

    private final DoctorProfileRepository doctorProfileRepository;

    private final DoctorProfileMapper doctorProfileMapper;

    public DoctorProfileQueryService(DoctorProfileRepository doctorProfileRepository, DoctorProfileMapper doctorProfileMapper) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorProfileMapper = doctorProfileMapper;
    }

    /**
     * Return a {@link Page} of {@link DoctorProfileDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DoctorProfileDTO> findByCriteria(DoctorProfileCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<DoctorProfile> specification = createSpecification(criteria);
        return doctorProfileRepository
            .fetchBagRelationships(doctorProfileRepository.findAll(specification, page))
            .map(doctorProfileMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DoctorProfileCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<DoctorProfile> specification = createSpecification(criteria);
        return doctorProfileRepository.count(specification);
    }

    /**
     * Function to convert {@link DoctorProfileCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<DoctorProfile> createSpecification(DoctorProfileCriteria criteria) {
        Specification<DoctorProfile> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(DoctorProfile_.user, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), DoctorProfile_.id),
                    buildStringSpecification(criteria.getFullName(), DoctorProfile_.fullName),
                    buildStringSpecification(criteria.getTitle(), DoctorProfile_.title),
                    buildRangeSpecification(criteria.getExperienceYears(), DoctorProfile_.experienceYears),
                    buildRangeSpecification(criteria.getConsultationFee(), DoctorProfile_.consultationFee),
                    buildStringSpecification(criteria.getPhone(), DoctorProfile_.phone),
                    buildSpecification(criteria.getGender(), DoctorProfile_.gender),
                    buildSpecification(criteria.getIsActive(), DoctorProfile_.isActive),
                    buildSpecification(criteria.getUserId(), root -> root.join(DoctorProfile_.user, JoinType.LEFT).get(User_.id)),
                    buildSpecification(criteria.getSpecializationsId(), root ->
                        root.join(DoctorProfile_.specializationses, JoinType.LEFT).get(Specialization_.id)
                    )
                )
            );
        }
        return specification;
    }
}
