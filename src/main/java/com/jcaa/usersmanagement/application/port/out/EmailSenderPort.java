package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.application.port.out.dto.EmailNotificationRequest;

public interface EmailSenderPort {
  void send(EmailNotificationRequest request);
}
