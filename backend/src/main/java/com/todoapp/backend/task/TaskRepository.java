package com.todoapp.backend.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {


    // SELECT CATEGORY.NAME
    // FROM TASK JOIN CATEGORY
    // ON TASK.ID = CATEGORY.ID
    // WHERE LOWERCASE(CATEGORY.NAME) = LOWERCASE(NAME)
    List<Task> findByCategoryNameIgnoreCase(String name);

    // SELECT COUNT(*)
    // FROM TASK 
    // WHERE TASK.CATEGORYID = CATEGORYID
    long countByCategoryId(Long categoryId);
}
