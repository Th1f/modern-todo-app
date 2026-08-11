package com.todoapp.backend.seed;

import com.todoapp.backend.category.Category;
import com.todoapp.backend.category.CategoryRepository;
import com.todoapp.backend.task.Task;
import com.todoapp.backend.task.TaskRepository;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class DataSeeder implements CommandLineRunner {
    private record SeedTask(String name, String category, boolean isDone) {}

    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public DataSeeder(
            CategoryRepository categoryRepository,
            TaskRepository taskRepository,
            ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(read("seed-categories.json", new TypeReference<List<Category>>() {}));
        }

        if (taskRepository.count() > 0) {
            return;
        }

        Map<String, Category> byName = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName, Function.identity()));

        List<Task> tasks = read("seed-tasks.json", new TypeReference<List<SeedTask>>() {}).stream()
                .map(seed -> {
                    Task task = new Task();
                    task.setName(seed.name());
                    task.setCategory(byName.get(seed.category()));
                    task.setDone(seed.isDone());
                    return task;
                })
                .toList();

        taskRepository.saveAll(tasks);
    }

    private <T> T read(String resource, TypeReference<T> type) throws Exception {
        try (InputStream stream = new ClassPathResource(resource).getInputStream()) {
            return objectMapper.readValue(stream, type);
        }
    }
}
