package com.todoapp.backend.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {


    // SELECT CATEGORY.NAME
    // FROM TASK JOIN CATEGORY
    // ON TASK.ID = CATEGORY.ID
    // WHERE LOWERCASE(CATEGORY.NAME) = LOWERCASE(NAME)
    List<Task> findByCategoryNameIgnoreCase(String name);

    List<Task> findByOwnerUsername(String username);

    Optional<Task> findByIdAndOwnerUsername(Long id, String username);

    List<Task> findByOwnerUsernameAndCategoryNameIgnoreCase(String username, String categoryName);


    // SELECT COUNT(*)
    // FROM TASK 
    // WHERE TASK.CATEGORYID = CATEGORYID
    long countByCategoryId(Long categoryId);
}
