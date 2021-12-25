package com.google.mu.util.stream;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A joiner (and {@code Collector}) that joins strings.
 *
 * <p>When used to join a pair of objects (for example in a BiStream), it can be used for BiFunction:
 * <pre>{@code
 *   BiStream.zip(userIds, names)
 *       .mapToObj(Joiner.on('=')::join)  // (id, name) -> id + "=" + name
 *       ....;
 * }</pre>
 *
 * <p>Alternatively, it can be used as a Collector. For example,
 * {@code names.collect(Joiner.on(','))} is equivalent to {@code names.collect(Collectors.joining(","))}.
 *
 * Except that JDK {@code joining()} requires the inputs to be strings; while Joiner can join any input,
 * e.g. numbers.
 *
 * <p>You can also chain {@link #between} to further enclose the joined result between a pair of strings.
 * The following code joins a list of ids and their corresponding names in the format of
 * {@code "[id1:name1, id2:name2, id3:name3, ...]"}:
 * <pre>{@code
 *   BiStream.zip(userIds, names)
 *       .mapToObj(Joiner.on('=')::join)
 *       .collect(Joiner.on(", ").between('[', ']'));
 * }</pre>
 * Which reads clearer than using JDK {@link Collectors#joining}:
 * <pre>{@code
 *   BiStream.zip(userIds, names)
 *       .mapToObj(id, name) -> id + ":" + name)
 *       .collect(Collectors.joining(", ", "[", "]"));
 * }</pre>
 *
 * <p>Or, you can format the {@code (id, name)} pairs in the format of
 * {@code "[(id1, name1), (id2, name2). ...]"}:
 * <pre>{@code
 *   BiStream.from(userIdMap)
 *       .mapValues(User::name)
 *       .mapToObj(Joiner.on(", ").between('(', ')')::join)
 *       .collect(Joiner.on(", ").between('[', ']'));
 * }</pre>
 *
 * <p>If you need to skip nulls and/or empty strings while joining, use {@code
 * collect(Joiner.on(...).skipNulls())} or {@code collect(Joiner.on(...).skipEmpties())} respectively.
 *
 * <p>Unlike Guava {@code com.google.common.base.Joiner}, nulls don't cause NullPointerException,
 * instead, they are stringified using {@link String#valueOf}.
 *
 * @since 5.6
 */
public final class Joiner implements Collector<Object, StringJoiner, String> {
  private final String prefix;
  private final String delimiter;
  private final String suffix;

  private Joiner(String prefix, String delimiter, String suffix) {
    this.prefix = prefix;
    this.delimiter = delimiter;
    this.suffix = suffix;
  }

  /** Joining the inputs on the {@code delimiter} character */
  public static Joiner on(char delimiter) {
    return on(Character.toString(delimiter));
  }

  /** Joining the inputs on the {@code delimiter} string */
  public static Joiner on(CharSequence delimiter) {
    return new Joiner("", delimiter.toString(), "");
  }

  /** Joins {@code l} and {@code r} together. */
  public String join(Object l, Object r) {
    return prefix + l + delimiter + r + suffix;
  }

  /**
   * Joins elements from {@code collection}.
   *
   * <p>{@code joiner.join(list)} is equivalent to {@code list.stream().collect(joiner)}.
   *
   * @since 5.7
   */
  public String join(Collection<?> collection) {
    return collection.stream().collect(this);
  }

  /**
   * Returns an instance that wraps the join result between {@code before} and {@code after}.
   *
   * <p>For example both {@code Joiner.on(',').between('[', ']').join(List.of(1, 2))} and
   * {@code Joiner.on(',').between('[', ']').join(1, 2)} return {@code "[1,2]"}.
   */
  public Joiner between(char before, char after) {
    return new Joiner(before + prefix, delimiter, suffix + after);
  }

  /*
   * Returns an instance that wraps the join result between {@code before} and {@code after}.
   *
   * <p>For example both {@code Joiner.on(',').between("[", "]").join([1, 2])} and
   * {@code Joiner.on(',').between("[", "]").join(1, 2)} result in {@code "[1,2]"}.
   */
  public Joiner between(CharSequence before, CharSequence after) {
    return new Joiner(requireNonNull(before) + prefix, delimiter, suffix + requireNonNull(after));
  }

  /** Returns a Collector that skips null inputs and joins the remaining using this Joiner. */
  public Collector<Object, ?, String> skipNulls() {
    return Java9Collectors.filtering(v -> v != null, this);
  }

  /**
   * Returns a Collector that skips null and empty string inputs and joins the remaining using
   * this Joiner.
   */
  public Collector<CharSequence, ?, String> skipEmpties() {
    return Java9Collectors.filtering(s -> s != null && s.length() > 0, this);
  }

  @Override public Supplier<StringJoiner> supplier() {
    return () -> new StringJoiner(delimiter, prefix, suffix);
  }

  @Override public BiConsumer<StringJoiner, Object> accumulator() {
    return (joiner, obj) -> joiner.add(String.valueOf(obj));
  }

  @Override public BinaryOperator<StringJoiner> combiner() {
    return StringJoiner::merge;
  }

  @Override public Function<StringJoiner, String> finisher() {
    return StringJoiner::toString;
  }

  @Override public Set<Characteristics> characteristics() {
    return Collections.emptySet();
  }
}
