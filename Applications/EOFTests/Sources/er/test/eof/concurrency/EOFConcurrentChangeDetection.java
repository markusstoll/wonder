package er.test.eof.concurrency;

import java.io.File;

import org.h2.tools.Server;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSPropertyListSerialization;

import er.extensions.ERXExtensions;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXObjectStoreCoordinator;
import er.test.eof.concurrency.data.TestEntity;
import er.test.eof.concurrency.migration.EOFConcurrentChange0;

public class EOFConcurrentChangeDetection
{
    static Server h2TestServer = null;
    static int h2ServerPort = 9123;
    static String connectionDict = "{\n" +
        "    URL = \"jdbc:h2:tcp://localhost:" + h2ServerPort + "/mem:test\";\n" + 
        "    driver = \"org.h2.Driver\";\n" + 
        "    username = sa;" + 
        "    plugin = H2PlugIn;"+
        "}";

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setupDatabase() throws Throwable
    {
        File mainBundleFolder = ERXExtensions.mainBundleFolder();
        ERXExtensions.initEOF(mainBundleFolder, new String[] {});
        h2TestServer = Server.createTcpServer("-tcpPort", "" + h2ServerPort, "-tcpAllowOthers").start();
        
        Object configValues = NSPropertyListSerialization.propertyListFromString(connectionDict);
        EOModel model = EOModelGroup.defaultGroup().modelNamed("EOFConcurrentChange");
        model.setConnectionDictionary((NSDictionary<String, Object>) configValues);
        
        EOEditingContext ec = ERXEC.newEditingContext();
        EODatabaseContext dbc = EOUtilities.databaseContextForModelNamed(ec, model.name());
        new EOFConcurrentChange0().upgrade(ec, dbc.availableChannel().adaptorChannel(), model);
        
        TestEntity.createTestEntity(ec, 10);
        TestEntity.createTestEntity(ec, 11);
        
        ec.saveChanges();
    }

    @AfterClass 
    public static void shutdownDatabase()
    {
        h2TestServer.stop(); 
    }

    @Test
    public void innerInstanceNoConcurrentChange()
    {
        EOEditingContext ec1 = ERXEC.newEditingContext();
        EOEditingContext ec2 = ERXEC.newEditingContext();
        
        ECDelegate ec1Delegate = new ECDelegate();
        ECDelegate ec2Delegate = new ECDelegate();
        ec1.setDelegate(ec1Delegate);
        ec2.setDelegate(ec2Delegate);
        
        TestEntity te1 = TestEntity.fetchRefreshedAllTestEntities(ec1).get(0);
        TestEntity te2 = TestEntity.fetchAllTestEntities(ec2).get(1);
        
        te1.setCount(21);
        te2.setCount(22);
        
        if(ec1Delegate.didMergeChanges())
        {
        	Assert.fail("concurrent change");
        }
        ec1.saveChanges();
        
        if(ec2Delegate.didMergeChanges())
        {
        	Assert.fail("concurrent change");
        }
        ec2.saveChanges();

        NSArray<TestEntity> testEntities;

        testEntities = TestEntity.fetchRefreshedAllTestEntities(ERXEC.newEditingContext());
        Assert.assertEquals(21L, (long)testEntities.get(0).count());
        Assert.assertEquals(22L, (long)testEntities.get(1).count());
        
        testEntities = TestEntity.fetchRefreshedAllTestEntities(ec1);
        Assert.assertEquals(21L, (long)testEntities.get(0).count());
        Assert.assertEquals(22L, (long)testEntities.get(1).count());

        testEntities = TestEntity.fetchRefreshedAllTestEntities(ec2);
        Assert.assertEquals(21L, (long)testEntities.get(0).count());
        Assert.assertEquals(22L, (long)testEntities.get(1).count());
    }

