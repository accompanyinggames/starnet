FROM ubuntu:18.04

## core
RUN apt-get update && \
    apt-get install -y \
            sudo  \
            git-core  \
            rlwrap  \
            software-properties-common  \
            unzip wget curl net-tools lsof

WORKDIR /tmp

## openjdk
# RUN apt-get update && \
#     apt-get install -y openjdk-8-jdk

## graalvm
# https://github.com/arjones/docker-graalvm/blob/master/Dockerfile
# https://github.com/OlegIlyenko/graalvm-native-image/blob/master/Dockerfile
ENV GRAALVM_VERSION=19.3.0
ENV SUFFIX_URL=java8-linux-amd64-${GRAALVM_VERSION}
ENV SUFFIX_DIR=java8-${GRAALVM_VERSION}
ENV PATH $PATH:/usr/local/graalvm/bin
#  dir will be graalvm-ce-java8-19.3.0
RUN curl -Ls "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-19.3.0/graalvm-ce-${SUFFIX_URL}.tar.gz" | \
    tar zx -C /usr/local/ && \
    ls -l /usr/local/ && \
    rm -f /usr/local/graalvm-ce-${SUFFIX_DIR}/src.zip && \
    ln -s /usr/local/graalvm-ce-${SUFFIX_DIR} /usr/local/graalvm && \
    rm -fr /var/lib/apt
RUN gu install native-image 

## clojure
ENV CLOJURE_TOOLS=linux-install-1.10.1.466.sh
RUN curl -O https://download.clojure.org/install/$CLOJURE_TOOLS && \
    chmod +x $CLOJURE_TOOLS && \
    sudo ./$CLOJURE_TOOLS && \
    clojure -Stree

## leiningen
ENV LEIN_VERSION=2.9.1
ENV LEIN_DIR=/usr/local/bin/
RUN curl -O https://raw.githubusercontent.com/technomancy/leiningen/${LEIN_VERSION}/bin/lein && \
    mv lein ${LEIN_DIR} && \
    chmod a+x ${LEIN_DIR}/lein && \
    lein version

## zookeeper
# WORKDIR /ctx/
# RUN wget https://github.com/apache/zookeeper/archive/release-3.5.6.tar.gz && \
#     tar -xzf release-3.5.6.tar.gz
# WORKDIR /ctx/zookeeper-release-3.5.6/

ENV ZOOKEEPER_VERSION=3.5.7
ENV APACHE_MIRROR1=http://mirror.linux-ia64.org/apache/
ENV APACHE_MIRROR2=http://apache-mirror.rbc.ru/pub/apache
ENV APACHE_MIRROR3=https://downloads.apache.org/

WORKDIR /ctx/
RUN wget $APACHE_MIRROR1/zookeeper/zookeeper-$ZOOKEEPER_VERSION/apache-zookeeper-$ZOOKEEPER_VERSION-bin.tar.gz && \
    tar -xzf apache-zookeeper-$ZOOKEEPER_VERSION-bin.tar.gz && \
    mv apache-zookeeper-$ZOOKEEPER_VERSION-bin zookeeper
WORKDIR /ctx/zookeeper/
COPY f f

# RUN useradd -u 1000 user
# RUN chown -R user /ctx
# USER user

EXPOSE 2181