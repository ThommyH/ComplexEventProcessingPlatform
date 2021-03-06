package sushi.application.pages.monitoring.notification;

import sushi.application.components.form.BootstrapModal;

/**
 * Modal that encapsulates the panel for the creation of a notificationRule
 */
public class AddNotificationModal extends BootstrapModal {

	private static final long serialVersionUID = 1L;
	private NotificationCreationPanel panel;

	/**
     * 
     * @param notificationPage
     * @param window
     */
    public AddNotificationModal(String id, final NotificationPage notificationPage) {
    	super(id, "Add Notification");
    	panel = new NotificationCreationPanel("notificationCreationPanel", notificationPage);
    	add(panel);
	}
}
