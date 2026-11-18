
package com.codepilot.springbootbasics.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // User assigned to complete this task
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private CodePilotUser assignee;

    // Optional deadline
    @Column(name = "due_date")
    private LocalDate dueDate;

    protected Task() {
    }

    public Task(
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            Project project
    ) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.project = project;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public Project getProject() {
        return project;
    }

    public CodePilotUser getAssignee() {
        return assignee;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setAssignee(CodePilotUser assignee) {
        this.assignee = assignee;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    // A task is overdue when its deadline has passed
    // and it has not been completed.
    @Transient
    public boolean isOverdue() {
        return dueDate != null
                && dueDate.isBefore(LocalDate.now())
                && status != TaskStatus.COMPLETED;
    }
}
