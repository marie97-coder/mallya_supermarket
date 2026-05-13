package com.example.mallya_supermarket.repository;

import com.example.mallya_supermarket.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT c FROM Category c WHERE SIZE(c.products) > 0")
    List<Category> findCategoriesWithProducts();
    
    @Query("SELECT COUNT(c) FROM Category c WHERE SIZE(c.products) > 0")
    Long countCategoriesWithProducts();
}
