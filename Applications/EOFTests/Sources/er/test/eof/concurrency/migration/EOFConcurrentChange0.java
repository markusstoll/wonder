package er.test.eof.concurrency.migration;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;

import er.extensions.migration.ERXMigrationDatabase;
import er.extensions.migration.ERXMigrationTable;
import er.extensions.migration.ERXModelVersion;

public class EOFConcurrentChange0 extends ERXMigrationDatabase.Migration {
    @Override
    public NSArray<ERXModelVersion> modelDependencies() {
        return null;
    }
  
    @Override
    public void downgrade(EOEditingContext editingContext, ERXMigrationDatabase database) throws Throwable {
        // DO NOTHING
    }

    @Override
    public void upgrade(EOEditingContext editingContext, ERXMigrationDatabase database) throws Throwable {
        ERXMigrationTable tesT_ENTITYTable = database.newTableNamed("TEST_ENTITY");
        tesT_ENTITYTable.newIntegerColumn("COUNT", NOT_NULL);
        tesT_ENTITYTable.newIntegerColumn("ID", NOT_NULL);
        tesT_ENTITYTable.create();
        tesT_ENTITYTable.setPrimaryKey("ID");

    }
}