package com.example;

import com.example.SendEmail.PubSubMessage;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;
import org.json.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

public class SendEmail implements BackgroundFunction<PubSubMessage> {
  private static final Logger logger = Logger.getLogger(SendEmail.class.getName());

  @Override
  public void accept(PubSubMessage message, Context context) {
    try {
      if (message.data != null) {
        JSONObject object = toJsonObject(message.data);
        if (shouldSendEmail(object)) {
          logger.info("Got water height of " + object.get("maximum_water_height") + " - sending email!!!");
          sendEmail(object);
        }
      }
    } catch(Exception e) {
      logger.warning("Error:" + e.getMessage());
    }
  }

  private JSONObject toJsonObject(String message) {
    return new JSONObject(new String(Base64.getDecoder().decode(message)));
  }

  public static class PubSubMessage {
    String data;
    Map<String, String> attributes;
    String messageId;
    String publishTime;
  }

  private boolean shouldSendEmail(JSONObject object) {
    return object.getBigDecimal("maximum_water_height").compareTo(BigDecimal.TEN) >= 0;
  }

  private void sendEmail(JSONObject object) {
    Email from = new Email(System.getenv("SENDGRID_FROM_EMAIL"));
    String subject = "Tsunami alert!!!";
    Email to = new Email(System.getenv("SENDGRID_TO_EMAIL"));
    Content content = new Content("text/plain", emailBody(object));
    Mail mail = new Mail(from, subject, to, content);

    SendGrid sg = new SendGrid(System.getenv("SENDGRID_API_KEY"));
    Request request = new Request();
    try {
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());
      Response response = sg.api(request);
      logger.info("Got response [" + response.getStatusCode() + "] Body {"+ response.getBody() + "}");
    } catch (IOException ex) {
      logger.warning("Failed to send email " + ex.getMessage());
    }
  }

  private String emailBody(JSONObject object) {
    String message = "We just received a wave of height " + object.get("maximum_water_height") +
      " at longitude " + object.getBigDecimal("longitude") + " and latitude " + object.getBigDecimal("latitude");
    logger.info("Email body " + message);
    return message;
  }
}