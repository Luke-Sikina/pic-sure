package edu.harvard.dbmi.avillach.data.repository;

import org.hibernate.Session;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @param <T> the type of the entity class
 * @param <K> the type of the primary key
 */
public class BaseRepository<T, K> {

	protected final Class<T> type;
	
	protected BaseRepository(Class<T> type){
		this.type = type;
	}

	@PersistenceContext
	protected EntityManager em;
	
	protected EntityManager em(){
		return em;
	}
	
	protected CriteriaBuilder cb() {
		return em().getCriteriaBuilder();
	}

	/**
	 *
	 * @return a criteriaQuery instance created by criteriaBuilder in entitiyManager
	 */
	public CriteriaQuery<T> query(){
		return cb().createQuery(type);
	}

	public Predicate eq(Root root, String columnName, Object value){
		return eq(cb(),root,columnName,value);
	}

	public Predicate eq(CriteriaBuilder cb, Root root, String columnName, Object value){
		return cb.equal(root.get(columnName), value);
	}

	public <V extends Number> Predicate lt(CriteriaBuilder cb, Root root, String columnName, V value){
		return cb.lt(root.get(columnName),value);
	}

	public <V extends Number> Predicate gt(CriteriaBuilder cb, Root root, String columnName, V value){
		return cb.gt(root.get(columnName),value);
	}

	public <V extends Comparable> Predicate between(CriteriaBuilder cb, Root root, String columnName, V value1, V value2){
		Expression<V> exp = root.get(columnName);
		return cb.between(exp,value1,value2);
	}

	public Predicate like(CriteriaBuilder cb, Root root, String columnName, String value){
		Expression<String> exp = root.get(columnName);
		return cb.like(exp, value);
	}

	public T getById(K id){
		return em().find(type, id);
	}

	/**
	 * assume the operator is eq
	 * @param columnName
	 * @param value
	 * @return
	 */
	public List<T> getByColumn(String columnName, Object value){
		Root root = root();
		return getByColumns(root, eq(cb(),root,columnName,value));
	}

	/**
	 * assume the operator is eq
	 * @param columnNameValueMap
	 * @return
	 */
	public List<T> getByColumns(Root root, Map<String, Object> columnNameValueMap){
		CriteriaBuilder cb = cb();
		List<Predicate> predicates = new ArrayList<>();
		for (Map.Entry<String, Object> entry : columnNameValueMap.entrySet()){
			predicates.add(eq(cb,root,entry.getKey(), entry.getValue()));
		}
		return getByColumns(root, (Predicate[]) predicates.toArray());
	}

	/**
	 * given the ability to assign your own predicates like lt, eq, like
	 * @param root
	 * @param predicates provide your own predicates
	 * @return
	 */
	public List<T> getByColumns(Root root, Predicate... predicates){

		CriteriaQuery<T> query = query();
		query.select(root);
		if (predicates != null)
			query.where(predicates);
		return em().createQuery(query)
				.getResultList();
	}

	public List<T> list(){
		return getByColumns(root());
	}

	protected Root<T> root(){
		return query().from(type);
	}
	
	public class InParam<S>{
		private String parameterName;
		private Class<S> parameterValueClass;
		private S parameterValue;
		
		public InParam(Class<S> type) {
			this.parameterValueClass = type;
		}
		public String getParameterName() {
			return parameterName;
		}
		public InParam<S> name(String parameterName) {
			this.parameterName = parameterName;
			return this;
		}
		public Class<S> getParameterValueClass() {
			return parameterValueClass;
		}
		public InParam<S> type(Class<S> parameterValueClass) {
			this.parameterValueClass = parameterValueClass;
			return this;
		}
		public S getParameterValue() {
			return parameterValue;
		}
		public InParam<S> value(S parameterValue) {
			this.parameterValue = parameterValue;
			return this;
		}
	}
	
	public InParam inParam(Class type){
		return new InParam(type);
	}
	
	public StoredProcedureQuery createQueryFor(String procedureName, Class entityType, InParam ... inParams){
		StoredProcedureQuery validationsQuery = 
				em().createStoredProcedureQuery(procedureName, entityType);
		for(InParam param : inParams){
			validationsQuery.registerStoredProcedureParameter(param.parameterName, param.parameterValueClass, ParameterMode.IN)
			.setParameter(param.parameterName, param.parameterValue);			
		}
		return validationsQuery;
	}

	public void persist(T t){
		em().persist(t);
	}

	public void remove(T t){
		em().remove(t);
	}


}
