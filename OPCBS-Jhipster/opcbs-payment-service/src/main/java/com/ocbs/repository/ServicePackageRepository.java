package com.ocbs.repository;

import com.ocbs.domain.ServicePackage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    List<ServicePackage> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}
