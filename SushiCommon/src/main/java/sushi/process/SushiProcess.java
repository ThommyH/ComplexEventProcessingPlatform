package sushi.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import sushi.bpmn.element.AbstractBPMNElement;
import sushi.bpmn.element.BPMNProcess;
import sushi.correlation.CorrelationRule;
import sushi.correlation.TimeCondition;
import sushi.event.SushiEventType;
import sushi.event.attribute.SushiAttribute;
import sushi.event.collection.SushiTree;
import sushi.persistence.Persistable;
import sushi.persistence.Persistor;

/**
 * This class represents a business process with bounded {@link SushiEventType}s and eventually a associated {@link BPMNProcess}.
 * The process knows the correlation rules under which events are assigned to concrete {@link SushiProcessInstance}s of this process. 
 * @author micha
 */
@Entity
@Table(name = "Process")
public class SushiProcess extends Persistable implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int ID;
	
	@Column(name = "NAME")
	private String name;
	
	@ManyToMany(fetch=FetchType.EAGER, cascade = CascadeType.MERGE)
	@JoinTable(name="ProcessEventTypes", joinColumns={@JoinColumn(name="Id")})
	@JoinColumn(name="EventTypes")
	private Set<SushiEventType> eventTypes = new HashSet<SushiEventType>();
	
	@OneToMany(fetch=FetchType.EAGER)
	private List<SushiProcessInstance> processInstances = new ArrayList<SushiProcessInstance>();
	
	@OneToOne(cascade = CascadeType.MERGE)
	private TimeCondition timeCondition;
	
	@OneToOne(cascade = CascadeType.MERGE)
	private BPMNProcess bpmnProcess;
	
	@OneToMany(fetch=FetchType.EAGER)
	private List<SushiAttribute> correlationAttributes = new ArrayList<SushiAttribute>();
	
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "process")
	private Set<CorrelationRule> correlationRules = new HashSet<CorrelationRule>();

