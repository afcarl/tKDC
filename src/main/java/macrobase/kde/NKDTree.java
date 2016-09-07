package macrobase.kde;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import macrobase.kde.AlgebraUtils;

import java.util.*;

public class NKDTree {
    // Core Data
    protected int k;
    protected NKDTree loChild;
    protected NKDTree hiChild;
    protected ArrayList<double[]> leafItems;

    // Parameters
    private int leafCapacity = 20;
    private boolean splitByWidth = false;

    // Statistics
    private int splitDimension;
    protected int nBelow;
    protected double[] mean;
    private double splitValue;
    // Array of (k,2) dimensions, of (min, max) pairs in all k dimensions
    private double[][] boundaries;

    public NKDTree() {
        splitDimension = 0;
    }

    public NKDTree(NKDTree parent, boolean loChild) {
        this.k = parent.k;
        this.splitDimension = (parent.splitDimension + 1) % k;
        this.boundaries = new double[k][2];
        for (int i=0;i<k;i++) {
            this.boundaries[i][0] = parent.boundaries[i][0];
            this.boundaries[i][1] = parent.boundaries[i][1];
        }
        if (loChild) {
            this.boundaries[parent.splitDimension][1] = parent.splitValue;
        } else {
            this.boundaries[parent.splitDimension][0] = parent.splitValue;
        }

        leafCapacity = parent.leafCapacity;
        splitByWidth = parent.splitByWidth;
    }

    public NKDTree setSplitByWidth(boolean f) {
        this.splitByWidth = f;
        return this;
    }
    public NKDTree setLeafCapacity(int leafCapacity) {
        this.leafCapacity = leafCapacity;
        return this;
    }

    public NKDTree build(List<double[]> data) {
        this.k = data.get(0).length;
        this.boundaries = AlgebraUtils.getBoundingBoxRaw(data);
        // Make a local copy of the data since we're going to sort it
        double[][] dataArray = new double[data.size()][k];
        for (int i=0;i<dataArray.length;i++) {
            dataArray[i] = data.get(i);
        }
        return buildRec(dataArray, 0, dataArray.length);
    }

    private NKDTree buildRec(double[][] data, int startIdx, int endIdx) {
        this.nBelow = endIdx - startIdx;

        if (endIdx - startIdx > this.leafCapacity) {

            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (int j=startIdx;j<endIdx;j++) {
                double curVal = data[j][splitDimension];
                if (curVal < min) { min = curVal; }
                if (curVal > max) { max = curVal; }
            }
            boundaries[splitDimension][0] = min;
            boundaries[splitDimension][1] = max;

            this.splitValue = 0.5 * (boundaries[splitDimension][0] + boundaries[splitDimension][1]);
            int l = startIdx;
            int r = endIdx - 1;
            while (true) {
                while (data[l][splitDimension] < splitValue && (l<r)) {
                    l++;
                }
                while (data[r][splitDimension] >= splitValue && (l<r)) {
                    r--;
                }
                if (l < r) {
                    double[] tmp = data[l];
                    data[l]= data[r];
                    data[r]= tmp;
                } else {
                    break;
                }
            }
            if (l==startIdx || l ==endIdx-1) {
                this.splitValue = data[l][splitDimension];
                l = (startIdx + endIdx) / 2;
            }
            this.loChild = new NKDTree(this, true).buildRec(data, startIdx, l);
            this.hiChild = new NKDTree(this, false).buildRec(data, l, endIdx);

            this.mean = new double[k];
            for (int i = 0; i < k; i++) {
                this.mean[i] = (loChild.mean[i] * loChild.getNBelow() + hiChild.mean[i] * hiChild.getNBelow())
                        / (loChild.getNBelow() + hiChild.getNBelow());
            }
        } else {
            this.leafItems = new ArrayList<>(leafCapacity);

            double[] sum = new double[k];
            for (int j=startIdx;j<endIdx;j++) {
                double[] d = data[j];
                leafItems.add(d);
                for (int i = 0; i < k; i++) {
                    sum[i] += d[i];
                }
            }

            for (int i = 0; i < k; i++) {
                sum[i] /= this.nBelow;
            }

            this.mean = sum;
        }
        return this;
    }

    /**
     * Estimates min and max difference absolute vectors from point to region
     * @return minVec, maxVec
     */
    public double[][] getMinMaxDistanceVectors(double[] q) {
        double[][] minMaxDiff = new double[2][k];

        for (int i=0; i<k; i++) {
            double d1 = q[i] - boundaries[i][0];
            double d2 = q[i] - boundaries[i][1];
            // outside to the right
            if (d2 >= 0) {
                minMaxDiff[0][i] = d2;
                minMaxDiff[1][i] = d1;
            }
            // inside, min distance is 0;
            else if (d1 >= 0) {
                minMaxDiff[1][i] = d1 > -d2 ? d1 : -d2;
            }
            // outside to the left
            else {
                minMaxDiff[0][i] = -d1;
                minMaxDiff[1][i] = -d2;
            }
        }

        return minMaxDiff;
    }

    /**
     * Estimates bounds on the distance to a region
     * @return array with min, max distances squared
     */
    public double[] getMinMaxDistances(double[] q) {
        double[][] diffVectors = getMinMaxDistanceVectors(q);
        double[] estimates = new double[2];
        for (int i = 0; i < k; i++) {
            double minD = diffVectors[0][i];
            double maxD = diffVectors[1][i];
            estimates[0] += minD * minD;
            estimates[1] += maxD * maxD;
        }
        return estimates;
    }

    public boolean isInsideBoundaries(double[] q) {
        for (int i=0; i<k; i++) {
            if (q[i] < this.boundaries[i][0] || q[i] > this.boundaries[i][1]) {
                return false;
            }
        }
        return true;
    }

    public ArrayList<double[]> getItems() {
        return this.leafItems;
    }

    public double[] getMean() {
        return this.mean;
    }

    public int getNBelow() {
        return nBelow;
    }

    public double[][] getBoundaries() {
        return this.boundaries;
    }

    public NKDTree getLoChild() {
        return this.loChild;
    }

    public NKDTree getHiChild() {
        return this.hiChild;
    }

    public boolean isLeaf() {
        return this.loChild == null && this.hiChild == null;
    }

    public int getSplitDimension() {
        return splitDimension;
    }

    public double getSplitValue() {
        return splitValue;
    }

    public Map<String, Object> toMap(boolean verbose) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (loChild == null && hiChild == null) {
            if (verbose) {
                out.put("items", this.leafItems);
            } else {
                out.put("n", this.leafItems.size());
            }
        } else {
            out.put("dim", this.splitDimension);
            out.put("val", this.splitValue);
            if (loChild != null) {
                out.put("lo", loChild.toMap(verbose));
            }
            if (hiChild != null) {
                out.put("hi", hiChild.toMap(verbose));
            }
        }
        return out;
    }

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(toMap(false));
    }
}
