package com.ocbs.repository;

import com.ocbs.domain.DoctorProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DoctorProfile entity.
 *
 * When extending this class, extend DoctorProfileRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface DoctorProfileRepository
    extends DoctorProfileRepositoryWithBagRelationships, JpaRepository<DoctorProfile, Long>, JpaSpecificationExecutor<DoctorProfile>
{
    default Optional<DoctorProfile> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<DoctorProfile> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<DoctorProfile> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select doctorProfile from DoctorProfile doctorProfile left join fetch doctorProfile.user",
        countQuery = "select count(doctorProfile) from DoctorProfile doctorProfile"
    )
    Page<DoctorProfile> findAllWithToOneRelationships(Pageable pageable);

    @Query("select doctorProfile from DoctorProfile doctorProfile left join fetch doctorProfile.user")
    List<DoctorProfile> findAllWithToOneRelationships();

    @Query("select doctorProfile from DoctorProfile doctorProfile left join fetch doctorProfile.user where doctorProfile.id =:id")
    Optional<DoctorProfile> findOneWithToOneRelationships(@Param("id") Long id);

    @Query("select doctorProfile from DoctorProfile doctorProfile left join fetch doctorProfile.user where doctorProfile.user.login =:login")
    Optional<DoctorProfile> findOneWithToOneRelationshipsByLogin(@Param("login") String login);

    default Optional<DoctorProfile> findByUser_Login(String login) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationshipsByLogin(login));
    }

    Optional<DoctorProfile> findByUser(com.ocbs.domain.User user);
}