    @Test
    public void innerInstanceConcurrentChange()
    {
        EOEditingContext ec1 = ERXEC.newEditingContext();
        EOEditingContext ec2 = ERXEC.newEditingContext();
        
        ECDelegate ec1Delegate = new ECDelegate();
        ECDelegate ec2Delegate = new ECDelegate();
        ec1.setDelegate(ec1Delegate);
        ec2.setDelegate(ec2Delegate);

        TestEntity te1 = TestEntity.fetchRefreshedAllTestEntities(ec1).get(0);
        te1.setCount(0);
        ec1.saveChanges();
        
        te1 = TestEntity.fetchAllTestEntities(ec1).get(0);
        TestEntity te2 = TestEntity.fetchAllTestEntities(ec2).get(0);
        
        te1.setCount(31);
        te2.setCount(32);

        Exception ex = null;

        try
        {
            if(ec1Delegate.didMergeChanges())
            {
            	throw new Exception("concurrent change");
            }

            ec1.saveChanges();
        } catch (Exception e)
        {
            ex = e;
        }
        Assert.assertNull("database concurrent change detected, which is wrong!", ex);

        ex = null;
        try
        {
            if(ec2Delegate.didMergeChanges())
            {
            	throw new Exception("concurrent change");
            }

            ec2.saveChanges();
        } catch (Exception e)
        {
            ex = e;
        }
        Assert.assertNotNull("database concurrent change not detected!", ex);

        ec2.revert();

        NSArray<TestEntity> testEntities;
        
        testEntities = TestEntity.fetchRefreshedAllTestEntities(ec1);
        Assert.assertEquals(31L, (long)testEntities.get(0).count());

        testEntities = TestEntity.fetchRefreshedAllTestEntities(ec2);
        Assert.assertEquals(31L, (long)testEntities.get(0).count());
    }

    @Test
    public void crossInstanceConcurrentChange()
    {
        EOEditingContext ec1 = ERXEC.newEditingContext(new ERXObjectStoreCoordinator(true) );
        EOEditingContext ec2 = ERXEC.newEditingContext(new ERXObjectStoreCoordinator(true) );
        
        TestEntity te1 = TestEntity.fetchAllTestEntities(ec1).get(0);
        te1.setCount(0);
        ec1.saveChanges();
        
        TestEntity te2 = TestEntity.fetchAllTestEntities(ec2).get(0);
        
        te1.setCount(1);
        te2.setCount(2);
        
        ec1.saveChanges();
        Exception ex = null;
        
        try
        {
            ec2.saveChanges();
        } catch (Exception e)
        {
            ex = e;
        }
        
        Assert.assertNotNull("database concurrent change not detected!", ex);

        TestEntity te3 = TestEntity.fetchAllTestEntities(ec1).get(0);
        
        Assert.assertEquals(1L, (long)te3.count());
    }
    
    public class ECDelegate implements EOEditingContext.Delegate
    {
        private boolean didMergeChanges = false;
        
        public void resetMergeChangeMarker()
        {
            didMergeChanges = false;
        }
        
        public boolean didMergeChanges()
        {
            return didMergeChanges;
        }

        @Override
        public boolean editingContextShouldPresentException(EOEditingContext paramEOEditingContext, Throwable paramThrowable)
        {
            return false;
        }

        @Override
        public boolean editingContextShouldValidateChanges(EOEditingContext paramEOEditingContext)
        {
            return true;
        }

        @Override
        public void editingContextWillSaveChanges(EOEditingContext paramEOEditingContext)
        {
        }

        @Override
        public boolean editingContextShouldInvalidateObject(EOEditingContext paramEOEditingContext, EOEnterpriseObject paramEOEnterpriseObject, EOGlobalID paramEOGlobalID)
        {
            return true;
        }

        @Override
        public boolean editingContextShouldUndoUserActionsAfterFailure(EOEditingContext paramEOEditingContext)
        {
            return false;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public NSArray editingContextShouldFetchObjects(EOEditingContext paramEOEditingContext, EOFetchSpecification paramEOFetchSpecification)
        {
            return null;
        }

        @Override
        public boolean editingContextShouldMergeChangesForObject(EOEditingContext paramEOEditingContext, EOEnterpriseObject paramEOEnterpriseObject)
        {
            didMergeChanges = true;

            return true;
        }

        @Override
        public void editingContextDidMergeChanges(EOEditingContext paramEOEditingContext)
        {
        }
        
    }
}
