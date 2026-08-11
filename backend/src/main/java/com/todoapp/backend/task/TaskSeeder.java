package com.todoapp.backend.task;

import java.io.InputStream;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class TaskSeeder implements CommandLineRunner {

    private final TaskRepository repository;
    private final ObjectMapper objectMapper;

    public TaskSeeder(TaskRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() > 0) {
            return;
        }
        try (InputStream stream = new ClassPathResource("seed-tasks.json").getInputStream()) {
            List<Task> tasks = objectMapper.readValue(stream, new TypeReference<List<Task>>() {});
            repository.saveAll(tasks);
        }
    }
}
