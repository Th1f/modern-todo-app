package com.todoapp.backend.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.todoapp.backend.error.InvalidRequestException;

/** Rules a task enforces about itself, with no repository in sight. */
class TaskTest {

    private Task task(String name) {
        Task task = new Task();
        task.setName(name);
        return task;
    }

    @Test
    void toggleFlipsInBothDirections() {
        Task task = task("Buy milk");

        task.toggle();
        assertThat(task.isDone()).isTrue();

        task.toggle();
        assertThat(task.isDone()).isFalse();
    }

    @Test
    void renameTrimsSurroundingWhitespace() {
        Task task = task("Buy milk");

        task.rename("  Buy oat milk  ");

        assertThat(task.getName()).isEqualTo("Buy oat milk");
    }

    @Test
    void renameRejectsAWhitespaceOnlyName() {
        Task task = task("Buy milk");

        assertThatThrownBy(() -> task.rename("   "))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(task.getName()).isEqualTo("Buy milk");
    }

    @Test
    void renameRejectsNull() {
        Task task = task("Buy milk");

        assertThatThrownBy(() -> task.rename(null))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(task.getName()).isEqualTo("Buy milk");
    }
}
