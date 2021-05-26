# async-dropbox
[![](https://jitci.com/gh/snowphone/async-dropbox/svg)](https://jitci.com/gh/snowphone/async-dropbox)

Simple Dropbox SDK with Java Future Interface

## Usage

### maven

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
<dependency>
  <groupId>com.github.snowphone</groupId>
  <artifactId>async-dropbox</artifactId>
  <version>0.3.1</version>
</dependency>
```

### gradle

```kotlin
repositories {
  maven { url="https://jitpack.io".let(::uri) }
}
dependencies {
  implementation("com.github.snowphone:async-dropbox:0.3.1")
}
```
