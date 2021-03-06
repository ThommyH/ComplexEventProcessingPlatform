package sushi.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Table;

import sushi.correlation.ConditionParser;
import sushi.event.attribute.SushiAttribute;
import sushi.event.collection.SushiMapTree;
import sushi.notification.SushiCondition;
import sushi.persistence.Persistable;
import sushi.persistence.Persistor;

/**
 * An EventTypeRule states a rule for creating new Events from existing Events in the database.
 * Events from certain eventTypes (usedEventTypes),
 * that fulfill a certain condition (conditionString) are trigger the creation
 * of a new event of the eventType createdEventType.
 */
@Entity
@Table(name = "EventTypeRule")
public class EventTypeRule extends Persistable{
			
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int ID;
	
	@ManyToMany(cascade = CascadeType.PERSIST, fetch=FetchType.EAGER)
	private ArrayList<SushiEventType> usedEventTypes = new ArrayList<SushiEventType>();;
	
	public ArrayList<SushiEventType> getUsedEventTypes() {
		return usedEventTypes;
	}

	public void setUsedEventTypes(ArrayList<SushiEventType> usedEventTypes) {
		this.usedEventTypes = usedEventTypes;
	}

	 @OneToOne(optional=true, cascade = CascadeType.PERSIST, fetch= FetchType.EAGER)
	private SushiCondition condition;
	
	@OneToOne(optional=true)
	@JoinColumn(name = "EventType_ID")
	private SushiEventType createdEventType;

	
	@ManyToOne(cascade={CascadeType.PERSIST, CascadeType.REMOVE})
	@JoinColumn(name="MapTreeID")
	private SushiMapTree<String, Serializable> eventAttributes;
	
	public EventTypeRule(){
		this.ID = 0;
		this.condition = null;
		this.createdEventType = null;
	}
	
	/**
	 * An EventTypeRule states a rule for creating new Events from existing Events in the database.
	 * Events from certain eventTypes (usedEventTypes),
	 * that fulfill a certain condition (conditionString) are trigger the creation
	 * of a new event of the eventType createdEventType  
	 * @param usedEventTypes: eventType of events that can trigger a new event
	 * @param conditionString: condition, that must be fulfilled to trigger a new event
	 * @param createdEventType: eventtype of the event created
	 */
	public EventTypeRule(ArrayList<SushiEventType> usedEventTypes, SushiCondition condition, SushiEventType createdEventType){
		this.ID = 0;
		this.usedEventTypes = usedEventTypes;
		this.condition = condition;
		this.createdEventType = createdEventType;
		this.eventAttributes = ConditionParser.extractEventAttributes(condition.getConditionString());
	}
	
	/**
	 * This method executes the event type rule on the existing events from the database.
	 * It will create new events.
	 * @return created events
	 */
	public ArrayList<SushiEvent> execute() {
		List<SushiEvent> chosenEvents = new ArrayList<SushiEvent>();
		//find Events for EventType
		for (SushiEventType usedEventType : usedEventTypes) {
			List<SushiEvent> chosenEventsForType = SushiEvent.findByEventType(usedEventType);
			if (! eventAttributes.isEmpty()) {
				chosenEventsForType.retainAll(SushiEvent.findByValues(eventAttributes));
			}
			chosenEvents.addAll(chosenEventsForType);
		}
		//create new Events
		ArrayList<SushiEvent> newEvents = new ArrayList<SushiEvent>();
		for (SushiEvent event : chosenEvents) {
			SushiEvent newEvent = new SushiEvent(this.createdEventType, event.getTimestamp(), createValues(event.getValues()));
			newEvents.add(newEvent);
		}
		return newEvents;
	}
	
	private SushiMapTree<String, Serializable> createValues(Map<String, Serializable> values) {
		SushiMapTree<String, Serializable> newValues= new SushiMapTree<String, Serializable>();
		for (SushiAttribute attribute : createdEventType.getValueTypes()) {
			String attributeName = attribute.getAttributeExpression();
			if (values.containsKey(attributeName)) {
				newValues.put(attributeName, values.get(attributeName));
			}
		}
		return newValues;
	}

	/**
	 * Removes the event type from the source event types of this rule.
	 * This is needed for instance if the event type will be deleted.
	 * @param eventType
	 * @return 
	 */
	public boolean removeUsedEventType(SushiEventType eventType){
		for(SushiEventType usedEventType : usedEventTypes){
			if(usedEventType.equals(eventType)){
				boolean result = usedEventTypes.remove(usedEventType);
				this.save();
				return result;
			}
		}
		return false;
	}

	//Getter and Setter
	
	public SushiEventType getCreatedEventType() {
		return createdEventType;
	}

	public void setCreatedEventType(SushiEventType createdEventType) {
		this.createdEventType = createdEventType;
	}

	@Override
	public int getID() {
		return ID;
	}	
	
	//JPA-Methods
	
	/**
	 * Finds the event type rules that use a certain event type as a source.
	 * @param eventType
	 * @return event type rules
	 */
	public static List<EventTypeRule> findEventTypeRuleForContainedEventType(SushiEventType eventType) {
		Query query = Persistor.getEntityManager().createNativeQuery("" +
				"SELECT * " +
				"FROM EventTypeRule " +
				"WHERE ID IN (" +
				"	SELECT A.EventTypeRule_ID " +
				"	FROM EventTypeRule_EventType AS A " +
				"	WHERE usedEventTypes_ID = " + eventType.getID() + ")", EventTypeRule.class);
		return query.getResultList();
	}

	/**
	 * Finds the event type rules that use a certain event type as output.
	 * @param eventType
	 * @return event type rules
	 */
	public static EventTypeRule findEventTypeRuleForCreatedEventType(SushiEventType eventType) {
		Query query = Persistor.getEntityManager().createNativeQuery("SELECT * FROM EventTypeRule WHERE EventType_ID = " + eventType.getID(), EventTypeRule.class);
		assert(query.getResultList().size() < 2);
		if (query.getResultList().size() > 0) {
			return (EventTypeRule) query.getResultList().get(0);}
		else {
			return null;
		}
	}
		
}
