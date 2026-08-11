package com.todoapp.backend.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOwnerUsernameOrderByNameAsc(String username);

    
    Optional<Category> findByIdAndOwnerUsername(Long id, String username);

    boolean existsByOwnerUsernameAndNameIgnoreCase(String username, String name);

    boolean existsByOwnerId(Long ownerId);
}
