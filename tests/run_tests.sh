#!/bin/bash

################################################
### PATHS (feel free to tweak paths accordingly)
CLI_JAVA_PATH=${PWD}/../Client-Java
CLI_PYTHON_PATH=${PWD}/../Client-Python
TESTS_FOLDER=${PWD}
TESTS_OUT_EXPECTED=${TESTS_FOLDER}/expected
TESTS_OUTPUT=${TESTS_FOLDER}/test-outputs
################################################
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
################################################

# Definir flags de execução
RUN_JAVA=false
RUN_PYTHON=false

# Verificar argumentos
for arg in "$@"; do
    case $arg in
        -j) RUN_JAVA=true ;;
        -p) RUN_PYTHON=true ;;
        *) echo "Uso: $0 [-j] [-p]"; exit 1 ;;
    esac
done

# Se nenhum argumento for passado, executa ambos
if [ "$RUN_JAVA" = false ] && [ "$RUN_PYTHON" = false ]; then
    RUN_JAVA=true
    RUN_PYTHON=true
fi

rm -rf $TESTS_OUTPUT
mkdir -p $TESTS_OUTPUT

### Run tests for Java client ###
if [ "$RUN_JAVA" = true ]; then
    cd $CLI_JAVA_PATH
    echo "Running tests for Java client..."
    i=1
    while :
    do
        TEST=$(printf "%02d" $i); 
        if [ -e ${TESTS_FOLDER}/input$TEST.txt ]
        then 
            mvn --quiet exec:java < ${TESTS_FOLDER}/input$TEST.txt > ${TESTS_OUTPUT}/java_out$TEST.txt
            DIFF=$(diff ${TESTS_OUTPUT}/java_out$TEST.txt ${TESTS_OUT_EXPECTED}/out$TEST.txt) 
            if [ "$DIFF" != "" ] 
            then
                echo "${RED}[Java][$TEST] TEST FAILED${NC}"
            else
                echo "${GREEN}[Java][$TEST] TEST PASSED${NC}"
            fi
            i=$((i+1))
        else
            break
        fi
    done
fi

### Run tests for Python client ###
if [ "$RUN_PYTHON" = true ]; then
    cd $CLI_PYTHON_PATH
    echo "Running tests for Python client..."
    i=1
    while :
    do
        TEST=$(printf "%02d" $i); 
        if [ -e ${TESTS_FOLDER}/input$TEST.txt ]
        then 
            python3 client_main.py localhost:2001 1 < ${TESTS_FOLDER}/input$TEST.txt > ${TESTS_OUTPUT}/python_out$TEST.txt
            DIFF=$(diff ${TESTS_OUTPUT}/python_out$TEST.txt ${TESTS_OUT_EXPECTED}/out$TEST.txt) 
            if [ "$DIFF" != "" ] 
            then
                echo "${RED}[Python][$TEST] TEST FAILED${NC}"
            else
                echo "${GREEN}[Python][$TEST] TEST PASSED${NC}"
            fi
            i=$((i+1))
        else
            break
        fi
    done
fi

echo "Check the outputs of each test in ${TESTS_OUTPUT}."
