package com.ocbs.repository;

import com.ocbs.domain.TreatmentPackage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TreatmentPackageRepository extends JpaRepository<TreatmentPackage, Long>, JpaSpecificationExecutor<TreatmentPackage> {

    @Query("select tp from TreatmentPackage tp left join fetch tp.doctor d left join fetch d.user left join fetch tp.patient p left join fetch p.user where tp.id = :id")
    Optional<TreatmentPackage> findOneWithEagerRelationships(@Param("id") Long id);

    @Query("select tp from TreatmentPackage tp left join fetch tp.doctor d left join fetch d.user left join fetch tp.patient p left join fetch p.user where tp.doctor.id = :doctorId order by tp.createdDate desc")
    List<TreatmentPackage> findAllByDoctorId(@Param("doctorId") Long doctorId);

    @Query("select tp from TreatmentPackage tp left join fetch tp.doctor d left join fetch d.user left join fetch tp.patient p left join fetch p.user where tp.patient.id = :patientId order by tp.createdDate desc")
    List<TreatmentPackage> findAllByPatientId(@Param("patientId") Long patientId);
}
