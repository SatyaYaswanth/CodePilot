
package com.codepilot.springbootbasics.dto;

import java.time.LocalDate;

public record UpdateTaskDueDateRequest(
        LocalDate dueDate
) {
}
