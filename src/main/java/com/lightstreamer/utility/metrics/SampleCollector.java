package com.lightstreamer.utility.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import io.prometheus.client.SimpleCollector;

public class SampleCollector extends SimpleCollector<SampleCollector.Child> {

  private final double[] bounds;

  private final List<String> labelValues;

  SampleCollector(Builder b) {
    super(b);
    this.bounds = b.bounds;
    List<String> tempLabels =
        DoubleStream.of(bounds).mapToObj(String::valueOf).collect(Collectors.toList());
    labelValues = List.copyOf(tempLabels);
    initializeNoLabelsChild();
  }

  /**
   * Return a Builder to allow configuration of a new Histogram. Ensures required fields are
   * provided.
   *
   * @param name The name of the metric
   * @param help The help string of the metric
   */
  public static Builder build(String name, String help) {
    return new Builder().name(name).help(help);
  }

  /**
   * Return a Builder to allow configuration of a new Histogram.
   */
  public static Builder build() {
    return new Builder();
  }

  public static class Builder extends SimpleCollector.Builder<Builder, SampleCollector> {

    private double[] bounds = {0.010, 0.018, 0.025, 0.050, 0.075, 0.100, 0.150, 0.200, 0.300, 0.500,
        1, 2, 3, 4, 5, 10, Double.POSITIVE_INFINITY};

    public Builder bounds(double[] bounds) {
      this.bounds = bounds;
      return this;
    }

    @Override
    public SampleCollector create() {
      return new SampleCollector(this);
    }

  }

  @Override
  public List<MetricFamilySamples> collect() {
    List<MetricFamilySamples.Sample> mfs = new ArrayList<>();
    for (Map.Entry<List<String>, Child> c : children.entrySet()) {
      Child child = c.getValue();
      List<String> labelNamesWithLe = new ArrayList<String>(labelNames);
      labelNamesWithLe.add("be");
      Value value = child.getValueAndReset();
      mfs.add(new MetricFamilySamples.Sample(fullname + "_max", labelNames, c.getKey(), value.max));
      mfs.add(new MetricFamilySamples.Sample(fullname + "_avg", labelNames, c.getKey(), value.avg));
      mfs.add(new MetricFamilySamples.Sample(fullname + "_sum", labelNames, c.getKey(), value.sum));
      mfs.add(
          new MetricFamilySamples.Sample(fullname + "_count", labelNames, c.getKey(), value.count));

      for (int i = 0; i < child.bounds.length; i++) {
        List<String> lableValues = new ArrayList<>(c.getKey());
        lableValues.add(labelValues.get(i));
        mfs.add(new MetricFamilySamples.Sample(fullname, labelNamesWithLe, lableValues,
            value.values[i]));
      }
    }

    return familySamplesList(Type.HISTOGRAM, mfs);
  }

  @Override
  protected Child newChild() {
    return new Child(bounds);
  }

  static class Value {
    private final int count;

    private final double[] values;

    private final double sum;

    private final double max;

    private final double avg;

    private Value(DoubleAdder sum, double max, DoubleAdder[] buckets) {
      //@formatter:off
      this.count = Stream.of(buckets)
          .mapToInt(DoubleAdder::intValue)
          .sum();
      this.values = Stream.of(buckets)
          .mapToDouble(DoubleAdder::sum)
          .toArray();
      this.sum = sum.sum();
      this.max = max;
      this.avg = this.sum / this.count;
      //@formatter:on
    }

    public double[] values() {
      return values;
    }

    public int getCount() {
      return count;
    }

    public double getMax() {
      return max;
    }

    public double getSum() {
      return sum;
    }

    public double getAvg() {
      return avg;
    }

  }

  static class BucketsHolder {

    private final DoubleAdder[] buckets;

    private final DoubleAdder sum = new DoubleAdder();

    private double max = 0;

    private final double[] bounds;

    BucketsHolder(double[] bounds) {
      this.bounds = bounds;
      this.buckets = new DoubleAdder[bounds.length];
      for (int i = 0; i < buckets.length; i++) {
        buckets[i] = new DoubleAdder();
      }
    }

    public void insert(double amt) {
      for (int i = 0; i < bounds.length; i++) {
        if (amt <= bounds[i]) {
          buckets[i].add(1);
          break;
        }
      }

      max = Math.max(max, amt);
      sum.add(amt);
    }

    public void reset() {
      for (int i = 0; i < buckets.length; i++) {
        buckets[i].reset();
      }
      max = 0;
      sum.reset();
    }

  }

  static class Child {

    private final double[] bounds;

    private final BucketsHolder holder;

    public Child(double[] bounds) {
      this.bounds = bounds;
      this.holder = new BucketsHolder(bounds);
    }

    public synchronized void observe(double amt) {
      holder.insert(amt);
    }

    public synchronized Value getValueAndReset() {
      Value snapshot = new Value(holder.sum, holder.max, holder.buckets);
      holder.reset();
      return snapshot;
    }
  }

}
