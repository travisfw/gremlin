package com.tinkerpop.gremlin.compiler.functions.sail;

import com.tinkerpop.blueprints.pgm.impls.sail.SailGraph;
import com.tinkerpop.gremlin.compiler.Atom;
import com.tinkerpop.gremlin.compiler.functions.AbstractFunction;
import com.tinkerpop.gremlin.compiler.functions.FunctionHelper;
import com.tinkerpop.gremlin.compiler.operations.Operation;

import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class NamespaceFunction extends AbstractFunction<String> {

    private final String FUNCTION_NAME = "ns";

    public Atom<String> compute(final List<Operation> parameters) throws RuntimeException {

        final int size = parameters.size();
        final SailGraph graph = (SailGraph) FunctionHelper.getGraph(parameters, 0);

        final String prefixURI;

        if (size == 1) {
            prefixURI = (String) parameters.get(0).compute().getValue();
        } else if (size == 2) {
            prefixURI = (String) parameters.get(1).compute().getValue();
        } else {
            throw new RuntimeException(this.createUnsupportedArgumentMessage());
        }

        return new Atom<String>(graph.expandPrefix(prefixURI));
    }

    public String getFunctionName() {
        return this.FUNCTION_NAME;
    }
}
