package br.com.sankhya.helpcenter.searchbot.utils;

import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.com.sankhya.helpcenter.searchbot.robot.HelpcenterRobot;

public class EmailSenderUtil {

	final static Logger logger = LogManager.getLogger(HelpcenterRobot.class);

	private static void constructEmail(Session session, String toEmail, String subject, String body) {
		try {
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");

			msg.setFrom(new InternetAddress("nilson.silva@jiva.com.br", "HelpCenterSearchBot Logger"));

			msg.setReplyTo(InternetAddress.parse("nilson.silva@jiva.com.br", false));

			msg.setSubject(subject, "UTF-8");

			msg.setText(body, "UTF-8");

			msg.setSentDate(new Date());

			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
			Transport.send(msg);

			logger.info("Reportando o erro para a equipe de desenvolvimento...");
		} catch (Exception e) {
			logger.error("Não foi possível enviar email. Causa: ", e);
		}
	}

	public static void reportErrorViaEmail(String content) {
		final String fromEmail = "nilson.silva@Jiva.com.br";
		final String password = "Luci@ne123";
		final String toEmail = "nyson96@hotmail.com";

		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.office365.com");
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true"); 
		props.put("mail.smtp.starttls.enable", "true"); 

		Authenticator auth = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(fromEmail, password);
			}
		};
		Session session = Session.getInstance(props, auth);

		constructEmail(session, toEmail, "ERRO na aplicação HelpCenterSearchBot!", content);

	}

}
