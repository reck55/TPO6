package zad1;

import javax.naming.*;
import javax.jms.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.Hashtable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JWSChat extends JFrame implements MessageListener {

    private TopicConnection con;
    private TopicSession subscriberSes;
    private TopicSession publisherSes;
    private TopicPublisher publisher;
    private TopicSubscriber subscriber;
    private JTextArea ta = new JTextArea(10, 20);
    private JButton sendButton;
    private String nick = "Default";

    public JWSChat(String destName) {

        try {
            Hashtable env = new Hashtable(11);
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.exolab.jms.jndi.InitialContextFactory");
            env.put(Context.PROVIDER_URL, "tcp://localhost:3035");
            Context ctx = new InitialContext(env);
            TopicConnectionFactory factory = (TopicConnectionFactory) ctx.lookup("JmsTopicConnectionFactory");
            Topic topic = (Topic) ctx.lookup(destName);
            con = factory.createTopicConnection();
            publisherSes = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            publisher = publisherSes.createPublisher(topic);
            subscriberSes = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            subscriber = subscriberSes.createSubscriber(topic);
            subscriber.setMessageListener(this);
            con.start();
        } catch (Exception exc) {
            exc.printStackTrace();
            System.exit(1);
        }

        this.getContentPane().setLayout(new BorderLayout());
        add(new JScrollPane(ta), BorderLayout.CENTER);
        adminDialog();
        add(chatSend(), BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try { con.close(); } catch(Exception exc) {}
                dispose();
                System.exit(0);
            }
        });
        setTitle("Chat");
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void adminDialog() {
        JDialog dialog = new JDialog();
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setTitle("Login");
        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(400, 100));
        dialog.getContentPane().setLayout(new BorderLayout());

        JPanel textPanels = new JPanel();
        textPanels.setLayout(new GridLayout(1, 2));

        JTextField loginLabel = new JTextField("Nickname:");
        JTextField loginField = new JTextField();

        loginLabel.setEditable(false);
        loginField.setEditable(true);

        textPanels.add(loginLabel);
        textPanels.add(loginField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!loginField.getText().equals("")) {
                   nick = loginField.getText();
                   sendButton.setEnabled(true);
                   dialog.dispose();
                }
            }
        });

        dialog.add(textPanels, BorderLayout.CENTER);
        dialog.add(loginButton, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setVisible(true);
    }

    public JPanel chatSend() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JTextField chatBox = new JTextField();
        chatBox.setEditable(true);

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!chatBox.getText().equals("")) {
                    try {
                        sendMessage(nick + ": " + chatBox.getText());
                    } catch (JMSException jmsException) {
                        jmsException.printStackTrace();
                    }
                }
            }
        });

        panel.add(chatBox, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        return panel;
    }

    public void sendMessage(String message) throws JMSException {
        Message msg = publisherSes.createTextMessage(message);
        publisher.publish(msg);
        //ta.append(message + "\n");
    }

    public void onMessage(Message msg) {
        try {
            ta.append(((TextMessage) msg).getText() + "\n");
        } catch(JMSException exc) { System.err.println(exc); }
    }

    public static void main(String[] args) {
        new JWSChat("topic1");
    }
}
