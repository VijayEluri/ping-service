package com.anjlab.ping.services;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.anjlab.cubics.Aggregate;
import com.anjlab.cubics.BeanValueProvider;
import com.anjlab.cubics.FactValueProvider;

public class GeneralStats {
    
    private double min;
    private double max;
    private double average;
    private double count;
    private double sum;
    private double squaredSigma;
    private double standard;
    private double median;
    
    private GeneralStats(double min, double max, double average, int count,
            double sum, double squaredSigma, double standard, double median) {
        this.min = min;
        this.max = max;
        this.average = average;
        this.count = count;
        this.sum = sum;
        this.squaredSigma = squaredSigma;
        this.standard = standard;
        this.median = median;
    }
    
    public double getMin() {
        return min;
    }
    public double getMax() {
        return max;
    }
    public double getAverage() {
        return average;
    }
    public double getCount() {
        return count;
    }
    public double getSum() {
        return sum;
    }
    public double getSquaredSigma() {
        return squaredSigma;
    }
    public double getStandard() {
        return standard;
    }
    public double getMedian() {
        return median;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> GeneralStats calculate(Class<T> clazz, final String property, List<T> data) {
        final FactValueProvider<T> beanClass = new BeanValueProvider<T>(clazz);
        
        Collections.sort((List<T>)data, new Comparator<T>() {
            @Override
            @SuppressWarnings("rawtypes")
            public int compare(T o1, T o2) {
                Comparable v1 = (Comparable<?>) beanClass.getValue(property, o1);
                Comparable v2 = (Comparable<?>) beanClass.getValue(property, o2);
                
                return v1.compareTo(v2);
            }
        });

        double median = ((Number) beanClass.getValue(property, data.get(data.size() / 2))).doubleValue();
        
        Aggregate<T> aggregate = new Aggregate<T>(null);

        for (T item : data) {
            aggregate.add(beanClass.getValue(property, item));
        }

        double avg = aggregate.getAverage();

        //    Dispersion
        double squaredSigma = 0;
        for (T item : data) {
            Number n = (Number) beanClass.getValue(property, item);
            squaredSigma += Math.pow(n.doubleValue() - avg, 2);
        }
        int N = aggregate.getCount();
        
        squaredSigma = squaredSigma / N;
        
        double standard = Math.sqrt(N / (N - 1) * squaredSigma);
        
        return new GeneralStats(
                aggregate.getMin(), 
                aggregate.getMax(), 
                aggregate.getAverage(), 
                aggregate.getCount(), 
                aggregate.getSum(), 
                squaredSigma,
                standard,
                median);
    }
    
}
