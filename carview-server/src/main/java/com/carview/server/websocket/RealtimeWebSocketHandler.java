package com.carview.server.websocket;

import com.carview.common.model.RealtimeAgg;
import com.carview.server.mapper.StatisticsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@EnableScheduling
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeWebSocketHandler.class);
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final StatisticsMapper statisticsMapper;
    private final ObjectMapper objectMapper;

    public RealtimeWebSocketHandler(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket 连接建立: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket 连接关闭: {}", session.getId());
    }

    @Scheduled(fixedRate = 3000)
    public void pushRealtimeData() {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            RealtimeAgg latest = statisticsMapper.getLatestAgg();
            if (latest == null) {
                return;
            }

            String json = objectMapper.writeValueAsString(latest);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("推送数据失败, session={}", session.getId(), e);
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取实时数据失败", e);
        }
    }
}
