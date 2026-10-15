package com.jcaa.notifiers.application.port.out;

import com.jcaa.notifiers.application.port.out.dto.EmailMessage;

public interface EmailSenderPort {

  void send(EmailMessage message);
}
