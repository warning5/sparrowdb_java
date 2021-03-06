package org.sparrow.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import org.sparrow.cache.ICache;

/**
 * Created by mauricio on 4/16/16.
 */
public class CacheMetrics implements Metric
{
    public final Meter hits;
    public final Gauge<Long> capacity;
    public final Meter requests;
    public final Gauge<Double> hitRate;
    public final Gauge<Double> oneMinuteHitRate;
    public final Gauge<Double> fiveMinuteHitRate;
    public final Gauge<Double> fifteenMinuteHitRate;
    public final Gauge<Integer> entries;

    public CacheMetrics(MetricRegistry metrics, String name, final ICache cache)
    {
        capacity = metrics.register(name + "sparrow_cache_capacity", () -> cache.capacity());

        hits = metrics.meter(name + "sparrow_cache_hits");
        requests = metrics.meter(name + "sparrow_cache_requests");
        hitRate = metrics.register(name + "sparrow_cache_hitRate", new RatioGauge()
        {
            @Override
            public Ratio getRatio()
            {
                return Ratio.of(hits.getCount(), requests.getCount());
            }
        });

        oneMinuteHitRate = metrics.register(name + "sparrow_cache_oneMinuteHitRate", new RatioGauge()
        {
            protected Ratio getRatio()
            {
                return Ratio.of(hits.getOneMinuteRate(), requests.getOneMinuteRate());
            }
        });

        fiveMinuteHitRate = metrics.register(name + "sparrow_cache_fiveMinuteHitRate", new RatioGauge()
        {
            protected Ratio getRatio()
            {
                return Ratio.of(hits.getFiveMinuteRate(), requests.getFiveMinuteRate());
            }
        });

        fifteenMinuteHitRate = metrics.register(name + "sparrow_cache_fifteenMinuteHitRate", new RatioGauge()
        {
            protected Ratio getRatio()
            {
                return Ratio.of(hits.getFifteenMinuteRate(), requests.getFifteenMinuteRate());
            }
        });

        entries = metrics.register(name + "sparrow_cache_entries", () -> cache.size());
    }
}
