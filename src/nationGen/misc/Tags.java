package nationGen.misc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A collection of tag values.  A tag value has a tag name and some number of arguments.  A {@link Tags} can contain
 * several different values for the same tag name.
 */
public class Tags {

  private HashMap<String, List<Args>> tagValues = new HashMap<>();

  private List<Args> computeArgsList(String name) {
    return this.tagValues.computeIfAbsent(name, n -> new ArrayList<>());
  }

  public void add(List<String> tag) {
    if (tag.isEmpty()) {
      throw new IllegalArgumentException("No tag name found.");
    }
    Args args = tag
      .subList(1, tag.size())
      .stream()
      .map(Arg::new)
      .collect(Collectors.toCollection(Args::new));
    add(tag.get(0), args);
  }

  public void add(String name, String value) {
    computeArgsList(name).add(Args.of(new Arg(value)));
  }

  public void add(String name, int value) {
    computeArgsList(name).add(Args.of(new Arg(value)));
  }

  public void add(String name, double value) {
    computeArgsList(name).add(Args.of(new Arg(value)));
  }

  public void add(String name, Args value) {
    computeArgsList(name).add(value);
  }

  public void addFromCommand(Command line) {
    computeArgsList(line.command).add(line.args);
  }

  public void addArgs(String name, Object... values) {
    computeArgsList(name).add(
      Arrays.stream(values)
        .map(Object::toString)
        .map(Arg::new)
        .collect(Collectors.toCollection(Args::new))
    );
  }

  public void addName(String name) {
    add(name, new Args());
  }

  public void addAll(String name, Collection<Args> values) {
    computeArgsList(name).addAll(values);
  }

  public void addAllNames(List<String> names) {
    names.forEach(this::addName);
  }

  public void addAll(Tags tags) {
    tags.tagValues.forEach(this::addAll);
  }

  public List<Args> remove(String name) {
    return this.tagValues.remove(name);
  }

  public boolean remove(Command command) {
    return this.remove(command.command, command.args);
  }

  public boolean remove(String name, Args args) {
    return this.tagValues.getOrDefault(name, new ArrayList<>()).remove(args);
  }

  public boolean removeAll(String name, Collection<Args> argses) {
    return this.tagValues.getOrDefault(name, new ArrayList<>()).removeAll(
        argses
      );
  }

  public List<Args> getAllArgs(String name) {
    return this.tagValues.getOrDefault(name, new ArrayList<>());
  }

  public Stream<Arg> streamAllValues(String name) {
    return getAllArgs(name).stream().map(l -> l.get(0));
  }

  public List<Arg> getAllValues(String name) {
    return streamAllValues(name).collect(Collectors.toList());
  }

  public List<String> getAllStrings(String name) {
    return streamAllValues(name).map(Arg::get).collect(Collectors.toList());
  }

  public List<Integer> getAllInts(String name) {
    return streamAllValues(name).map(Arg::getInt).collect(Collectors.toList());
  }

  public List<Double> getAllDoubles(String name) {
    return streamAllValues(name)
      .map(Arg::getDouble)
      .collect(Collectors.toList());
  }

  public List<Command> getAllCommands(String name) {
    return streamAllValues(name)
      .map(Arg::getCommand)
      .collect(Collectors.toList());
  }

  public Optional<Args> getArgs(String name) {
    return this.tagValues.containsKey(name)
      ? this.tagValues.get(name).stream().findFirst()
      : Optional.empty();
  }

  public Optional<Arg> getValue(String name) {
    return getArgs(name).map(l -> l.get(0));
  }

  public Optional<String> getString(String name) {
    return getValue(name).map(Arg::get);
  }

  public Optional<Integer> getInt(String name) {
    return getValue(name).map(Arg::getInt);
  }

  public Optional<Double> getDouble(String name) {
    return getValue(name).map(Arg::getDouble);
  }

  public Optional<Command> getCommand(String name) {
    return getValue(name).map(Arg::getCommand);
  }

  public boolean containsName(String name) {
    return this.tagValues.containsKey(name);
  }

  public boolean contains(String name, Object value) {
    String stringValue = value.toString();
    return (
      this.tagValues.containsKey(name) &&
      this.tagValues.get(name)
        .stream()
        .flatMap(List::stream)
        .map(Arg::get)
        .anyMatch(stringValue::equals)
    );
  }

  public boolean containsTag(Command command) {
    return getAllArgs(command.command).stream().anyMatch(command.args::equals);
  }

  public Tags named(String name) {
    Tags temp = new Tags();
    temp.addAll(name, getAllArgs(name));
    return temp;
  }

  public boolean isEmpty() {
    return (
      this.tagValues.isEmpty() ||
      this.tagValues.values().stream().allMatch(List::isEmpty)
    );
  }

  public String toString() {
    return this.tagValues.entrySet()
      .stream()
      .map(e -> "[" + e.getKey() + ": " + e.getValue() + "]")
      .collect(Collectors.joining(", "));
  }
}
