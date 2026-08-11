package com.todoapp.backend.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.todoapp.backend.category.Category;
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

    // Named `done` so Lombok's isDone()/setDone() pair up with it as ONE Jackson
    // property; @JsonProperty then renames that single property for the API.
    // Calling the field `isDone` instead makes Jackson emit both "done" and "isDone".
    @JsonProperty("isDone")
    @Column(name = "is_done")
    private boolean done;
}
