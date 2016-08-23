# XML Parser

SAX and DOM works together

## Concept

Under the hood parser uses SAXParser to seek through your file.xml for nodes you choice.

Then it creates dom Element from found node and calls your handlers via Executor.execute

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

    @Handles(tag = "tag1")
    public void handleTag1(Element element) {
        //...
    }

    @Handles(tag = "tag2")
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
```
