package com.dtao.lms.service;

import com.dtao.lms.model.Comment;
import com.dtao.lms.model.TargetType;
import com.dtao.lms.repo.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private static final int MAX_LENGTH = 1000;

    @Autowired
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Comment createComment(TargetType targetType, String targetId, String email, String text) {
        if (text == null) throw new IllegalArgumentException("text required");
        String trimmed = text.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("text required");
        if (trimmed.length() > MAX_LENGTH) throw new IllegalArgumentException("text too long");
        // simple sanitize: strip control characters (frontend should escape on render)
        trimmed = trimmed.replaceAll("\\p{Cntrl}", "");
        Comment c = new Comment(targetType, targetId, email, trimmed);
        return commentRepository.save(c);
    }

    public List<Comment> listComments(TargetType targetType, String targetId, int page, int size) {
        PageRequest p = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return commentRepository.findByTargetTypeAndTargetIdAndDeletedOrderByCreatedAtDesc(targetType, targetId, false, p);
    }

    public long countComments(TargetType targetType, String targetId) {
        return commentRepository.countByTargetTypeAndTargetIdAndDeleted(targetType, targetId, false);
    }

    public Optional<Comment> editComment(String commentId, String email, String newText) {
        Optional<Comment> opt = commentRepository.findById(commentId);
        if (!opt.isPresent()) return Optional.empty();
        Comment c = opt.get();
        if (!c.getEmail().equals(email)) throw new SecurityException("only author can edit");
        if (c.isDeleted()) throw new IllegalStateException("comment deleted");
        String trimmed = newText == null ? "" : newText.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("text required");
        if (trimmed.length() > MAX_LENGTH) throw new IllegalArgumentException("text too long");
        trimmed = trimmed.replaceAll("\\p{Cntrl}", "");
        c.setText(trimmed);
        c.setEditedAt(java.time.Instant.now());
        commentRepository.save(c);
        return Optional.of(c);
    }

    public void deleteComment(String commentId, String email) {
        Optional<Comment> opt = commentRepository.findById(commentId);
        if (!opt.isPresent()) return;
        Comment c = opt.get();
        if (!c.getEmail().equals(email)) throw new SecurityException("only author can delete");
        c.setDeleted(true);
        commentRepository.save(c);
    }
}
