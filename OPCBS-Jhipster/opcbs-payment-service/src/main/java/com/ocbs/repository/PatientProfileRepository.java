package com.ocbs.repository;

import com.ocbs.domain.PatientProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PatientProfile entity.
 */
@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    default Optional<PatientProfile> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<PatientProfile> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<PatientProfile> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select patientProfile from PatientProfile patientProfile left join fetch patientProfile.user",
        countQuery = "select count(patientProfile) from PatientProfile patientProfile"
    )
    Page<PatientProfile> findAllWithToOneRelationships(Pageable pageable);

    @Query("select patientProfile from PatientProfile patientProfile left join fetch patientProfile.user")
    List<PatientProfile> findAllWithToOneRelationships();

    @Query("select patientProfile from PatientProfile patientProfile left join fetch patientProfile.user where patientProfile.id =:id")
    Optional<PatientProfile> findOneWithToOneRelationships(@Param("id") Long id);

    @Query("select patientProfile from PatientProfile patientProfile left join fetch patientProfile.user where patientProfile.user.login =:login")
    Optional<PatientProfile> findByUser_Login(@Param("login") String login);

    Optional<PatientProfile> findByUser(com.ocbs.domain.User user);
}
