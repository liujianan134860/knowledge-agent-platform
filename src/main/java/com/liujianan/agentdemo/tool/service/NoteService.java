package com.liujianan.agentdemo.tool.service;

import com.liujianan.agentdemo.tool.entity.NoteItem;
import com.liujianan.agentdemo.tool.repository.NoteItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoteService {
    private final NoteItemRepository noteRepository;

    public NoteService(NoteItemRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional
    public NoteItem create(String userId, String title, String content, String tags) {
        LocalDateTime now = LocalDateTime.now();
        NoteItem note = new NoteItem(null, userId, title, content, tags, now, now);
        return noteRepository.save(note);
    }

    public List<NoteItem> list(String userId) {
        return noteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<NoteItem> listSince(String userId, LocalDateTime since) {
        return noteRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, since);
    }

    public List<NoteItem> search(String userId, String keyword) {
        return noteRepository.findByUserIdAndTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                userId, keyword, keyword);
    }
}
