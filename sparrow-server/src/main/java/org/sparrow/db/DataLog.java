package org.sparrow.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sparrow.common.DataDefinition;
import org.sparrow.common.util.FileUtils;
import org.sparrow.config.DatabaseConfig;
import org.sparrow.config.DatabaseDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Created by mauricio on 07/01/2016.
 */
public final class DataLog extends DataFile
{
    private static Logger logger = LoggerFactory.getLogger(DataLog.class);
    private Set<DataHolder> dataHolders;
    private AtomicLong currentSize = new AtomicLong();
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static final String DEFAULT_DATAHOLDER_FILE = "datalog.spw";
    private ExecutorService executor = Executors.newCachedThreadPool();

    public DataLog(Set<DataHolder> dataHolders, DatabaseConfig.Descriptor descriptor)
    {
        super();
        this.descriptor = descriptor;
        this.dataHolders = dataHolders;
        this.filename = FileUtils.joinPath(descriptor.path, DEFAULT_DATAHOLDER_FILE);
        dataHolderProxy = new DataHolderProxy(this.filename);
    }

    private void append(DataDefinition dataDefinition)
    {
        try {
            dataHolderProxy.append(dataDefinition);
            indexer.put(dataDefinition.getKey32(), dataDefinition.getOffset());
        } catch (IOException e) {
            logger.error("Could not append data to DataLog {}: {} ", filename, e.getMessage());
        }
    }

    public void add(DataDefinition dataDefinition)
    {
        if ((dataDefinition.getSize() + currentSize.get()) >= DatabaseDescriptor.config.max_datalog_size)
        {
            flush();
        }

        lock.writeLock().lock();
        try
        {
            append(dataDefinition);
            long size = currentSize.get();
            currentSize.set(size + dataDefinition.getSize());
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public void flush()
    {
        String nextFileName = DataFileManager.getNextDataHolderName(descriptor.path);
        logger.debug("Flushing data into {}", nextFileName);

        if (new File(filename).renameTo(new File(FileUtils.joinPath(descriptor.path, nextFileName))))
        {
            IndexSummary temp = new IndexSummary();
            temp.getIndexList().putAll(this.indexer.getIndexList());

            executor.execute(() -> {
                DataHolder dataHolder = DataHolder.create(nextFileName, descriptor, temp);
                dataHolders.add(dataHolder);
            });

            this.indexer.clear();

            currentSize.set(0);
            dataHolderProxy.open(filename);
        }

        logger.debug("-------------- End flushing");
    }

    public void load()
    {
        dataHolderProxy.iterateDataHolder(
                (dataDefinition, bytesRead) -> indexer.put(dataDefinition.getKey32(), dataDefinition.getOffset())
        );
    }
}