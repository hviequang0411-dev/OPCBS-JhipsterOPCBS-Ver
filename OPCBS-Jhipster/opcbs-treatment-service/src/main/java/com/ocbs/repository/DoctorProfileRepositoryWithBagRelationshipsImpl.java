package com.ocbs.repository;

import com.ocbs.domain.DoctorProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class DoctorProfileRepositoryWithBagRelationshipsImpl implements DoctorProfileRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String DOCTORPROFILES_PARAMETER = "doctorProfiles";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<DoctorProfile> fetchBagRelationships(Optional<DoctorProfile> doctorProfile) {
        return doctorProfile.map(this::fetchSpecializationses);
    }

    @Override
    public Page<DoctorProfile> fetchBagRelationships(Page<DoctorProfile> doctorProfiles) {
        return new PageImpl<>(
            fetchBagRelationships(doctorProfiles.getContent()),
            doctorProfiles.getPageable(),
            doctorProfiles.getTotalElements()
        );
    }

    @Override
    public List<DoctorProfile> fetchBagRelationships(List<DoctorProfile> doctorProfiles) {
        return Optional.of(doctorProfiles).map(this::fetchSpecializationses).orElse(List.of());
    }

    DoctorProfile fetchSpecializationses(DoctorProfile result) {
        return entityManager
            .createQuery(
                "select doctorProfile from DoctorProfile doctorProfile left join fetch doctorProfile.specializationses where doctorProfile.id = :id",
                DoctorProfile.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<DoctorProfile> fetchSpecializationses(List<DoctorProfile> doctorProfiles) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, doctorProfiles.size()).forEach(index -> order.put(doctorProfiles.get(index).getId(), index));
        List<DoctorProfile> result = entityManager
            .createQuery(
                "select doctorProfile from DoctorProfile doctorProfile left join fetch doctorProfile.specializationses where doctorProfile in :doctorProfiles",
                DoctorProfile.class
            )
            .setParameter(DOCTORPROFILES_PARAMETER, doctorProfiles)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
