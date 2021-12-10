package com.smart.service;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

	//this method is responsible to send email
		public boolean sendEmail(String subject, String message, String to)
		{
			boolean f = false;
			
			String from="shock860482@gmail.com";
			
//			message="Hello, Arpit this side";
//			subject="CodersArea: Confirmation";
//			to="arpitawasthi.14@gmail.com";
			
			//host for gmail
			String host="smtp.gmail.com";
			
			//get system properties
			Properties properties = System.getProperties();
			
			//setting important information to properties object
			properties.put("mail.smtp.host", host);
			properties.put("mail.smtp.port", "465");  //port of gmail server
			properties.put("mail.smtp.ssl.enable", "true");
			properties.put("mail.smtp.auth", "true");
			
			//Step 1: To get the session object...
			Session session = Session.getInstance(properties, new Authenticator() {

				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication("shock860482@gmail.com", "@s$hock24");
				}
				
			});
			
			session.setDebug(true);
			
			//Step 2: compose the message
			MimeMessage m = new MimeMessage(session);
			
			try {
				
				//from email
				m.setFrom(from);
				
				//adding recipient to message
				m.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
				
				//adding subject to message
				m.setSubject(subject);
				
				
				//adding text to message
//				m.setText(message);
				m.setContent(message, "text/html");
				
				//send the message using transport class
				Transport.send(m);
				
				System.out.println("Sent successfully.................");
				
				f=true;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return f;
		} 
}
