package com.dtao.lms.service;

import com.dtao.lms.dto.ActionResponse;
import com.dtao.lms.model.LikeRecord;
import com.dtao.lms.model.LikeType;
import com.dtao.lms.model.TargetType;
import com.dtao.lms.repo.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;

    @Autowired
    public LikeService(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    /**
     * Toggle a LIKE for the given user & target. Returns updated counts and userState.
     */
    @Transactional
    public ActionResponse toggleLike(TargetType targetType, String targetId, String email) {
        LikeRecord existing = likeRepository.findByTargetTypeAndTargetIdAndEmail(targetType, targetId, email).orElse(null);
        if (existing == null) {
            LikeRecord r = new LikeRecord(targetType, targetId, email, LikeType.LIKE);
            likeRepository.save(r);
        } else if (existing.getType() == LikeType.LIKE) {
            // already liked -> remove (unlike)
            likeRepository.deleteByTargetTypeAndTargetIdAndEmail(targetType, targetId, email);
        } else {
            // was DISLIKE -> switch to LIKE
            existing.setType(LikeType.LIKE);
            likeRepository.save(existing);
        }
        return buildResponse(targetType, targetId, email);
    }

    @Transactional
    public ActionResponse toggleDislike(TargetType targetType, String targetId, String email) {
        LikeRecord existing = likeRepository.findByTargetTypeAndTargetIdAndEmail(targetType, targetId, email).orElse(null);
        if (existing == null) {
            LikeRecord r = new LikeRecord(targetType, targetId, email, LikeType.DISLIKE);
            likeRepository.save(r);
        } else if (existing.getType() == LikeType.DISLIKE) {
            // already disliked -> remove (undislike)
            likeRepository.deleteByTargetTypeAndTargetIdAndEmail(targetType, targetId, email);
        } else {
            // was LIKE -> switch to DISLIKE
            existing.setType(LikeType.DISLIKE);
            likeRepository.save(existing);
        }
        return buildResponse(targetType, targetId, email);
    }

    public ActionResponse getStats(TargetType targetType, String targetId, String email) {
        return buildResponse(targetType, targetId, email);
    }

    private ActionResponse buildResponse(TargetType targetType, String targetId, String email) {
        long likes = likeRepository.countByTargetTypeAndTargetIdAndType(targetType, targetId, LikeType.LIKE);
        long dislikes = likeRepository.countByTargetTypeAndTargetIdAndType(targetType, targetId, LikeType.DISLIKE);
        String userState = "NONE";
        LikeRecord existing = likeRepository.findByTargetTypeAndTargetIdAndEmail(targetType, targetId, email).orElse(null);
        if (existing != null) {
            userState = existing.getType() == LikeType.LIKE ? "LIKED" : "DISLIKED";
        }
        return new ActionResponse(likes, dislikes, userState);
    }
}
