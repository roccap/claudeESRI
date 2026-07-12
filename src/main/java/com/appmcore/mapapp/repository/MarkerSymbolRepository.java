package com.appmcore.mapapp.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.appmcore.mapapp.entity.MarkerSymbol;

/**
 * Persistence for {@link MarkerSymbol} entities.
 */
@Repository
public interface MarkerSymbolRepository extends JpaRepository<MarkerSymbol, UUID> {
}
