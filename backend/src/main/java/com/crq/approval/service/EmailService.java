package com.crq.approval.service;

import com.crq.approval.model.Crq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Slf4j
@Service
@Profile("!mock")
public class EmailService implements EmailPort {

    @Value("${email.from}")
    private String from;

    @Value("${email.to}")
    private String to;

    @Value("${email.subject}")
    private String subject;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an approval notification email for a list of approved CRQs.
     */
    public void sendApprovalEmail(List<Crq> approvedCrqs) {
        if (approvedCrqs == null || approvedCrqs.isEmpty()) {
            log.info("No approved CRQs to send email for");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to.split(","));
            helper.setSubject(subject + " - " + approvedCrqs.size() + " CRQ(s) Approved");
            helper.setText(buildEmailBody(approvedCrqs), true);

            mailSender.send(message);
            log.info("Approval email sent for {} CRQs", approvedCrqs.size());
        } catch (Exception e) {
            log.error("Failed to send approval email: {}", e.getMessage(), e);
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends approval email for a single CRQ.
     */
    public void sendApprovalEmail(Crq crq) {
        sendApprovalEmail(List.of(crq));
    }

    private String buildEmailBody(List<Crq> crqs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2 style='color:#2d6a4f;'>CRQ Approval Notification</h2>");
        sb.append("<p>The following Change Requests have been approved (Status: <strong>Request in Change</strong>):</p>");
        sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;font-family:Arial,sans-serif;'>");
        sb.append("<thead style='background-color:#40916c;color:white;'>");
        sb.append("<tr><th>#</th><th>CRQ Number</th><th>Title</th><th>Assignee</th><th>Remedy Status</th></tr>");
        sb.append("</thead><tbody>");

        int idx = 1;
        for (Crq crq : crqs) {
            String rowColor = idx % 2 == 0 ? "#f0fdf4" : "#ffffff";
            sb.append("<tr style='background-color:").append(rowColor).append(";'>");
            sb.append("<td>").append(idx++).append("</td>");
            sb.append("<td><strong>").append(safe(crq.getCrqNumber())).append("</strong></td>");
            sb.append("<td>").append(safe(crq.getTitle())).append("</td>");
            sb.append("<td>").append(safe(crq.getAssignee())).append("</td>");
            sb.append("<td style='color:green;'>").append(safe(crq.getRemedyStatus())).append("</td>");
            sb.append("</tr>");
        }

        sb.append("</tbody></table>");
        sb.append("<br/><p style='color:#666;font-size:12px;'>This is an automated notification from the CRQ Approval System.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String safe(String val) {
        return val != null ? val : "-";
    }
}
