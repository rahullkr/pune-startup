package com.punestartup.geography.controller;

import com.punestartup.geography.dto.MapMarkerResponse;
import com.punestartup.geography.dto.NearbyCompanyResponse;
import com.punestartup.geography.service.GeographyService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
public class GeographyController {

    private final GeographyService geographyService;

    public GeographyController(GeographyService geographyService) {
        this.geographyService = geographyService;
    }

    @GetMapping("/map")
    public List<MapMarkerResponse> getMapMarkers(@RequestParam double minLat,
                                                 @RequestParam double maxLat,
                                                 @RequestParam double minLng,
                                                 @RequestParam double maxLng) {
        return geographyService.getMapMarkers(minLat, maxLat, minLng, maxLng);
    }

    @GetMapping("/nearby")
    public List<NearbyCompanyResponse> getNearbyCompanies(@RequestParam double lat,
                                                           @RequestParam double lng,
                                                           @RequestParam double radiusKm,
                                                           @RequestParam(required = false) Integer limit) {
        return geographyService.getNearbyCompanies(lat, lng, radiusKm, limit);
    }
}
