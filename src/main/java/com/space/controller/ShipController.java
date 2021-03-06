package com.space.controller;

import com.space.model.ShipType;
import com.space.model.entity.Ship;
import com.space.service.ShipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/rest")
public class ShipController {
    private static final Logger log = LoggerFactory.getLogger(ShipController.class);
    Date dateFrom2800Year = new Date(26194914000000L);
    Date dateTo3019Year = new Date(33126786000000L);

    @Autowired
    ShipService shipService;

    @GetMapping(path = "/ships", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<Ship> showAll(@RequestParam(value = "name", required = false) String name,
                              @RequestParam(value = "planet", required = false) String planet,
                              @RequestParam(value = "shipType", required = false) ShipType shipType,
                              @RequestParam(value = "after", required = false) Long after,
                              @RequestParam(value = "before", required = false) Long before,
                              @RequestParam(value = "isUsed", required = false) Boolean isUsed,
                              @RequestParam(value = "minSpeed", required = false) Double minSpeed,
                              @RequestParam(value = "maxSpeed", required = false) Double maxSpeed,
                              @RequestParam(value = "minCrewSize", required = false) Integer minCrewSize,
                              @RequestParam(value = "maxCrewSize", required = false) Integer maxCrewSize,
                              @RequestParam(value = "minRating", required = false) Double minRating,
                              @RequestParam(value = "maxRating", required = false) Double maxRating,
                              @RequestParam(value = "order", required = false, defaultValue = "ID") ShipOrder order,
                              @RequestParam(value = "pageNumber", required = false, defaultValue = "0") Integer pageNumber,
                              @RequestParam(value = "pageSize", required = false, defaultValue = "3") Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(order.getFieldName()));

        return shipService.listAll(
                Specification.where(shipService.sortByName(name)
                        .and(shipService.sortByPlanet(planet)))
                        .and(shipService.sortByShipType(shipType))
                        .and(shipService.sortByProdDate(after, before))
                        .and(shipService.sortByUsed(isUsed))
                        .and(shipService.sortBySpeed(minSpeed, maxSpeed))
                        .and(shipService.sortByCrewSize(minCrewSize, maxCrewSize))
                        .and(shipService.sortByRating(minRating, maxRating)), pageable)
                .getContent();

    }

    @GetMapping(path = "/ships/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Integer count(@RequestParam(value = "name", required = false) String name,
                         @RequestParam(value = "planet", required = false) String planet,
                         @RequestParam(value = "shipType", required = false) ShipType shipType,
                         @RequestParam(value = "after", required = false) Long after,
                         @RequestParam(value = "before", required = false) Long before,
                         @RequestParam(value = "isUsed", required = false) Boolean isUsed,
                         @RequestParam(value = "minSpeed", required = false) Double minSpeed,
                         @RequestParam(value = "maxSpeed", required = false) Double maxSpeed,
                         @RequestParam(value = "minCrewSize", required = false) Integer minCrewSize,
                         @RequestParam(value = "maxCrewSize", required = false) Integer maxCrewSize,
                         @RequestParam(value = "minRating", required = false) Double minRating,
                         @RequestParam(value = "maxRating", required = false) Double maxRating) {

        return shipService.сount(
                Specification.where(shipService.sortByName(name)
                        .and(shipService.sortByPlanet(planet)))
                        .and(shipService.sortByShipType(shipType))
                        .and(shipService.sortByProdDate(after, before))
                        .and(shipService.sortByUsed(isUsed))
                        .and(shipService.sortBySpeed(minSpeed, maxSpeed))
                        .and(shipService.sortByCrewSize(minCrewSize, maxCrewSize))
                        .and(shipService.sortByRating(minRating, maxRating)));
    }

    @GetMapping(path = "ships/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Ship findById(@PathVariable("id") Long id) {
        if (id == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        try {
            Optional<Ship> shipResponse = shipService.findById(id);
            return shipResponse.get();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found.", e);
        }
    }

    @PostMapping(path = "/ships", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Ship createShip(@RequestBody Ship ship) {
        try {
            if (ship.getName().length() > 50 || ship.getPlanet().length() > 50 || ship.getSpeed() == null || ship.getName().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            if (ship.getCrewSize() > 9999) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            if (ship.getProdDate().getTime() < dateFrom2800Year.getTime() || ship.getProdDate().getTime() > dateTo3019Year.getTime()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            if (ship.getUsed() == null) {
                ship.setUsed(false);
            }
            ship.setSpeed(Ship.roundDoubleTo100(ship.getSpeed()));
            ship.setRating(ship.countRating());
            return shipService.save(ship);
        } catch (NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping(path = "ships/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Ship updateShip(@RequestBody(required = false) Ship ship, @PathVariable("id") Long id) {

        try {
            if (id == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            if (shipService.isExistById(id)) {
                log.debug("UPDATE request to ship {}", id);

                Ship updatedShip = shipService.findById(id).get();

                if (ship.getName() != null && ship.getName().length() <= 50) {
                    if (ship.getName().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                    }
                    updatedShip.setName(ship.getName());
                }
                if (ship.getPlanet() != null && ship.getPlanet().length() <= 50) {
                    updatedShip.setPlanet(ship.getPlanet());
                }
                if (ship.getShipType() != null) {
                    updatedShip.setShipType(ship.getShipType());
                }
                if (ship.getProdDate() != null) {
                    if (ship.getProdDate().getTime() < dateFrom2800Year.getTime() || ship.getProdDate().getTime() > dateTo3019Year.getTime()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                    }
                    updatedShip.setProdDate(ship.getProdDate());
                }
                if (ship.getUsed() != null) {
                    updatedShip.setUsed(ship.getUsed());
                }
                if (ship.getSpeed() != null) {
                    updatedShip.setSpeed(Ship.roundDoubleTo100(ship.getSpeed()));
                }
                if (ship.getCrewSize() != null) {
                    if (ship.getCrewSize() < 1 || ship.getCrewSize() > 9999) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                    }
                    updatedShip.setCrewSize(ship.getCrewSize());
                }
                updatedShip.setRating(updatedShip.countRating());
                return shipService.save(updatedShip);
            } else {
                log.error("Ship {} not found.", id);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found.");
            }
        } catch (NullPointerException e) {
            log.debug("Id is null.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "ships/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteById(@PathVariable("id") Long id) {
        if (id == 0) {
            log.debug("Id = 0. Incorrect Id.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id = 0.");
        }
        try {
            log.debug("DELETE request to ship {}", id);
            try {
                shipService.deleteById(id);
                log.debug("Ship {} is deleted.", id);
            } catch (Exception e) {
                log.error("Ship {} not found.", id);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found.", e);
            }
        } catch (NullPointerException e) {
            log.debug("Id is null.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}