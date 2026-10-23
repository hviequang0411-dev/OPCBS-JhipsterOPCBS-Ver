package com.ocbs.repository;

import com.ocbs.domain.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ServicePackage entity in Booking Service.
 */
@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {}
