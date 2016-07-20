// DO NOT EDIT.  Make changes to TestEntity.java instead.
package er.test.eof.concurrency.data;

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.*;
import java.util.*;

import er.extensions.eof.*;
import er.extensions.foundation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public abstract class _TestEntity extends  ERXGenericRecord {
  public static final String ENTITY_NAME = "TestEntity";

  // Attribute Keys
  public static final ERXKey<Integer> COUNT = new ERXKey<Integer>("count");

  // Relationship Keys

  // Attributes
  public static final String COUNT_KEY = COUNT.key();

  // Relationships

  private static final Logger log = LoggerFactory.getLogger(_TestEntity.class);

  public TestEntity localInstanceIn(EOEditingContext editingContext) {
    TestEntity localInstance = (TestEntity)EOUtilities.localInstanceOfObject(editingContext, this);
    if (localInstance == null) {
      throw new IllegalStateException("You attempted to localInstance " + this + ", which has not yet committed.");
    }
    return localInstance;
  }

  public Integer count() {
    return (Integer) storedValueForKey(_TestEntity.COUNT_KEY);
  }

  public void setCount(Integer value) {
    log.debug( "updating count from {} to {}", count(), value);
    takeStoredValueForKey(value, _TestEntity.COUNT_KEY);
  }


  public static TestEntity createTestEntity(EOEditingContext editingContext, Integer count
) {
    TestEntity eo = (TestEntity) EOUtilities.createAndInsertInstance(editingContext, _TestEntity.ENTITY_NAME);
    eo.setCount(count);
    return eo;
  }

  public static ERXFetchSpecification<TestEntity> fetchSpec() {
    return new ERXFetchSpecification<TestEntity>(_TestEntity.ENTITY_NAME, null, null, false, true, null);
  }

  public static NSArray<TestEntity> fetchAllTestEntities(EOEditingContext editingContext) {
    return _TestEntity.fetchAllTestEntities(editingContext, null);
  }

  public static NSArray<TestEntity> fetchAllTestEntities(EOEditingContext editingContext, NSArray<EOSortOrdering> sortOrderings) {
    return _TestEntity.fetchTestEntities(editingContext, null, sortOrderings);
  }

  public static NSArray<TestEntity> fetchTestEntities(EOEditingContext editingContext, EOQualifier qualifier, NSArray<EOSortOrdering> sortOrderings) {
    ERXFetchSpecification<TestEntity> fetchSpec = new ERXFetchSpecification<TestEntity>(_TestEntity.ENTITY_NAME, qualifier, sortOrderings);
    NSArray<TestEntity> eoObjects = fetchSpec.fetchObjects(editingContext);
    return eoObjects;
  }

  public static TestEntity fetchTestEntity(EOEditingContext editingContext, String keyName, Object value) {
    return _TestEntity.fetchTestEntity(editingContext, ERXQ.equals(keyName, value));
  }

  public static TestEntity fetchTestEntity(EOEditingContext editingContext, EOQualifier qualifier) {
    NSArray<TestEntity> eoObjects = _TestEntity.fetchTestEntities(editingContext, qualifier, null);
    TestEntity eoObject;
    int count = eoObjects.count();
    if (count == 0) {
      eoObject = null;
    }
    else if (count == 1) {
      eoObject = eoObjects.objectAtIndex(0);
    }
    else {
      throw new IllegalStateException("There was more than one TestEntity that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static TestEntity fetchRequiredTestEntity(EOEditingContext editingContext, String keyName, Object value) {
    return _TestEntity.fetchRequiredTestEntity(editingContext, ERXQ.equals(keyName, value));
  }

  public static TestEntity fetchRequiredTestEntity(EOEditingContext editingContext, EOQualifier qualifier) {
    TestEntity eoObject = _TestEntity.fetchTestEntity(editingContext, qualifier);
    if (eoObject == null) {
      throw new NoSuchElementException("There was no TestEntity that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static TestEntity localInstanceIn(EOEditingContext editingContext, TestEntity eo) {
    TestEntity localInstance = (eo == null) ? null : ERXEOControlUtilities.localInstanceOfObject(editingContext, eo);
    if (localInstance == null && eo != null) {
      throw new IllegalStateException("You attempted to localInstance " + eo + ", which has not yet committed.");
    }
    return localInstance;
  }
}
