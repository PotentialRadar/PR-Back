package com.potential_radar.PR.notification.controller;

import com.potential_radar.PR.notification.dto.NotificationPageResDto;
import com.potential_radar.PR.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        return notificationService.subscribe(lastEventId);
    }

    // 내 알림 리스트
    @GetMapping("/list")
    public ResponseEntity<?> getMyNotifications(
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        NotificationPageResDto response = notificationService.getNotifications(lastId, size);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 알림 삭제
    @PostMapping("/delete/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId){
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    // 알림 읽음 처리
//    @PostMapping("/update/read/{notificationId}")
//    public ResponseEntity<?> updateNotificationReadStatus(@PathVariable Long notificationId){
//        notificationService.updateNotificationReadStatus(notificationId);
//        return ResponseEntity.ok().build();
//    }


    // 알림 전체 읽음 처리
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

}
