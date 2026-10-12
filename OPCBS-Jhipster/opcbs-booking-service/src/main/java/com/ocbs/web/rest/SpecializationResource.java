package com.ocbs.web.rest;

import com.ocbs.repository.SpecializationRepository;
import com.ocbs.service.SpecializationService;
import com.ocbs.service.dto.SpecializationDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.ocbs.domain.Specialization}.
 */
@RestController
@RequestMapping("/api/specializations")
public class SpecializationResource {

    private static final Logger LOG = LoggerFactory.getLogger(SpecializationResource.class);

    private static final String ENTITY_NAME = "specialization";

    @Value("${jhipster.clientApp.name:opcbs}")
    private String applicationName;

    private final SpecializationService specializationService;

    private final SpecializationRepository specializationRepository;

    public SpecializationResource(SpecializationService specializationService, SpecializationRepository specializationRepository) {
        this.specializationService = specializationService;
        this.specializationRepository = specializationRepository;
    }

    /**
     * {@code POST  /specializations} : Create a new specialization.
     *
     * @param specializationDTO the specializationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new specializationDTO, or with status {@code 400 (Bad Request)} if the specialization already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SpecializationDTO> createSpecialization(@Valid @RequestBody SpecializationDTO specializationDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save Specialization : {}", specializationDTO);
        if (specializationDTO.getId() != null) {
            throw new BadRequestAlertException("A new specialization cannot already have an ID", ENTITY_NAME, "idexists");
        }
        specializationDTO = specializationService.save(specializationDTO);
        return ResponseEntity.created(new URI("/api/specializations/" + specializationDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, specializationDTO.getId().toString()))
            .body(specializationDTO);
    }

    /**
     * {@code PUT  /specializations/:id} : Updates an existing specialization.
     *
     * @param id the id of the specializationDTO to save.
     * @param specializationDTO the specializationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated specializationDTO,
     * or with status {@code 400 (Bad Request)} if the specializationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the specializationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SpecializationDTO> updateSpecialization(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody SpecializationDTO specializationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Specialization : {}, {}", id, specializationDTO);
        if (specializationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, specializationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!specializationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        specializationDTO = specializationService.update(specializationDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, specializationDTO.getId().toString()))
            .body(specializationDTO);
    }

    /**
     * {@code PATCH  /specializations/:id} : Partial updates given fields of an existing specialization, field will ignore if it is null
     *
     * @param id the id of the specializationDTO to save.
     * @param specializationDTO the specializationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated specializationDTO,
     * or with status {@code 400 (Bad Request)} if the specializationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the specializationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the specializationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SpecializationDTO> partialUpdateSpecialization(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody SpecializationDTO specializationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Specialization : {}, {}", id, specializationDTO);
        if (specializationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, specializationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!specializationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SpecializationDTO> result = specializationService.partialUpdate(specializationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, specializationDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /specializations} : get all the Specializations.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Specializations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<SpecializationDTO>> getAllSpecializations(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of Specializations");
        Page<SpecializationDTO> page = specializationService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /specializations/:id} : get the "id" specialization.
     *
     * @param id the id of the specializationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the specializationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SpecializationDTO> getSpecialization(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Specialization : {}", id);
        Optional<SpecializationDTO> specializationDTO = specializationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(specializationDTO);
    }

    /**
     * {@code DELETE  /specializations/:id} : delete the "id" specialization.
     *
     * @param id the id of the specializationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpecialization(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Specialization : {}", id);
        specializationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
