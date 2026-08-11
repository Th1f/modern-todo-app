package com.todoapp.backend.seed;

import com.todoapp.backend.category.Category;
import com.todoapp.backend.category.CategoryDefaults;
import com.todoapp.backend.category.CategoryRepository;
import com.todoapp.backend.task.Task;
import com.todoapp.backend.task.TaskRepository;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class DataSeeder implements CommandLineRunner {

   
    private record SeedTask(String name, String category, boolean isDone) {}

    private final CategoryRepository categoryRepository;
    private final CategoryDefaults categoryDefaults;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataSeeder(
            CategoryRepository categoryRepository,
            CategoryDefaults categoryDefaults,
            TaskRepository taskRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryDefaults = categoryDefaults;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        User demo = findOrCreateDemoUser();
        backfillMissingCategories();
        seedDemoTasks(demo);
    }

    private User findOrCreateDemoUser() {
        return userRepository.findByUsername("demo").orElseGet(() -> {
            User user = new User();
            user.setUsername("demo");
            user.setPassword(passwordEncoder.encode("demopassword"));
            return userRepository.save(user);
        });
    }

    private void backfillMissingCategories() {
        userRepository.findAll().stream()
                .filter(user -> !categoryRepository.existsByOwnerId(user.getId()))
                .forEach(categoryDefaults::createFor);
    }

    private void seedDemoTasks(User demo) throws Exception {
        if (taskRepository.count() > 0) {
            return;
        }

        Map<String, Category> byName =
                categoryRepository.findByOwnerUsernameOrderByNameAsc(demo.getUsername()).stream()
                        .collect(Collectors.toMap(Category::getName, Function.identity()));

        List<Task> tasks = read("seed-tasks.json", new TypeReference<List<SeedTask>>() {}).stream()
                .map(seed -> {
                    Task task = new Task();
                    task.setName(seed.name());
                    task.setCategory(byName.get(seed.category()));
                    task.setDone(seed.isDone());
                    task.setOwner(demo);
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
