package com.taskflow.api.task.controller;

import com.taskflow.api.task.dto.CommentResponseDto;
import com.taskflow.api.task.dto.CreateCommentRequestDto;
import com.taskflow.api.task.service.TaskCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateCommentRequestDto request,
            Principal principal
    ) {
        String currentUserEmail = principal.getName();
        CommentResponseDto response = commentService.createComment(taskId, request, currentUserEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable UUID taskId,
            Principal principal
    ) {
        String currentUserEmail = principal.getName();
        List<CommentResponseDto> comments = commentService.getCommentsForTask(taskId, currentUserEmail);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            Principal principal
    ) {
        String currentUserEmail = principal.getName();
        commentService.deleteComment(commentId, currentUserEmail);
        return ResponseEntity.noContent().build();
    }
}
