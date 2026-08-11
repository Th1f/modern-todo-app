package com.todoapp.backend.category;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {


    // SELECT 1 
    // FROM CATEGORY
    // WHERE LOWERCASE(CATEGORY.NAME) = LOWERCASE(NAME)
    boolean existsByNameIgnoreCase(String name);


    // SELECT CATEGORY.NAME
    // FROM CATEGORY
    // WHERE LOWERCASE(NAME) = LOWECASE(NAME)
    Optional<Category> findByNameIgnoreCase(String name);
}
