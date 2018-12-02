import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class SendEmailNotication implements RequestHandler<Object, Boolean> {
    @Override
    public Boolean handleRequest(Object input, Context context) {
        try {
            sendUpcomingAssignmentNotifications();
            return true;
        } catch (Exception ex) {
            System.out.println("Failed");
            System.out.println(ex);
        }

        return false;
    }

    public static void main(String[] args) {
        try {
            sendUpcomingAssignmentNotifications();
        } catch (Exception ex) {
            System.out.println("Failed");
            System.out.println(ex);
        }
    }

    public static Map<String, ArrayList<Map<Date, ArrayList<String>>>> getUpcomingAssignments() throws Exception {
        Map<String, ArrayList<Map<Date, ArrayList<String>>>> emailNotifications = new HashMap<>();

        Connection dbCon = new DatabaseConnection().getConnection();
        try {
            PreparedStatement ps = dbCon.prepareStatement("SELECT userId, emailAddress FROM Users");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String emailAddress = rs.getString("emailAddress");
                ArrayList<Map<Date, ArrayList<String>>> assignments = new ArrayList<>();
                Map<Date, ArrayList<String>> assignment = new HashMap<>();

                PreparedStatement psAssignments = dbCon.prepareStatement("SELECT Assignments.assignmentName, Assignments.assignmentDueDate, Classes.className, Assignments.userId FROM Assignments INNER JOIN Users ON Assignments.userId = Users.userId INNER JOIN Classes ON Assignments.classId = Classes.classId WHERE Users.userId=? AND (assignmentDueDate >= Convert(datetime, GETDATE()) AND assignmentDueDate <= Convert(datetime, DATEADD(day, 3, Convert(datetime, GETDATE())))) ORDER BY assignmentDueDate ASC");
                psAssignments.setInt(1, rs.getInt("userId"));
                ResultSet assignmentsRs = psAssignments.executeQuery();

                Date lastDate = null;
                while (assignmentsRs.next()) {
                    Date currentDate = assignmentsRs.getDate("assignmentDueDate");
                    if (lastDate != null && lastDate.compareTo(currentDate) == 0) {
                        assignment.get(currentDate).add(assignmentsRs.getString("className") + " Class: " + assignmentsRs.getString("assignmentName"));
                    } else {
                        ArrayList<String> assignmentsForDate = new ArrayList<>();
                        assignmentsForDate.add(assignmentsRs.getString("className") + " Class: " + assignmentsRs.getString("assignmentName"));
                        assignment.put(currentDate, assignmentsForDate);
                        assignments.add(assignment);
                    }

                    lastDate = currentDate;
                }

                emailNotifications.put(emailAddress, assignments);
            }

            return emailNotifications;
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            dbCon.close();
        }

        return null;
    }

    // Send an email notification for any assignments due in three days or less
    public static void sendUpcomingAssignmentNotifications() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("cmsc495gradebook","1234test%");
                    }
                });

        try {
            Message message = new MimeMessage(session);
            Map<String, ArrayList<Map<Date, ArrayList<String>>>> assignments = getUpcomingAssignments();
            for (String emailAddress : assignments.keySet()) {
                if (assignments.get(emailAddress).size() > 0) {
                    String messageText = "";
                    message.setFrom(new InternetAddress("cmsc495gradebook@gmail.com"));
                    message.setRecipients(Message.RecipientType.TO,
                            InternetAddress.parse(emailAddress));
                    message.setSubject("GradeBook: Upcoming Assignments Reminder");

                    int assignmentCount = assignments.get(emailAddress).get(0).keySet().size();
                    messageText += "Hello,\n\nThank you for using the GradeBook. This is a friendly reminder that the following " + assignmentCount + " assignments will be due within the next three days: ";

                    SortedSet<Date> keys = new TreeSet<Date>(assignments.get(emailAddress).get(0).keySet());
                    for (Date assignmentDueDate : keys) {
                        messageText += "\n\nDue on " + new SimpleDateFormat("MMMM dd, yyyy").format(assignmentDueDate) + ": ";
                        for (int i = 0; i < assignments.get(emailAddress).get(0).get(assignmentDueDate).size(); i++) {
                            messageText += "\n" + assignments.get(emailAddress).get(0).get(assignmentDueDate).get(i);
                        }
                    }

                    messageText += "\n\nWishing you success on your assignments!\n\nThe GradeBook";
                    message.setText(messageText);
                    Transport.send(message);
                }
            }

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (Exception ex) {

        }
    }
}
