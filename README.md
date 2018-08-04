# StreamEx
Enhancing Java 8 Streams.

[![Maven Central](https://img.shields.io/maven-central/v/com.landawn/streamex.svg)](https://maven-badges.herokuapp.com/maven-central/com.landawn/streamex/)
[![Javadocs](https://www.javadoc.io/badge/com.landawn/streamex.svg)](https://www.javadoc.io/doc/com.landawn/streamex)

This library defines four classes: `StreamEx`, `IntStreamEx`, `LongStreamEx`, `DoubleStreamEx`
which are fully compatible with Java 8 stream classes and provide many additional useful methods.
Also `EntryStream` class is provided which represents the stream of map entries and provides
additional functionality for this case. Finally there are some new useful collectors defined in `MoreCollectors`
class as well as primitive collectors concept.

Full API documentation is available [here](https://www.javadoc.io/doc/com.landawn/streamex).

Take a look at the [Cheatsheet](CHEATSHEET.md) for brief introduction to the StreamEx!

Before updating StreamEx check the [migration notes](MIGRATION.md) and full list of [changes](CHANGES.md).

StreamEx library main points are following:

* Shorter and convenient ways to do the common tasks.
* Better interoperability with older code.
* Friendliness for parallel processing: any new feature takes the advantage on parallel streams as much as possible.
* Performance and minimal overhead. If StreamEx allows to solve the task using less code compared to standard Stream, it
should not be significantly slower than the standard way (and sometimes it's even faster).
* Almost 100% compatibility with original JDK streams except not [throwing exception in `StreamEx.toMap/Collectors.toMap` for `null` entry values](https://stackoverflow.com/questions/24630963/java-8-nullpointerexception-in-collectors-tomap).

### Examples

Collector shortcut methods (toList, toSet, toMap, join, etc.)
```java
List<String> userNames = StreamEx.of(users).map(User::getName).toList();
Map<Role, List<User>> role2users = StreamEx.of(users).groupTo(User::getRole);
StreamEx.of(1,2,3).join("; "); // "1; 2; 3"
```

Intermediate operations: groupBy/groupByToEntry

```java
// By native Java Stream APIs:
accounts.stream()
    .collect(Collectors.groupingBy(e -> e.getFirstName(), Collectors.counting()))
    .entrySet().stream()
    .sorted(Entry.comparingByValue())
    .collect(Collectors.toMap(Function.identity(), Function.identity(), () -> new LinkedHashMap<>()));

// groupBy. Less steps and more clear.
StreamEx.of(accounts)
    .groupBy(e -> e.getFirstName(), Collectors.counting())
    .sorted(Entry.comparingByValue())
    .toMap(Function.identity(), Function.identity(), () -> new LinkedHashMap<>());

// even shorter and clearer with: groupByToEntry.
StreamEx.of(accounts)
    .groupByToEntry(e -> e.getFirstName(), Collectors.counting())
    .sorted(Entry.comparingByValue())
    .toMap(LinkedHashMap::new);
```

Selecting stream elements of specific type
```java
public List<Element> elementsOf(NodeList nodeList) {
    return IntStreamEx.range(nodeList.getLength())
      .mapToObj(nodeList::item).select(Element.class).toList();
}
```

Adding elements to stream
```java
public List<String> getDropDownOptions() {
    return StreamEx.of(users).map(User::getName).prepend("(none)").toList();
}

public int[] addValue(int[] arr, int value) {
    return IntStreamEx.of(arr).append(value).toArray();
}
```

Removing unwanted elements and using the stream as Iterable:
```java
public void copyNonEmptyLines(Reader reader, Writer writer) throws IOException {
    for(String line : StreamEx.ofLines(reader).remove(String::isEmpty)) {
        writer.write(line);
        writer.write(System.lineSeparator());
    }
}
```

Selecting map keys by value predicate:
```java
Map<String, Role> nameToRole;

public Set<String> getEnabledRoleNames() {
    return StreamEx.ofKeys(nameToRole, Role::isEnabled).toSet();
}
```

Operating on key-value pairs:
```java
public Map<String, List<String>> invert(Map<String, List<String>> map) {
    return EntryStream.of(map).flatMapValues(List::stream).invert().groupTo();
}

public Map<String, String> stringMap(Map<Object, Object> map) {
    return EntryStream.of(map).mapKeys(String::valueOf)
        .mapValues(String::valueOf).toMap();
}

Map<String, Group> nameToGroup;

public Map<String, List<User>> getGroupMembers(Collection<String> groupNames) {
    return StreamEx.of(groupNames).mapToEntry(nameToGroup::get)
        .nonNullValues().mapValues(Group::getMembers).toMap();
}
```

Pairwise differences:
```java
DoubleStreamEx.of(input).pairMap((a, b) -> b-a).toArray();
```

Support of byte/char/short/float types:
```java
short[] multiply(short[] src, short multiplier) {
    return IntStreamEx.of(src).map(x -> x*multiplier).toShortArray(); 
}
```

Define custom lazy intermediate operation recursively:
```java
static <T> StreamEx<T> scanLeft(StreamEx<T> input, BinaryOperator<T> operator) {
        return input.headTail((head, tail) -> scanLeft(tail.mapFirst(cur -> operator.apply(head, cur)), operator)
                .prepend(head));
}
```

And more!

### License

This project is licensed under [Apache License, version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

### Installation

Releases are available in [Maven Central](https://repo1.maven.org/maven2/com/landawn/streamex/)

Before updating StreamEx check the [migration notes](MIGRATION.md) and full list of [changes](CHANGES.md).

To use from maven add this snippet to the pom.xml `dependencies` section:

```xml
<dependency>
  <groupId>com.landawn</groupId>
  <artifactId>streamex</artifactId>
  <version>2.2.3</version>
</dependency>
```

Pull requests are welcome.
