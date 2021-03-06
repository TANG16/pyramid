package edu.neu.ccs.pyramid.regression.least_squares_boost;


import edu.neu.ccs.pyramid.dataset.DataSetType;
import edu.neu.ccs.pyramid.dataset.RegDataSet;
import edu.neu.ccs.pyramid.dataset.TRECFormat;
import edu.neu.ccs.pyramid.eval.RMSE;
import edu.neu.ccs.pyramid.feature.FeatureList;
import edu.neu.ccs.pyramid.regression.ConstantRegressor;
import edu.neu.ccs.pyramid.regression.regression_tree.RegTreeConfig;
import edu.neu.ccs.pyramid.regression.regression_tree.RegTreeFactory;
import edu.neu.ccs.pyramid.regression.regression_tree.RegressionTree;
import org.dmg.pmml.PMML;
import org.jpmml.model.MetroJAXBUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class PMMLConverterTest {
    public static void main(String[] args) throws Exception{

        RegDataSet trainSet = TRECFormat.loadRegDataSet(new File("/Users/chengli/Dropbox/Public/pyramid/abalone//train"), DataSetType.REG_DENSE,true);

        RegDataSet testSet = TRECFormat.loadRegDataSet(new File("/Users/chengli/Dropbox/Public/pyramid/abalone//test"),DataSetType.REG_DENSE,true);


        LSBoost lsBoost = new LSBoost();

        RegTreeConfig regTreeConfig = new RegTreeConfig().setMaxNumLeaves(3);
        RegTreeFactory regTreeFactory = new RegTreeFactory(regTreeConfig);
        LSBoostOptimizer optimizer = new LSBoostOptimizer(lsBoost, trainSet, regTreeFactory);
        optimizer.setShrinkage(0.1);
        optimizer.initialize();

        for (int i=0;i<10;i++){
            System.out.println("iteration "+i);
            System.out.println("train RMSE = "+ RMSE.rmse(lsBoost, trainSet));
            System.out.println("test RMSE = "+ RMSE.rmse(lsBoost, testSet));
            optimizer.iterate();
        }
        FeatureList featureList = trainSet.getFeatureList();
        List<RegressionTree> regressionTrees = lsBoost.getEnsemble(0).getRegressors().stream()
                .filter(a->a instanceof RegressionTree).map(a->(RegressionTree)a).collect(Collectors.toList());
        System.out.println(regressionTrees);

        double constant = ((ConstantRegressor)lsBoost.getEnsemble(0).get(0)).getScore();
        PMML pmml = PMMLConverter.encodePMML(null, null, featureList, regressionTrees, (float)constant);

        System.out.println(pmml.toString());

        try(OutputStream os = new FileOutputStream("/Users/chengli/tmp/pmml.xml")){
            MetroJAXBUtil.marshalPMML(pmml, os);
        }
    }

}