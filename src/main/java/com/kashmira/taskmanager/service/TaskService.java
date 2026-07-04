package com.kashmira.taskmanager.service;

import com.kashmira.taskmanager.dto.TaskRequest;
import com.kashmira.taskmanager.exception.TaskNotFoundException;
import com.kashmira.taskmanager.model.Task;
import com.kashmira.taskmanager.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task createTask(TaskRequest request) {
        Task task = new Task();
        applyRequestToTask(request, task);
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, TaskRequest request) {
        Task task = getTaskById(id);
        applyRequestToTask(request, task);
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }

    private void applyRequestToTask(TaskRequest request, Task task) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        task.setDueDate(request.getDueDate());
    }
}
