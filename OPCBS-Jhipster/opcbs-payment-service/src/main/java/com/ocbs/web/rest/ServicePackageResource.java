package com.ocbs.web.rest;

import com.ocbs.domain.ServicePackage;
import com.ocbs.repository.ServicePackageRepository;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Transactional
public class ServicePackageResource {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePackageResource.class);
    private static final String ENTITY_NAME = "servicePackage";

    private final ServicePackageRepository servicePackageRepository;

    public ServicePackageResource(ServicePackageRepository servicePackageRepository) {
        this.servicePackageRepository = servicePackageRepository;
    }

    /**
     * {@code GET /service-packages} : Get all active platform service packages for doctors.
     */
    @GetMapping("/service-packages")
    public ResponseEntity<List<ServicePackage>> getAllActiveServicePackages() {
        List<ServicePackage> list = servicePackageRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
        return ResponseEntity.ok(list);
    }

    /**
     * {@code GET /service-packages/:id} : Get service package details.
     */
    @GetMapping("/service-packages/{id}")
    public ResponseEntity<ServicePackage> getServicePackage(@PathVariable("id") Long id) {
        ServicePackage pkg = servicePackageRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Service package not found", ENTITY_NAME, "idnotfound"));
        return ResponseEntity.ok(pkg);
    }

    /**
     * {@code POST /service-packages} : Admin creates a new service package tier.
     */
    @PostMapping("/service-packages")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ServicePackage> createServicePackage(@Valid @RequestBody ServicePackage servicePackage) throws URISyntaxException {
        if (servicePackage.getId() != null) {
            throw new BadRequestAlertException("A new service package cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ServicePackage result = servicePackageRepository.save(servicePackage);
        return ResponseEntity.created(new URI("/api/service-packages/" + result.getId())).body(result);
    }
}
