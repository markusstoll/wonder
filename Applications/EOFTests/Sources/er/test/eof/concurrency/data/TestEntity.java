package er.test.eof.concurrency.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;

import er.extensions.eof.ERXFetchSpecification;
import er.test.eof.concurrency.data._TestEntity;

public class TestEntity extends _TestEntity {
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(TestEntity.class);
    
    @Override
    public String toString()
    {
        return "<" + getClass().getSimpleName() + " pk:\"" + primaryKey() + " count:\"" + count() + "\">";
    }
    
    public static NSArray<TestEntity> fetchRefreshedAllTestEntities(EOEditingContext editingContext) 
    {
        ERXFetchSpecification<TestEntity> fetchSpec = new ERXFetchSpecification<TestEntity>(_TestEntity.ENTITY_NAME, null, null);
        fetchSpec.setRefreshesRefetchedObjects(true);
        NSArray<TestEntity> eoObjects = fetchSpec.fetchObjects(editingContext);

        return eoObjects;
    }
}
