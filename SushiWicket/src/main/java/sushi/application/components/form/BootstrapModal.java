package sushi.application.components.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import de.agilecoders.wicket.Bootstrap;

/**
 * This is a {@link Panel} subclass and is rendered as a Bootstrap modal for visualizing a kind of pop-up.
 * @author micha
 */
public abstract class BootstrapModal extends Panel {

	private static final long serialVersionUID = 1L;
	private String heading;
	private String id;
	
	Model<String> visibleModel = new Model<String>("display: block;");
	
	@Override
	public void renderHead(IHeaderResponse response) {
	    super.renderHead(response);
		Bootstrap.renderHead(response);
	}

	/**
	 * Constructor for a Bootstrap modal for visualizing a kind of pop-up.
	 * @param id
	 * @param heading
	 */
	public BootstrapModal(String id, String heading) {
		super(id);
		this.id = id;
		this.heading = heading;
		this.add(AttributeModifier.replace("id", id));
		this.add(AttributeModifier.replace("class", "modal hide fade in"));
		this.add(AttributeModifier.replace("style", "display: none;"));
		buildMainLayout();
	}

	private void buildMainLayout() {
		add(new Label("modalHeadline", heading));
	}

	/**
	 * Show the modal in an ajax request.
	 * @param target
	 */
	public void show(AjaxRequestTarget target) {
		target.appendJavaScript("$(\"#" + id + "\").modal('toggle'); ");
	}
	
	/**
	 * Close the modal in an ajax request.
	 * @param target
	 */
	public void close(AjaxRequestTarget target) {
		target.appendJavaScript("$(\"#" + id + "\").modal('hide'); ");
	}

}
