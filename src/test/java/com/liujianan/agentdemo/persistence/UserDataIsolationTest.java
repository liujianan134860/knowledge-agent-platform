package com.liujianan.agentdemo.persistence;

import com.liujianan.agentdemo.harness.ChatSession;
import com.liujianan.agentdemo.harness.ChatMessage;
import com.liujianan.agentdemo.harness.ChatMessageRepository;
import com.liujianan.agentdemo.harness.ChatSessionRepository;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserDataIsolationTest {
    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Test
    void sessionsAndDocuments_areFilteredByUserId() {
        chatSessionRepository.save(new ChatSession("s-user-a", new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(), "u-a"));
        chatSessionRepository.save(new ChatSession("s-user-b", new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(), "u-b"));

        documentChunkRepository.save(new DocumentChunk(null, "A doc", "A content",
                List.of("a"), LocalDateTime.now(), "u-a"));
        documentChunkRepository.save(new DocumentChunk(null, "B doc", "B content",
                List.of("b"), LocalDateTime.now(), "u-b"));

        assertThat(chatSessionRepository.findByUserIdOrderByUpdatedAtDesc("u-a"))
                .extracting(ChatSession::getId)
                .containsExactly("s-user-a");
        assertThat(documentChunkRepository.findByUserId("u-a"))
                .extracting(DocumentChunk::getTitle)
                .containsExactly("A doc");
    }

    @Test
    void sessionMessages_persistLongAssistantAnswer() {
        String longAnswer = "assistant-answer-".repeat(80);
        ChatSession session = new ChatSession("s-long-answer", new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(), "u-a");
        chatSessionRepository.saveAndFlush(session);
        chatMessageRepository.save(new ChatMessage(null, "s-long-answer", "u-a",
                "user", "question", LocalDateTime.now()));
        chatMessageRepository.saveAndFlush(new ChatMessage(null, "s-long-answer", "u-a",
                "assistant", longAnswer, LocalDateTime.now()));

        List<ChatMessage> reloaded = chatMessageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc("u-a", "s-long-answer");
        assertThat(reloaded).hasSize(2);
        assertThat(reloaded.get(1).getRole()).isEqualTo("assistant");
        assertThat(reloaded.get(1).getContent()).isEqualTo(longAnswer);
    }
}
