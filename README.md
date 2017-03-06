# XML Parser

SAX and DOM works together

## Concept

The parser uses SAXParser to seek through your file.xml, and uses your handlers to process DOM nodes

## Usage

```java
import org.j.xmlparser.Parser;
import org.w3c.dom.Element;

// ...

Parser parser = new Parser();

parser.addHandler("tag", (Element element) -> {
    // do whatever you want
});

parser.parse("path/to/file.xml");
```

sharing context:
```java
import org.j.xmlparser.Parser;
import org.j.xmlparser.annotations.Handles;
import org.w3c.dom.Element;

// ...
public class TagsHandler {

    @Handles("tag1")
    public void handleTag1(Element element) {
        //...
    }

    @Handles("tag2")
    public void handleTag2(Element element) {
        //...
    }
}

// ...

Parser parser = new Parser();

parser.addHandler(new TagsHandler());

parser.parse("path/to/file.xml");
```

concurrent execution:
```java
import org.j.xmlparser.Parser;

// ...
Parser parser = new Parser();

// add handlers

parser.parseFixedThreadPool("path/to/file.xml", nThreads);
// or
parser.parseCachedThreadPool("path/to/file.xml");
// or
parser.setExecutor(new ThreadPoolExecutor(...));
parser.parse("path/to/file.xml");
// or as alternative
parser.parse("path/to/file.xml", new ThreadPoolExecutor(...));
```