//	@OneToOne(cascade = CascadeType.MERGE)
	@Transient
	private SushiTree<AbstractBPMNElement> processDecompositionTree = new SushiTree<AbstractBPMNElement>();
	
	/**
	 * Default-Constructor for JPA.
	 */
	public SushiProcess(){
		this.name = "";
	}
	
	public SushiProcess(String name){
		this.name = name;
	}
	
	public SushiProcess(String name, List<SushiEventType> eventTypes){
		this.name = name;
		this.eventTypes.addAll(eventTypes);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getID() {
		return ID;
	}

	public void setID(int ID) {
		this.ID = ID;
	}
	
	public ArrayList<SushiEventType> getEventTypes() {
		return new ArrayList<SushiEventType> (eventTypes);
	}
	
	public List<SushiProcessInstance> getProcessInstances() {
		return processInstances;
	}

	public void setProcessInstances(List<SushiProcessInstance> processInstances) {
		this.processInstances = processInstances;
	}
	
	public List<SushiAttribute> getCorrelationAttributes() {
		return correlationAttributes;
	}

	public void setCorrelationAttributes(List<SushiAttribute> correlationAttributes) {
		this.correlationAttributes = correlationAttributes;
	}
	
	public boolean isCorrelationWithCorrelationRules() {
		return !correlationRules.isEmpty();
	}
	
	public void addCorrelationAttribute(SushiAttribute correlationAttribute){
		if(!correlationAttributes.contains(correlationAttribute)){
			correlationAttributes.add(correlationAttribute);
		}
	}
	
	public void addCorrelationAttributes(List<SushiAttribute> correlationAttributes){
		for(SushiAttribute correlationAttribute : correlationAttributes){
			addCorrelationAttribute(correlationAttribute);
		}
	}
	
	public void removeCorrelationAttribute(SushiAttribute correlationAttribute){
		correlationAttributes.remove(correlationAttribute);
	}

	public Set<CorrelationRule> getCorrelationRules() {
		return correlationRules;
	}
	
	public void setCorrelationRules(Set<CorrelationRule> correlationRules) {
		this.correlationRules = correlationRules;
	}

	public void addCorrelationRule(CorrelationRule correlationRule){
		if(!correlationRules.contains(correlationRule)){
			correlationRules.add(correlationRule);
			correlationRule.setProcess(this);
		}
	}
	
	public void addCorrelationRules(Set<CorrelationRule> correlationRules){
		for(CorrelationRule rule : correlationRules){
			addCorrelationRule(rule);
		}
	}
	
	public void removeCorrelationRule(CorrelationRule correlationRule){
		correlationRules.remove(correlationRule);
		correlationRule.setProcess(null);
	}

	public boolean addProcessInstance(SushiProcessInstance processInstance) {
		if(!processInstances.contains(processInstance)){
			return processInstances.add(processInstance);
		}
		return false;
	}

	public boolean removeProcessInstance(SushiProcessInstance processInstance) {
		return processInstances.remove(processInstance);
	}

	public void setEventTypes(Set<SushiEventType> eventTypes) {
		this.eventTypes = eventTypes;
	}
	
	public boolean addEventType(SushiEventType eventType) {
		if(!eventTypes.contains(eventType)){
			eventType.save();
			eventTypes.add(eventType);
			return true;
		}
		return false;
	}

	public boolean removeEventType(SushiEventType eventType) {
		if (!eventTypes.remove(eventType)){
			// EventType als Notfallvariante per Typename und ID suchen und löschen, 
			// da durch JPA teilweise unterschiedliche ObjektIDs entstehen können
			for(SushiEventType containedEventType : eventTypes){
				if(containedEventType.getID() == eventType.getID() && containedEventType.getTypeName().equals(eventType.getTypeName())){
					return eventTypes.remove(containedEventType);
				}
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		String processText = this.name + "(" + this.ID + ")";
		return processText;
	}
	
	/**
	 * Returns all {@link SushiProcess}es from the database.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<SushiProcess> findAll() {
		Query q = Persistor.getEntityManager().createQuery("SELECT t FROM SushiProcess t");
		return q.getResultList();
	}
	
	/**
	 * Returns all {@link SushiProcess}es from the database, which have the given {@link SushiEventType}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<SushiProcess> findByEventType(SushiEventType eventType){
		Query query = Persistor.getEntityManager().createNativeQuery("" +
				"Select * " +
				"FROM Process " +
				"WHERE ID IN (" +
					"Select Id " +
					"FROM ProcessEventTypes " +
					"WHERE eventTypes_ID = '" + eventType.getID()+ "')", SushiProcess.class);
		return query.getResultList();
	}
	
	/**
	 * Returns all {@link SushiProcess}es from the database, which have the given attribute and associated value.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<SushiProcess> findByAttribute(String columnName, String value){
		Query query = Persistor.getEntityManager().createNativeQuery("SELECT * FROM Process WHERE " + columnName + " = '" + value + "'", SushiProcess.class);
		return query.getResultList();
	}
	
	/**
	 * Returns all {@link SushiProcess}es from the database, which have the given ID.
	 * @return
	 */
	public static SushiProcess findByID(int ID){
		List<SushiProcess> processes = findByAttribute("ID", Integer.toString(ID));
		 if(!processes.isEmpty()){
			 return processes.get(0);
		 }else{
			 return null;
		 }
	}
	
	/**
	 * Returns all {@link SushiProcess}es from the database, which have an ID greater than the given.
	 * @return
	 */
	public static List<SushiProcess> findByIDGreaterThan(int ID){
		return findByAttributeGreaterThan("ID", Integer.toString(ID));
	}
	
	/**
	 * Returns all {@link SushiProcess}es from the database, which have an ID less than the given.
	 * @return
	 */
	public static List<SushiProcess> findByIDLessThan(int ID){
		return findByAttributeLessThan("ID", Integer.toString(ID));
	}
	
	@SuppressWarnings("unchecked")
	private static List<SushiProcess> findByAttributeGreaterThan(String columnName, String value) {
		Query query = Persistor.getEntityManager().createNativeQuery("" +
				"SELECT * FROM Process " +
				"WHERE " + columnName + " > '" + value + "'", SushiProcess.class);
		return query.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	private static List<SushiProcess> findByAttributeLessThan(String columnName, String value) {
		Query query = Persistor.getEntityManager().createNativeQuery("" +
				"SELECT * FROM Process " +
				"WHERE " + columnName + " < '" + value + "'", SushiProcess.class);
		return query.getResultList();
	}
	
	public static List<SushiProcess> findByName(String name){
		return findByAttribute("NAME", name);
	}
	
	/**
	 * Returns all {@link SushiProcess}es from the database, which have the given {@link SushiProcessInstance}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<SushiProcess> findByProcessInstance (SushiProcessInstance processInstance){
		Query query = Persistor.getEntityManager().createNativeQuery("" +
				"Select * " +
				"FROM Process " +
				"WHERE ID IN (" +
					"Select SushiProcess_ID " +
					"FROM Process_ProcessInstance " +
					"WHERE processInstances_ID = '" + processInstance.getID()+ "')", SushiProcess.class);
		return query.getResultList();
	}
	
	/**
	 * Returns all {@link SushiProcess}es from the database, which have a {@link SushiProcessInstance} with the given ID.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<SushiProcess> findByProcessInstanceID (int ID){
		Query query = Persistor.getEntityManager().createNativeQuery("" +
				"Select * " +
				"FROM Process " +
				"WHERE ID IN (" +
					"Select SushiProcess_ID " +
					"FROM Process_ProcessInstance " +
					"WHERE processInstances_ID = '" + ID + "')", SushiProcess.class);
		return query.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	public static List<SushiProcess> findByTimeCondition(TimeCondition timeCondition) {
		Query query = Persistor.getEntityManager().createNativeQuery("" +
				"Select * " +
				"FROM Process " +
				"WHERE TIMECONDITION_ID = '" + timeCondition.getID()+ "'", SushiProcess.class);
		return query.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	public static SushiProcess findByBPMNProcess(BPMNProcess bpmnProcess) {
		Query query = Persistor.getEntityManager().createNativeQuery("" +
				"Select * " +
				"FROM Process " +
				"WHERE BPMNPROCESS_ID = '" + bpmnProcess.getID()+ "'", SushiProcess.class);
		List<SushiProcess> processes = query.getResultList();
		if(!processes.isEmpty()){
			return processes.get(0);
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<SushiProcess> findProcessesByBPMNProcess(BPMNProcess bpmnProcess) {
		Query query = Persistor.getEntityManager().createNativeQuery("" +
				"Select * " +
				"FROM Process " +
				"WHERE BPMNPROCESS_ID = '" + bpmnProcess.getID()+ "'", SushiProcess.class);
		return query.getResultList();
	}

	/**
	 * Saves this process to the database.
	 * @return 
	 */
	@Override
	public SushiProcess save() {
		return (SushiProcess) super.save();
	}
	
	/**
	 * Merges this process to the database.
	 * @return 
	 */
	@Override
	public SushiProcess merge() {
		return (SushiProcess) super.merge();
	}
	
	public static boolean save(ArrayList<SushiProcess> processes) {
		try {
			Persistor.getEntityManager().getTransaction().begin();
			for (SushiProcess process : processes) {
				Persistor.getEntityManager().persist(process);
			}
			Persistor.getEntityManager().getTransaction().commit();
			return true;
		}  catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Deletes this process from the database.
	 * @return 
	 */
	@Override
	public SushiProcess remove() {
		SushiProcessInstance.remove(processInstances);
		return (SushiProcess) super.remove();
	}
	
	/**
	 * Deletes the specified processes from the database.
	 * @return 
	 */
	public static boolean remove(ArrayList<SushiProcess> processes) {
		boolean removed = true;
		for(SushiProcess process : processes){
			removed = (process.remove()!=null);
		}
		return removed;
	}
	
	/**
	 * Deletes all processes from the database.
	 */
	public static void removeAll() {
		try {
			EntityTransaction entr = Persistor.getEntityManager().getTransaction();
			entr.begin();
			Query query = Persistor.getEntityManager().createQuery("DELETE FROM SushiProcess");
			int deleteRecords = query.executeUpdate();
			entr.commit();
			System.out.println(deleteRecords + " records are deleted.");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	public TimeCondition getTimeCondition() {
		return timeCondition;
	}

	public void setTimeCondition(TimeCondition timeCondition) {
		this.timeCondition = timeCondition;
		if(timeCondition != null){
			this.timeCondition.setProcess(this);
		}
	}

	public BPMNProcess getBpmnProcess() {
		return bpmnProcess;
	}

	public void setBpmnProcess(BPMNProcess bpmnProcess) {
		this.bpmnProcess = bpmnProcess;
	}

	/**
	 * Searches for the specified process and returns true if it exists.
	 * @param name
	 * @return
	 */
	public static boolean exists(String name) {
		return !SushiProcess.findByName(name).isEmpty();
	}
	
	/**
	 * Returns true, if the process has a correlation rule.
	 * @return
	 */
	public boolean hasCorrelation(){
		return !correlationAttributes.isEmpty() || !correlationRules.isEmpty();
	}

	/**
	 * Returns a decompositional tree of the contained BPMN process, if any.
	 * @return
	 */
	public SushiTree<AbstractBPMNElement> getProcessDecompositionTree() {
		return processDecompositionTree;
	}

	public void setProcessDecompositionTree(SushiTree<AbstractBPMNElement> processDecompositionTree) {
		this.processDecompositionTree = processDecompositionTree;
	}
}
