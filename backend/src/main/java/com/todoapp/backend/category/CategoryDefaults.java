package com.todoapp.backend.category;

import com.todoapp.backend.user.User;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CategoryDefaults {

    private record Preset(String name, String color) {}

    private static final List<Preset> PRESETS = List.of(
            new Preset("Work", "#3B82F6"),
            new Preset("Personal", "#10B981"),
            new Preset("Errands", "#F59E0B"));

    private final CategoryRepository repository;

    public CategoryDefaults(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> createFor(User owner) {
        List<Category> categories = PRESETS.stream()
                .map(preset -> {
                    Category category = new Category();
                    category.setName(preset.name());
                    category.setColor(preset.color());
                    category.setOwner(owner);
                    return category;
                })
                .toList();
        return repository.saveAll(categories);
    }
}
