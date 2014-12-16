/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.dataflow.sdk.transforms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.api.client.util.Joiner;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.coders.BigEndianIntegerCoder;
import com.google.cloud.dataflow.sdk.testing.DataflowAssert;
import com.google.cloud.dataflow.sdk.testing.TestPipeline;
import com.google.cloud.dataflow.sdk.values.PCollection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests for Sample transform.
 */
@RunWith(JUnit4.class)
@SuppressWarnings("serial")
public class SampleTest {
  static final Integer[] EMPTY = new Integer[] { };
  static final Integer[] DATA = new Integer[] {1, 2, 3, 4, 5};
  static final Integer[] REPEATED_DATA = new Integer[] {1, 1, 2, 2, 3, 3, 4, 4, 5, 5};

  /**
   * Verifies that the result of a Sample operation contains the expected number of elements,
   * and that those elements are a subset of the items in expected.
   */
  public static class VerifyCorrectSample<T extends Comparable>
      implements SerializableFunction<Iterable<T>, Void> {
    private T[] expectedValues;
    private int expectedSize;

    /**
     * expectedSize is the number of elements that the Sample should contain. expected is the set
     * of elements that the sample may contain.
     */
    VerifyCorrectSample(int expectedSize, T... expected) {
      this.expectedValues = expected;
      this.expectedSize = expectedSize;
    }

    @Override
    public Void apply(Iterable<T> in) {
      List<T> actual = new ArrayList<>();
      for (T elem : in) {
        actual.add(elem);
      }

      assertEquals(expectedSize, actual.size());

      Collections.sort(actual);  // We assume that @expected is already sorted.
      int i = 0;  // Index into @expected
      for (T s : actual) {
        boolean matchFound = false;
        for (; i < expectedValues.length; i++) {
          if (s.equals(expectedValues[i])) {
            matchFound = true;
            break;
          }
        }
        assertTrue("Invalid sample: " +  Joiner.on(',').join(actual), matchFound);
        i++;  // Don't match the same element again.
      }
      return null;
    }
  }

  @Test
  @Category(com.google.cloud.dataflow.sdk.testing.RunnableOnService.class)
  public void testSample() {
    Pipeline p = TestPipeline.create();

    PCollection<Integer> input = p.apply(Create.of(DATA))
        .setCoder(BigEndianIntegerCoder.of());
    PCollection<Iterable<Integer>> output = input.apply(
        Sample.<Integer>fixedSizeGlobally(3));

    DataflowAssert.thatSingletonIterable(output)
        .satisfies(new VerifyCorrectSample<>(3, DATA));
    p.run();
  }

  @Test
  @Category(com.google.cloud.dataflow.sdk.testing.RunnableOnService.class)
  public void testSampleEmpty() {
    Pipeline p = TestPipeline.create();

    PCollection<Integer> input = p.apply(Create.of(EMPTY))
        .setCoder(BigEndianIntegerCoder.of());
    PCollection<Iterable<Integer>> output = input.apply(
        Sample.<Integer>fixedSizeGlobally(3));

    DataflowAssert.thatSingletonIterable(output)
        .satisfies(new VerifyCorrectSample<>(0, EMPTY));
    p.run();
  }

  @Test
  @Category(com.google.cloud.dataflow.sdk.testing.RunnableOnService.class)
  public void testSampleZero() {
    Pipeline p = TestPipeline.create();

    PCollection<Integer> input = p.apply(Create.of(DATA))
        .setCoder(BigEndianIntegerCoder.of());
    PCollection<Iterable<Integer>> output = input.apply(
        Sample.<Integer>fixedSizeGlobally(0));

    DataflowAssert.thatSingletonIterable(output)
        .satisfies(new VerifyCorrectSample<>(0, DATA));
    p.run();
  }

  @Test
  @Category(com.google.cloud.dataflow.sdk.testing.RunnableOnService.class)
  public void testSampleInsufficientElements() {
    Pipeline p = TestPipeline.create();

    PCollection<Integer> input = p.apply(Create.of(DATA))
        .setCoder(BigEndianIntegerCoder.of());
    PCollection<Iterable<Integer>> output = input.apply(
        Sample.<Integer>fixedSizeGlobally(10));

    DataflowAssert.thatSingletonIterable(output)
        .satisfies(new VerifyCorrectSample<>(5, DATA));
    p.run();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSampleNegative() {
    Pipeline p = TestPipeline.create();

    PCollection<Integer> input = p.apply(Create.of(DATA))
        .setCoder(BigEndianIntegerCoder.of());
    input.apply(Sample.<Integer>fixedSizeGlobally(-1));
  }

  @Test
  @Category(com.google.cloud.dataflow.sdk.testing.RunnableOnService.class)
  public void testSampleMultiplicity() {
    Pipeline p = TestPipeline.create();

    PCollection<Integer> input = p.apply(Create.of(REPEATED_DATA))
        .setCoder(BigEndianIntegerCoder.of());
    // At least one value must be selected with multiplicity.
    PCollection<Iterable<Integer>> output = input.apply(
        Sample.<Integer>fixedSizeGlobally(6));

    DataflowAssert.thatSingletonIterable(output)
        .satisfies(new VerifyCorrectSample<>(6, REPEATED_DATA));
    p.run();
  }
}