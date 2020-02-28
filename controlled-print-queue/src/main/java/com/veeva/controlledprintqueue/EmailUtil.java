/* 
 * Copyright (c) 2018 Veeva Systems Inc.
 * All Rights Reserved.
 * The Controlled-Print-Queue is developed and owned by Veeva Systems Inc.;
 * it is not to be copied, reproduced, or transmitted in any form, by any means, in whole or in part,
 * and may only be used in connection with the deliverable with which it was provided to Customer.
 */
package com.veeva.controlledprintqueue;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;

/**
 * Email Utility to build up and send emails.
 * @author amol.chinchalkar
 *
 */
public class EmailUtil {
	
	/** Email Recipient */
	@Value("${email.to}")
	private String emailTo;
	
	/** Email from */
	@Value("${email.from.address}")
	private String emailFrom;
	
	/** Email Password (Encrypted) */
	@Value("${email.from.password}")
	private String emailFromPassword;
	
	/** Host Smtp Name */
	@Value("${email.host}")
	private String host;
	
	/** Smtp Port Number*/
	@Value("${email.port}")
	private String port;
	
	/** Authentication Enabled */
	@Value("${email.auth}")
	private String auth;
	
	/** TLS Auth Enabled */
	@Value("${email.tls.enabled}")
	private String tlsEnabled;
	

	/**
	 * Creates and sends email template for a failure scenario
	 * @param reason
	 * @param documentId
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws UnknownHostException
	 */
	public void GenerateFailureEmail(String reason, long documentId) throws UnsupportedEncodingException, MessagingException, UnknownHostException {
		
		final String subject = "Veeva Controlled Printing Critical Failure";
		
		InetAddress address = InetAddress.getLocalHost();
		
		Properties props = new Properties();
		props.put("mail.smtp.host", host); //SMTP Host
		props.put("mail.smtp.port", port); 
		props.put("mail.smtp.auth", auth); //enable authentication
		props.put("mail.smtp.starttls.enable",tlsEnabled); //enable STARTTLS
		
		String body = String.format("Controlled printing failed for Document Id: %1$s<br><br>Server: %2$s<br><br>Reason: %3$s", documentId, address.getHostName(), reason);
        
		Authenticator auth = new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(emailFrom, emailFromPassword);
			}
		};
		
		Session session = Session.getInstance(props, auth);
		SendEmail(session, subject, body);
	}
	
	/**
	 * Utility method to send simple HTML email
	 * @param session
	 *  - Email Session
	 * @param toEmail
	 *  - Email Recipient
	 * @param subject
	 *  - Email Subject
	 * @param body
	 *  - Email Body
	 * @throws UnsupportedEncodingException 
	 */
	private void SendEmail(Session session, String subject, String body) throws MessagingException, UnsupportedEncodingException {

      MimeMessage msg = new MimeMessage(session);

      msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
      msg.addHeader("format", "flowed");
      msg.addHeader("Content-Transfer-Encoding", "8bit");
      //update to indicate from which is relavent  to integration
      msg.setFrom(new InternetAddress(emailFrom, "Veeva-Vault"));
      msg.setReplyTo(InternetAddress.parse(emailTo, false));
      msg.setSubject(subject, "UTF-8");
      msg.setContent(body, "text/html; charset=utf-8");
      msg.setSentDate(new Date());
      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo, false));
      
	  Transport.send(msg);  
    }
}
