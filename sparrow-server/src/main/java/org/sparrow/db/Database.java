package org.sparrow.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sparrow.common.DataDefinition;
import org.sparrow.common.Tombstone;
import org.sparrow.common.util.FileUtils;
import org.sparrow.common.util.SPUtils;
import org.sparrow.config.DatabaseDescriptor;
import org.sparrow.protocol.DataObject;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by mauricio on 25/12/2015.
 */
public class Database
{
    private static Logger logger = LoggerFactory.getLogger(Database.class);
    private static final String FILENAME_EXTENSION = ".spw";
    private volatile Set<DataHolder> dataHolders;
    private volatile DataLog dataLog;
    private final String dbname;

    private Database(String dbname)
    {
        this.dbname = dbname;
        dataHolders = new LinkedHashSet<>();
        dataLog = new DataLog(dbname, dataHolders, DataFileManager.getDbPath(dbname, "datalog", FILENAME_EXTENSION));
    }

    public static Database build(String dbname)
    {
        Database database = null;
        try
        {
            FileUtils.createDirectory(DatabaseDescriptor.getDataFilePath() + dbname);
            database = new Database(dbname);
        }
        catch (Exception e)
        {
            e.getMessage();
        }
        return database;
    }

    public static Database open(String dbname)
    {
        Database database = new Database(dbname);

        if (!database.dataLog.isEmpty())
        {
            logger.debug("Loading datalog {} with size: {}", dbname, database.dataLog.getSize());
            database.dataLog.load();
        }

        DataFileManager.getDataHolders(dbname)
                .stream()
                .forEach(x -> {
                    if (DataFileManager.isValidDataHolder(x.getAbsolutePath()))
                    {
                        database.dataHolders.add(DataHolder.open(x.getAbsolutePath()));
                    }
                });

        return database;
    }


    public void close()
    {
        SparrowDatabase.cacheManager.clear();
        dataHolders.clear();
        dataLog.close();
    }

    public void insertData(DataObject object)
    {
        int hash32key = SPUtils.hash32(object.getKey());
        DataDefinition dataDefinition = new DataDefinition();
        dataDefinition.setKey(object.getKey());
        dataDefinition.setKey32(hash32key);

        /*
         *  As append only data file, the offset of new data is the
         *   the size of data file. It is updated when the data is
         *   written to the file.
        */
        dataDefinition.setOffset(0);

        // Get current time int UTC
        dataDefinition.setTimestamp(java.time.Instant.now().getEpochSecond());
        dataDefinition.setSize(object.bufferForData().capacity());
        dataDefinition.setExtension(object.getExtension().toLowerCase());
        dataDefinition.setState(DataDefinition.DataState.ACTIVE);
        dataDefinition.setBuffer(object.bufferForData().array());
        insertData(dataDefinition);

        // Put in cache
        SparrowDatabase.cacheManager.put(dataDefinition.getKey(), dataDefinition);
    }

    public void insertData(DataDefinition dataDefinition)
    {
        dataLog.add(dataDefinition);
    }

    public DataDefinition getDataWithImageByKey32(String dataKey)
    {
        DataDefinition dataDefinition = SparrowDatabase.cacheManager.get(dataKey);

        if (dataDefinition == null)
        {
            dataDefinition = dataLog.get(dataKey);
        }

        if (dataDefinition == null)
        {
            Iterator<DataHolder> iterDataHolder = dataHolders.stream()
                    .filter(x -> x.isKeyInFile(dataKey))
                    .iterator();

            while (iterDataHolder.hasNext())
            {
                dataDefinition = iterDataHolder.next().get(dataKey);
            }
        }

        if (dataDefinition != null)
        {
            SparrowDatabase.cacheManager.put(dataKey, dataDefinition);
        }

        return dataDefinition;
    }

    public boolean deleteData(String dataKey)
    {
        DataDefinition dataDefinition = getDataWithImageByKey32(dataKey);

        if (dataDefinition == null)
        {
            return false;
        }
        else
        {
            Tombstone tombstone = new Tombstone(dataDefinition);
            dataLog.add(tombstone);
            SparrowDatabase.cacheManager.put(dataKey, tombstone);
        }

        return true;
    }

    public Set<DataHolder> getDataHolders()
    {
        return dataHolders;
    }

    public DataLog getDataLog()
    {
        return dataLog;
    }

    public long countData()
    {
        long count = 0;

        count += dataLog.count();

        for (DataHolder dh : dataHolders)
        {
            count += dh.count();
        }

        return count;
    }
}