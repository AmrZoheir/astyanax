package com.netflix.astyanax.query;

import java.nio.ByteBuffer;
import java.util.Collection;

import com.netflix.astyanax.ExceptionCallback;
import com.netflix.astyanax.Execution;
import com.netflix.astyanax.RowCallback;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ByteBufferRange;
import com.netflix.astyanax.model.ColumnSlice;
import com.netflix.astyanax.model.Rows;

/**
 * Specialized query to iterate the contents of a column family.
 * 
 * ColumnFamily<String, String> CF_STANDARD1 = new ColumnFamily<String,
 * String>("Standard1", StringSerializer.get(), StringSerializer.get());
 * 
 * Iterator<Row<String,String>> iter =
 * keyspace.prepareQuery(MockConstants.CF_STANDARD1).iterator(); while
 * (iter.hasNext()) { Row<String,String> row = iter.next(); LOG.info("ROW: " +
 * row.getKey()); }
 * 
 * The iterator is implemented by making 'paginated' queries to Cassandra with
 * each query returning up to a the block size set by setBlockSize (default is
 * 10). The incremental query is hidden from the caller thereby providing a
 * virtual view into the column family.
 * 
 * There are a few important implementation details that need to be considered.
 * This implementation assumes the random partitioner is used. Consequently the
 * KeyRange query is done using tokens and not row keys. This is done because
 * when using the random partitioner tokens are sorted while keys are not.
 * However, because multiple keys could potentially map to the same token each
 * incremental query to Cassandra will repeat the last token from the previous
 * response. This will ensure that no keys are skipped. This does however have
 * to very important implications. First, the last and potentially more (if they
 * have the same token) row keys from the previous response will repeat. Second,
 * if a range of repeating tokens is larger than the block size then the code
 * will enter an infinite loop. This can be mitigated by selecting a block size
 * that is large enough so that the likelyhood of this happening is very low.
 * Also, if your application can tolerate the potential for skipped row keys
 * then call setRepeatLastToken(false) to turn off this features.
 * 
 * @author elandau
 * 
 * @param <K>
 * @param <C>
 */
public interface AllRowsQuery<K, C> extends Execution<Rows<K, C>> {
    /**
     * @deprecated Use setRowLimit instead
     */
    AllRowsQuery<K, C> setBlockSize(int blockSize);

    /**
     * Maximum number of rows to return for each incremental query to Cassandra.
     * This limit also represents the page size when paginating.
     * 
     * @param blockSize
     * @return
     */
    AllRowsQuery<K, C> setRowLimit(int rowLimit);

    /**
     * Sets the exception handler to use when handling exceptions inside
     * Iterator.next(). This gives the caller a chance to implement a backoff
     * strategy or stop the iteration.
     * 
     * @param cb
     * @return
     */
    AllRowsQuery<K, C> setExceptionCallback(ExceptionCallback cb);

    /**
     * If true will repeat the last token in the previous block.
     * 
     * @param repeatLastToken
     * @return
     */
    AllRowsQuery<K, C> setRepeatLastToken(boolean repeatLastToken);

    /**
     * Specify a non-contiguous set of columns to retrieve.
     * 
     * @param columns
     * @return
     */
    AllRowsQuery<K, C> withColumnSlice(C... columns);

    /**
     * Specify a non-contiguous set of columns to retrieve.
     * 
     * @param columns
     * @return
     */
    AllRowsQuery<K, C> withColumnSlice(Collection<C> columns);

    /**
     * Use this when your application caches the column slice.
     * 
     * @param slice
     * @return
     */
    AllRowsQuery<K, C> withColumnSlice(ColumnSlice<C> columns);

    /**
     * Specify a range of columns to return.
     * 
     * @param startColumn
     *            First column in the range
     * @param endColumn
     *            Last column in the range
     * @param reversed
     *            True if the order should be reversed. Note that for reversed,
     *            startColumn should be greater than endColumn.
     * @param count
     *            Maximum number of columns to return (similar to SQL LIMIT)
     * @return
     */
    AllRowsQuery<K, C> withColumnRange(C startColumn, C endColumn, boolean reversed, int count);

    /**
     * Specify a range and provide pre-constructed start and end columns. Use
     * this with Composite columns
     * 
     * @param startColumn
     * @param endColumn
     * @param reversed
     * @param count
     * @return
     */
    AllRowsQuery<K, C> withColumnRange(ByteBuffer startColumn, ByteBuffer endColumn, boolean reversed, int count);

    /**
     * Specify a range of composite columns. Use this in conjunction with the
     * AnnotatedCompositeSerializer.buildRange().
     * 
     * @param range
     * @return
     */
    AllRowsQuery<K, C> withColumnRange(ByteBufferRange range);

    /**
     * Execute the operation in a separate thread for each token range and
     * provide the results in a callback.
     * 
     * @param predicate
     * @return
     * @throws ConnectionException
     */
    void executeWithCallback(RowCallback<K, C> callback) throws ConnectionException;
}
