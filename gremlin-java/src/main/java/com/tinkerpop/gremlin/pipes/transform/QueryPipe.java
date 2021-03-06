package com.tinkerpop.gremlin.pipes.transform;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Query;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.AbstractPipe;
import com.tinkerpop.pipes.util.PipeHelper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * QueryPipe makes use of the Vertex.query() method in Blueprints which allows for intelligent edge look ups from the underlying graph.
 * Note that QueryPipe is automatically constructed by a GremlinPipeline when a pattern of the following is seen:
 * <p>outE(x).has(x).interval(x).xV()</p>
 * The final xV() can be either inV(), outV(), or bothV().
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class QueryPipe<E extends Element> extends AbstractPipe<Vertex, E> {

    private Direction direction = Direction.BOTH;
    private String[] labels;
    private List<HasContainer> hasContainers;
    private List<IntervalContainer> intervalContainers;
    private final Class<E> elementClass;
    private final long limit;

    private Iterator<E> currentIterator = PipeHelper.emptyIterator();

    /**
     * Construct a new Query pipe that wraps an underlying Blueprints Query object.
     * Given the optional nature of many of the parameters, note the "wildcard" settings for each parameter.
     *
     * @param resultingElementClass this must be either Vertex.class or Edge.class (anything else will throw an IllegalArgumentException)
     * @param direction             this must be a legal direction representing the direction of the edge.
     * @param hasContainers         this must be a collection of 'has'-filters (i.e. property filters). Provide an empty list if no such filters are to be applied.
     * @param intervalContainers    this must be a collection of 'interval'-filters (i.e. property filters within a range). Provide an empty list if no such filters are to be applied.
     * @param limit                 this must be a long value which limits the number of elements returns. Use -1 if no such limit is desired.
     * @param labels                this is a list of Strings representing the edge label filters to apply. Do not provide any Strings if no such filtering is desired.
     */
    public QueryPipe(final Class<E> resultingElementClass, final Direction direction, final List<HasContainer> hasContainers, final List<IntervalContainer> intervalContainers, final long limit, final String... labels) {
        this.elementClass = resultingElementClass;
        this.direction = direction;
        this.hasContainers = hasContainers;
        this.intervalContainers = intervalContainers;
        this.labels = labels;
        this.limit = limit;

        if (!resultingElementClass.equals(Vertex.class) && !resultingElementClass.equals(Edge.class))
            throw new IllegalArgumentException("The provided element class must be either Vertex or Edge");
    }

    public void reset() {
        super.reset();
        this.currentIterator = PipeHelper.emptyIterator();
    }

    public String toString() {
        final String extra = "has:" + (this.hasContainers.size() > 0) + ",interval:" + (this.intervalContainers.size() > 0) + ",limit:" + this.limit;
        return PipeHelper.makePipeString(this, this.direction.name().toLowerCase(), Arrays.asList(this.labels), extra, this.elementClass.getSimpleName().toLowerCase());
    }

    public E processNextStart() {
        while (true) {
            if (this.currentIterator.hasNext())
                return currentIterator.next();
            else {
                final Vertex vertex = this.starts.next();
                Query query = vertex.query();
                query = query.direction(this.direction);
                if (this.labels.length > 0)
                    query = query.labels(this.labels);
                if (this.hasContainers.size() > 0) {
                    for (final HasContainer hasContainer : hasContainers) {
                        if (hasContainer.compare.equals(Query.Compare.EQUAL))
                            query = query.has(hasContainer.key, hasContainer.value);
                        else
                            query = query.has(hasContainer.key, (Comparable) hasContainer.value, hasContainer.compare);
                    }
                }
                if (this.intervalContainers.size() > 0) {
                    for (final IntervalContainer intervalContainer : intervalContainers) {
                        query = query.interval(intervalContainer.key, (Comparable) intervalContainer.startValue, (Comparable) intervalContainer.endValue);
                    }
                }
                if (this.limit > -1) {
                    query = query.limit(this.limit);
                }
                if (this.elementClass.equals(Vertex.class))
                    this.currentIterator = (Iterator<E>) query.vertices().iterator();
                else
                    this.currentIterator = (Iterator<E>) query.edges().iterator();
            }
        }
    }

    public static class HasContainer {
        public String key;
        public Object value;
        public Query.Compare compare;

        public HasContainer(final String key, final Object value, final Query.Compare compare) {
            this.key = key;
            this.value = value;
            this.compare = compare;
        }
    }

    public static class IntervalContainer {
        public String key;
        public Object startValue;
        public Object endValue;

        public IntervalContainer(final String key, final Object startValue, final Object endValue) {
            this.key = key;
            this.startValue = startValue;
            this.endValue = endValue;
        }
    }

}
