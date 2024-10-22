Application is splitted into 3 parts

1. Configuration setup: based on yaml file https://github.com/jurikern/mapper/blob/master/src/main/resources/config.yaml
   - Defines file names for lookup table and logs. 
   - Defines composite lookup key for tags (dstport+protocol)
   - Defines protocols mapping
   - Defines log file schema (sequential order of field names based on V2 spec)
   ```
       17:15:33.954 [main] DEBUG jkern.mapper.parser.ParserTest -- V2Template(
       tag=[dstport, protocol],
       output=[tag,count, dstport,protocol,count],
       keys=[version, account-id, interface-id, dstaddr, srcaddr, dstport, srcport, protocol, packets, bytes, start, end, action, log-status],
       protocols={tcp=6, udp=17, icmp=1},
       files={lookup_table_file=lookup_table.txt, log_data=logs.txt})
   ```
2. Lookup file parse: https://github.com/jurikern/mapper/blob/master/src/main/java/jkern/mapper/parser/TagConfigParser.java
   - Parser reads the file as a buffered stream and creates lookup graph. Graph is implemented as adjacency list based on hash tables.
   - Graph describe edges between protocol and port and adds tag as a edge weight.
   - Lookup can be done with O(1) amortized time complexity: G[PROTOCOL][PORT] returns TAG
    ```
    17:34:40.122 [main] DEBUG jkern.mapper.parser.TagConfigParserTest -- {
      17={68=SV_P2, 31=SV_P3}, 1={0=SV_P5},
      6={993=EMAIL, 22=SV_P4, 23=SV_P1, 25=SV_P1, 443=SV_P2, 3389=SV_P5, 110=EMAIL, 143=EMAIL}}
    ```
3. Log file representation:
   - Each log line represented as abstract syntax tree where each node describes column name, column value and column type
     ```
      17:15:33.954 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=version, token=Token(type=INT, value=2))
      17:15:33.955 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=account-id, token=Token(type=LONG, value=123456789012))
      17:15:33.955 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=interface-id, token=Token(type=IDENT, value=eni-0a1b2c3d))
      17:15:33.955 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=dstaddr, token=Token(type=IP_ADDR, value=10.0.1.201))
      17:15:33.955 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=srcaddr, token=Token(type=IP_ADDR, value=198.51.100.2))
      17:15:33.955 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=dstport, token=Token(type=INT, value=443))
      17:15:33.955 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=action, token=Token(type=IDENT, value=ACCEPT))
      17:15:33.955 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=log-status, token=Token(type=IDENT, value=OK))
      17:15:33.955 [main] DEBUG jkern.mapper.parser.ParserTest -- {protocol=7, dstport=5}
    ```
  - Syntax tree stores lookup table to find composite key values in O(1) time complexity (without traversing entire tree)
    ```
    17:15:33.957 [main] DEBUG jkern.mapper.parser.ParserTest -- {protocol=7, dstport=5}
    ```
  - Abstract syntax tree is created with the help of Lexer: https://github.com/jurikern/mapper/blob/master/src/main/java/jkern/mapper/lexer/Lexer.java and Parser: https://github.com/jurikern/mapper/blob/master/src/main/java/jkern/mapper/parser/V2ParserImpl.java (V2 implementation)
  - Reason: Abstract syntax tree is widely used for interpretators/compilers implementation and makes parsing extendable and readable
3. Mapper initialized. https://github.com/jurikern/mapper/blob/master/src/main/java/jkern/mapper/Mapper.java
   - Mapper reads lines as a buffered stream from log file.
   - Mapper initializes processing graph that describes edges in format of (PROTOCOL) -> (PORT, TAG). Means that file is parsed only once and all calculations would be done based on graph data.
   - Mapper converts each line into syntax tree, analyzes and adds values into processing graph
     ```
     -- {17={68=SV_P2, 31=SV_P3}, 1={0=SV_P5}, 6={993=EMAIL, 22=SV_P4, 23=SV_P1, 25=SV_P1, 443=SV_P2, 3389=SV_P5, 110=EMAIL, 143=EMAIL}}
     ```
  - Mapper runs calculations on graph (tags count, port/protocol matches):
    ```
    17:34:40.118 [main] DEBUG jkern.mapper.MapperTest -- tag,count
    SV_P2,1
    SV_P1,2
    
    17:34:40.118 [main] DEBUG jkern.mapper.MapperTest -- port,protocol,count
    23,tcp,1
    25,tcp,1
    443,tcp,1
    ```

