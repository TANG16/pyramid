package edu.neu.ccs.pyramid.optimization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import java.util.Iterator;
import java.util.LinkedList;


/**
 * Numerical Optimization, Second Edition, Jorge Nocedal Stephen J. Wright
 * Algorithm 7.4 and 7.5
 * Liu, Tao-Wen.
 * "A regularized limited memory BFGS method for nonconvex unconstrained minimization."
 * Numerical Algorithms 65.2 (2014): 305-323.
 * Formula 2.7
 * Created by chengli on 12/9/14.
 */
public class LBFGS {
    private static final Logger logger = LogManager.getLogger();
    private Optimizable.ByGradientValue function;
    private BackTrackingLineSearcher lineSearcher;
    /**
     * history length;
     */
    private double m = 5;
    private LinkedList<Vector> sQueue;
    private LinkedList<Vector> yQueue;
    private LinkedList<Double> rhoQueue;
    /**
     * stop condition
     */
    private double epsilon = 0.1;

    public LBFGS(Optimizable.ByGradientValue function) {
        this.function = function;
        this.lineSearcher = new BackTrackingLineSearcher(function);
        lineSearcher.setInitialStepLength(1);
        this.sQueue = new LinkedList<>();
        this.yQueue = new LinkedList<>();
        this.rhoQueue = new LinkedList<>();
    }

    public void optimize(){
        LinkedList<Double> valueQueue = new LinkedList<>();
        valueQueue.add(function.getValue(function.getParameters()));
        if (logger.isDebugEnabled()){
            logger.debug("initial value = "+function.getValue(function.getParameters()));
        }
        iterate();
        if (logger.isDebugEnabled()){
            logger.debug("value = "+function.getValue(function.getParameters()));
        }
        valueQueue.add(function.getValue(function.getParameters()));
        while(true){
//            System.out.println("objective = "+valueQueue.getLast());
            if (Math.abs(valueQueue.getFirst()-valueQueue.getLast())<epsilon){
                break;
            }
            iterate();
            valueQueue.remove();
            valueQueue.add(function.getValue(function.getParameters()));
            if (logger.isDebugEnabled()){
                logger.debug("value = "+function.getValue(function.getParameters()));
            }
        }

    }

    public void iterate(){
        if (logger.isDebugEnabled()){
            logger.debug("start one iteration");
        }
        Vector parameters = function.getParameters();
        if (logger.isDebugEnabled()){
            logger.debug("current parameters = "+parameters);
        }
        Vector oldGradient = function.getGradient();
        Vector direction = findDirection();
        if (logger.isDebugEnabled()){
            logger.debug("search direction = "+direction);
        }
//        System.out.println("doing line search");
        double stepLength = lineSearcher.findStepLength(direction);
        if (logger.isDebugEnabled()){
            logger.debug("step length = "+stepLength);
        }
//        System.out.println("line search done");
        Vector s = direction.times(stepLength);
        Vector updatedParams = parameters.plus(s);
        parameters.assign(updatedParams);
        if (logger.isDebugEnabled()){
            logger.debug("updated parameters = "+parameters);
        }
        function.refresh();
        Vector newGradient = function.getGradient();
        Vector y = newGradient.minus(oldGradient);
        double denominator = y.dot(s);

        double rho = 0;
        if (denominator>0){
            rho = 1/denominator;
        }


        if (logger.isDebugEnabled()){
            logger.debug("rho = "+rho);
        }
        sQueue.add(s);
        yQueue.add(y);
        rhoQueue.add(rho);
        if (sQueue.size()>m){
            sQueue.remove();
            yQueue.remove();
            rhoQueue.remove();
        }
        if (logger.isDebugEnabled()){
            logger.debug("finish one iteration");
        }
    }

    Vector findDirection(){
        Vector g = function.getGradient();
        Vector q = new DenseVector(g.size());
        q.assign(g);
        Iterator<Double> rhoDesIterator = rhoQueue.descendingIterator();
        Iterator<Vector> sDesIterator = sQueue.descendingIterator();
        Iterator<Vector> yDesIterator = yQueue.descendingIterator();

        LinkedList<Double> alphaQueue = new LinkedList<>();

        while(rhoDesIterator.hasNext()){
            double rho = rhoDesIterator.next();
            Vector s = sDesIterator.next();
            Vector y = yDesIterator.next();
            double alpha = s.dot(q) * rho;
            alphaQueue.addFirst(alpha);
            //seems no need to use "assign"
            q = q.minus(y.times(alpha));
        }

        double gamma = gamma();
        //use H_k^0 = gamma I
        Vector r = q.times(gamma);
        Iterator<Double> rhoIterator = rhoQueue.iterator();
        Iterator<Vector> sIterator = sQueue.iterator();
        Iterator<Vector> yIterator = yQueue.iterator();
        Iterator<Double> alphaIterator = alphaQueue.iterator();
        while(rhoIterator.hasNext()){
            double rho = rhoIterator.next();
            Vector s = sIterator.next();
            Vector y = yIterator.next();
            double alpha = alphaIterator.next();
            double beta = y.dot(r) * rho;
            r = r.plus(s.times(alpha - beta));
        }

        return r.times(-1);
    }

    /**
     * scaling factor
     * @return
     */
    double gamma(){
        if (sQueue.isEmpty()){
            return 1;
        }
        Vector s = sQueue.getLast();
        Vector y = yQueue.getLast();
        double denominator = y.dot(y);
        if (denominator<=0){
            return 1;
        }
        return (s.dot(y)) / (y.dot(y));
    }

    public void setHistory(double m) {
        this.m = m;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }
}
