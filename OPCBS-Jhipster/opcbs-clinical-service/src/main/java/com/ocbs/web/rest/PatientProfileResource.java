package com.ocbs.web.rest;

import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.service.PatientProfileService;
import com.ocbs.service.dto.PatientProfileDTO;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.ocbs.domain.PatientProfile}.
 */
@RestController
@RequestMapping("/api/patient-profiles")
public class PatientProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(PatientProfileResource.class);

    private static final String ENTITY_NAME = "patientProfile";

    @Value("${jhipster.clientApp.name:opcbs}")
    private String applicationName;

    private final PatientProfileService patientProfileService;

    private final PatientProfileRepository patientProfileRepository;

    public PatientProfileResource(PatientProfileService patientProfileService, PatientProfileRepository patientProfileRepository) {
        this.patientProfileService = patientProfileService;
        this.patientProfileRepository = patientProfileRepository;
    }

    /**
     * {@code POST  /patient-profiles} : Create a new patientProfile.
     *
     * @param patientProfileDTO the patientProfileDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new patientProfileDTO, or with status {@code 400 (Bad Request)} if the patientProfile already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<PatientProfileDTO> createPatientProfile(@Valid @RequestBody PatientProfileDTO patientProfileDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save PatientProfile : {}", patientProfileDTO);
        if (patientProfileDTO.getId() != null) {
            throw new BadRequestAlertException("A new patientProfile cannot already have an ID", ENTITY_NAME, "idexists");
        }
        patientProfileDTO = patientProfileService.save(patientProfileDTO);
        return ResponseEntity.created(new URI("/api/patient-profiles/" + patientProfileDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, patientProfileDTO.getId().toString()))
            .body(patientProfileDTO);
    }

    /**
     * {@code PUT  /patient-profiles/:id} : Updates an existing patientProfile.
     *
     * @param id the id of the patientProfileDTO to save.
     * @param patientProfileDTO the patientProfileDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated patientProfileDTO,
     * or with status {@code 400 (Bad Request)} if the patientProfileDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the patientProfileDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PatientProfileDTO> updatePatientProfile(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PatientProfileDTO patientProfileDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update PatientProfile : {}, {}", id, patientProfileDTO);
        if (patientProfileDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, patientProfileDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!patientProfileRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        patientProfileDTO = patientProfileService.update(patientProfileDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, patientProfileDTO.getId().toString()))
            .body(patientProfileDTO);
    }

    /**
     * {@code PATCH  /patient-profiles/:id} : Partial updates given fields of an existing patientProfile, field will ignore if it is null
     *
     * @param id the id of the patientProfileDTO to save.
     * @param patientProfileDTO the patientProfileDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated patientProfileDTO,
     * or with status {@code 400 (Bad Request)} if the patientProfileDTO is not valid,
     * or with status {@code 404 (Not Found)} if the patientProfileDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the patientProfileDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PatientProfileDTO> partialUpdatePatientProfile(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody PatientProfileDTO patientProfileDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update PatientProfile : {}, {}", id, patientProfileDTO);
        if (patientProfileDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, patientProfileDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!patientProfileRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PatientProfileDTO> result = patientProfileService.partialUpdate(patientProfileDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, patientProfileDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /patient-profiles} : get all the Patient Profiles.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Patient Profiles in body.
     */
    @GetMapping("")
    public List<PatientProfileDTO> getAllPatientProfiles(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all PatientProfiles");
        if (eagerload) {
            return patientProfileService.findAllWithEagerRelationships();
        } else {
            return patientProfileService.findAll();
        }
    }

    /**
     * {@code GET  /patient-profiles/:id} : get the "id" patientProfile.
     *
     * @param id the id of the patientProfileDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the patientProfileDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PatientProfileDTO> getPatientProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to get PatientProfile : {}", id);
        Optional<PatientProfileDTO> patientProfileDTO = patientProfileService.findOne(id);
        return ResponseUtil.wrapOrNotFound(patientProfileDTO);
    }

    /**
     * {@code DELETE  /patient-profiles/:id} : delete the "id" patientProfile.
     *
     * @param id the id of the patientProfileDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatientProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete PatientProfile : {}", id);
        patientProfileService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
