package com.castfan.repository;

import java.util.List;
import java.util.Optional;

import com.castfan.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

	Optional<Store> findBySlug(String slug);

	List<Store> findAllByOwnerIdOrderByNameAsc(Long ownerId);

	boolean existsBySlug(String slug);
}
