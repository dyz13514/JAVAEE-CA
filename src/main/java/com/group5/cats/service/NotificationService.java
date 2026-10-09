package com.group5.cats.service;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.NotificationType;
public interface NotificationService {
    void createNotification(CourseApplication application, NotificationType type);
}