Local setup (my setup is based on Arch linux 6.11.4-arch2-1):

- Java: 
  ```
  curl -s "https://get.sdkman.io" | bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"

  sdk install java 21.0.5-tem
  sdk install maven 3.9.9
  ```
- Run tests:
  ```
  mvn test
  ```
- Run app:
  ```
  mvn exec:java
  ```

Tests output:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running jkern.mapper.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.051 s -- in jkern.mapper.AppTest
[INFO] Running jkern.mapper.lexer.LexerTest
SLF4J(I): Connected with provider of type [ch.qos.logback.classic.spi.LogbackServiceProvider]
17:53:20.967 [main] DEBUG jkern.mapper.lexer.LexerTest -- 2 123456789012 @ eni-0a1b2c3d 10.0.1.201 ACCEPT
17:53:20.971 [main] DEBUG jkern.mapper.lexer.LexerTest -- Token(type=INT, value=2)
17:53:20.971 [main] DEBUG jkern.mapper.lexer.LexerTest -- Token(type=LONG, value=123456789012)
17:53:20.971 [main] DEBUG jkern.mapper.lexer.LexerTest -- Token(type=INVALID, value=@)
17:53:20.971 [main] DEBUG jkern.mapper.lexer.LexerTest -- Token(type=IDENT, value=eni-0a1b2c3d)
17:53:20.971 [main] DEBUG jkern.mapper.lexer.LexerTest -- Token(type=IP_ADDR, value=10.0.1.201)
17:53:20.971 [main] DEBUG jkern.mapper.lexer.LexerTest -- Token(type=IDENT, value=ACCEPT)
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.043 s -- in jkern.mapper.lexer.LexerTest
[INFO] Running jkern.mapper.MapperTest
17:53:20.979 [main] DEBUG jkern.mapper.MapperTest -- {17={68=SV_P2, 31=SV_P3}, 1={0=SV_P5}, 6={993=EMAIL, 22=SV_P4, 23=SV_P1, 25=SV_P1, 443=SV_P2, 3389=SV_P5, 110=EMAIL, 143=EMAIL}}
17:53:20.981 [main] DEBUG jkern.mapper.MapperTest -- {6={23=Mapper.Pair(tag=SV_P1, count=1), 25=Mapper.Pair(tag=SV_P1, count=1), 443=Mapper.Pair(tag=SV_P2, count=1)}}
17:53:20.981 [main] DEBUG jkern.mapper.MapperTest -- tag,count
SV_P2,1
SV_P1,2

17:53:20.981 [main] DEBUG jkern.mapper.MapperTest -- port,protocol,count
23,tcp,1
25,tcp,1
443,tcp,1

