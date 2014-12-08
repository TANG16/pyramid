package edu.neu.ccs.pyramid.optimization;

import org.apache.mahout.math.Vector;

/**
 * Created by chengli on 12/7/14.
 * inspired by the design of mallet
 */
public interface Optimizable {
    Vector getParameters();

    public interface ByGradient extends Optimizable {
        Vector getGradient();
    }

}
