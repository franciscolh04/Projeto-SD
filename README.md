# TupleSpaces

Distributed Systems Project 2025
 
**Group A19**

**Difficulty level: I am Death incarnate!**

#

## A.1:
4.67/5

## A.2:
1.73/2

## B.1:
2.80/3

## B.2:
3.33/4

## C.1:
3/3

## C.2:
3/3

## Final Grade:
18.53/20

#


### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __A19__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members

| Number | Name               | User                                   | Email                                          |
|--------|--------------------|----------------------------------------|------------------------------------------------|
| 106074 | Rodrigo Perestrelo | <https://github.com/RodrigoPerestrelo> | <mailto:rodrigo.perestrelo@tecnico.ulisboa.pt> |
| 106970 | Francisco Heleno   | <https://github.com/franciscolh04>     | <mailto:francisco.l.heleno@tecnico.ulisboa.pt> |
| 106494 | Mafalda Dias       | <https://github.com/mafaldarpdias>     | <mailto:mafalda.p.dias@tecnico.ulisboa.pt>     |

## Getting Started

The overall system is made up of several modules.
The definition of messages and services is in _Contract_.

See the [Project Statement](https://github.com/tecnico-distsys/Tuplespaces-2025) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules, run the following commands in the root directory:

```s
mvn clean install
cd Contract
mvn install
mvn exec:exec
cd ..
```

### Testing

All test commands must be executed from the `tests` directory.

To run all tests using both the Java and Python clients, execute:

```s
./run_tests.sh
````

To run tests only with the Java client:

```s
./run_tests.sh -j
````

To run tests only with the Python client:

```s
./run_tests.sh -p
````

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
