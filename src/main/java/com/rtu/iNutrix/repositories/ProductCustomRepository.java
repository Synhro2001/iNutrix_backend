package com.rtu.iNutrix.repositories;

import com.rtu.iNutrix.models.entities.ProductCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductCustomRepository extends JpaRepository<ProductCustom, UUID> {
    List<ProductCustom> findByUser_id(String userID);
}