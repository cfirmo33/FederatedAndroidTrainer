package com.mccorby.federatedlearning.model;

import com.mccorby.federatedlearning.datasource.TrainerDataSource;

import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class IrisModel implements FederatedModel {

    private static final int BATCH_SIZE = 150;
    private static final int NUM_CLASSES = 3;

    private MultiLayerNetwork model;
    private String id;
    private IterationListener iterationListener;

    public IrisModel(String id, IterationListener iterationListener) {
        this.id = id;

        this.iterationListener = iterationListener;
    }

    public void buildModel() {
        final int numInputs = 4;
        int iterations = 1000;
        long seed = 6;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .activation(Activation.TANH)
                .weightInit(WeightInit.XAVIER)
                .learningRate(0.1)
                .regularization(true).l2(1e-4)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(3)
                        .build())
                .layer(1, new DenseLayer.Builder().nIn(3).nOut(3)
                        .build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .activation(Activation.SOFTMAX)
                        .nIn(3).nOut(NUM_CLASSES).build())
                .backprop(true).pretrain(false)
                .build();

        //run the model
        model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(iterationListener);

    }

    public void train(TrainerDataSource trainingData) {
        model.fit(trainingData.getTrainingData(BATCH_SIZE));
    }

    public String evaluate(TrainerDataSource dataSource) {
        //evaluate the model on the test set
        DataSet testData = dataSource.getTestData(BATCH_SIZE);
        double score = model.score(testData);
        Evaluation eval = new Evaluation(NUM_CLASSES);
        INDArray output = model.output(testData.getFeatureMatrix());
        eval.eval(testData.getLabels(), output);
        return eval.stats() + "\n\nScore: " + score;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void updateWeights(INDArray remoteGradient) {

    }

    @Override
    public INDArray getGradientAsArray() {
        return null;
    }

    @Override
    public void updateWeights(Gradient averageGradient) {
        model.update(averageGradient);
    }

    public Gradient getGradient() {
        return model.gradient();
    }
}
