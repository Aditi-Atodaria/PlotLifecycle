package com.realestate.repository;

import com.realestate.model.Site;
import com.realestate.model.SiteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Long> {
    List<Site> findByStatus(SiteStatus status);
}