[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in jkern.mapper.MapperTest
[INFO] Running jkern.mapper.parser.TagConfigParserTest
17:53:20.985 [main] DEBUG jkern.mapper.parser.TagConfigParserTest -- {17={68=SV_P2, 31=SV_P3}, 1={0=SV_P5}, 6={993=EMAIL, 22=SV_P4, 23=SV_P1, 25=SV_P1, 443=SV_P2, 3389=SV_P5, 110=EMAIL, 143=EMAIL}}
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in jkern.mapper.parser.TagConfigParserTest
[INFO] Running jkern.mapper.parser.ParserTest
17:53:20.992 [main] DEBUG jkern.mapper.parser.ParserTest -- 2 123456789012 eni-0a1b2c3d 10.0.1.201 198.51.100.2 443 49153 6 25 20000 1620140761 1620140821 ACCEPT OK

2 123456789012 eni-4d3c2b1a 192.168.1.100 203.0.113.101 23 49154 6 15 12000 1620140761 1620140821 REJECT OK

2 123456789012 eni-5e6f7g8h 192.168.1.101 198.51.100.3 25 49155 6 10 8000 1620140761 1620140821 ACCEPT OK

17:53:20.998 [main] DEBUG jkern.mapper.parser.ParserTest -- V2Template(tag=[dstport, protocol], output=[tag,count, dstport,protocol,count], keys=[version, account-id, interface-id, srcaddr, dstaddr, dstport, srcport, protocol, packets, bytes, start, end, action, log-status], protocols={tcp=6, udp=17, icmp=1}, files={lookup_table_file=lookup_table.txt, log_data=logs.txt})
17:53:20.998 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=version, token=Token(type=INT, value=2))
17:53:20.998 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=account-id, token=Token(type=LONG, value=123456789012))
17:53:20.998 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=interface-id, token=Token(type=IDENT, value=eni-0a1b2c3d))
17:53:20.998 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=srcaddr, token=Token(type=IP_ADDR, value=10.0.1.201))
17:53:20.998 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=dstaddr, token=Token(type=IP_ADDR, value=198.51.100.2))
17:53:20.998 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=dstport, token=Token(type=INT, value=443))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=srcport, token=Token(type=INT, value=49153))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=protocol, token=Token(type=INT, value=6))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=packets, token=Token(type=INT, value=25))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=bytes, token=Token(type=INT, value=20000))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=start, token=Token(type=INT, value=1620140761))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=end, token=Token(type=INT, value=1620140821))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=action, token=Token(type=IDENT, value=ACCEPT))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=log-status, token=Token(type=IDENT, value=OK))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- {protocol=7, dstport=5}
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=version, token=Token(type=INT, value=2))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=account-id, token=Token(type=LONG, value=123456789012))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=interface-id, token=Token(type=IDENT, value=eni-4d3c2b1a))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=srcaddr, token=Token(type=IP_ADDR, value=192.168.1.100))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=dstaddr, token=Token(type=IP_ADDR, value=203.0.113.101))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=dstport, token=Token(type=INT, value=23))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=srcport, token=Token(type=INT, value=49154))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=protocol, token=Token(type=INT, value=6))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=packets, token=Token(type=INT, value=15))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=bytes, token=Token(type=INT, value=12000))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=start, token=Token(type=INT, value=1620140761))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=end, token=Token(type=INT, value=1620140821))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=action, token=Token(type=IDENT, value=REJECT))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=log-status, token=Token(type=IDENT, value=OK))
17:53:20.999 [main] DEBUG jkern.mapper.parser.ParserTest -- {protocol=7, dstport=5}
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=version, token=Token(type=INT, value=2))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=account-id, token=Token(type=LONG, value=123456789012))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=interface-id, token=Token(type=IDENT, value=eni-5e6f7g8h))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=srcaddr, token=Token(type=IP_ADDR, value=192.168.1.101))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=dstaddr, token=Token(type=IP_ADDR, value=198.51.100.3))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=dstport, token=Token(type=INT, value=25))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=srcport, token=Token(type=INT, value=49155))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=protocol, token=Token(type=INT, value=6))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=packets, token=Token(type=INT, value=10))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=bytes, token=Token(type=INT, value=8000))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=start, token=Token(type=INT, value=1620140761))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=end, token=Token(type=INT, value=1620140821))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=action, token=Token(type=IDENT, value=ACCEPT))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- Statement(name=log-status, token=Token(type=IDENT, value=OK))
17:53:21.000 [main] DEBUG jkern.mapper.parser.ParserTest -- {protocol=7, dstport=5}
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in jkern.mapper.parser.ParserTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.587 s
[INFO] Finished at: 2024-10-21T17:53:21-07:00
[INFO] ------------------------------------------------------------------------
```

Run output:
```
[INFO] --- exec:1.4.0:java (default-cli) @ mapper ---
Tag Counts:
tag,count
SV_P2,1
SV_P1,2
EMAIL,3
Untagged,8

Port/Protocol Combination Counts:
port,protocol,count
1024,tcp,1
56000,tcp,1
49152,tcp,1
993,tcp,1
49153,tcp,1
49154,tcp,1
1030,tcp,1
49321,tcp,1
110,tcp,1
143,tcp,1
80,tcp,1
23,tcp,1
25,tcp,1
443,tcp,1

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.354 s
[INFO] Finished at: 2024-10-21T17:54:30-07:00
[INFO] ------------------------------------------------------------------------
```

Possible features:
- Concurrency. Thread pool can be created to minimize IO idle while reading lines from file.
  Since order of lines doesn't matter, each line would be parsed in thread and results added into processing graph. In this scenario interface/implementation of graph must be changed to concurrency safe version that implements mutex.
  Same can be done for output.
- Explore abstraction and add interfaces where needed to brake dependencies between specs and implementation






 
