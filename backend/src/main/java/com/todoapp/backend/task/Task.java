package com.todoapp.backend.task;

import com.todoapp.backend.category.Category;
import com.todoapp.backend.error.InvalidRequestException;
import com.todoapp.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "is_done")
    private boolean done;

    public void toggle() {
        this.done = !this.done;
    }

    public void rename(String newName) {
        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidRequestException("Task name must not be blank");
        }
        this.name = trimmed;
    }
}
