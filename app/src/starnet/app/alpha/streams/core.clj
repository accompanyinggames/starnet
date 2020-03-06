(ns starnet.app.alpha.streams.core
  (:require
   [clojure.pprint :as pp]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.spec.gen.alpha :as gen])
  (:import
   starnet.app.aux.serdesTransitJsonSerializer
   starnet.app.aux.serdesTransitJsonDeserializer
   starnet.app.aux.serdesTransitJsonSerde

   org.apache.kafka.common.serialization.Serdes
   org.apache.kafka.streams.KafkaStreams
   org.apache.kafka.streams.StreamsBuilder
   org.apache.kafka.streams.StreamsConfig
   org.apache.kafka.streams.Topology
   org.apache.kafka.streams.kstream.KStream
   org.apache.kafka.streams.kstream.KTable
   java.util.Properties
   java.util.concurrent.CountDownLatch
   org.apache.kafka.clients.admin.KafkaAdminClient
   org.apache.kafka.clients.admin.NewTopic
   org.apache.kafka.clients.consumer.KafkaConsumer
   org.apache.kafka.clients.producer.KafkaProducer
   org.apache.kafka.clients.producer.ProducerRecord
   org.apache.kafka.streams.kstream.ValueMapper
   org.apache.kafka.streams.kstream.KeyValueMapper
   org.apache.kafka.streams.KeyValue

   org.apache.kafka.streams.kstream.Materialized
   org.apache.kafka.streams.kstream.Produced
   org.apache.kafka.streams.kstream.Reducer
   org.apache.kafka.streams.kstream.Grouped
   org.apache.kafka.streams.state.QueryableStoreTypes

   org.apache.kafka.streams.kstream.Initializer
   org.apache.kafka.streams.kstream.Aggregator

   java.util.ArrayList
   java.util.Locale
   java.util.Arrays))




(defn create-topics
  [{:keys [props
           names
           num-partitions
           replication-factor] :as opts}]
  (let [client (KafkaAdminClient/create props)
        topics (java.util.ArrayList.
                (mapv (fn [name]
                        (NewTopic. name num-partitions (short replication-factor))) names))]
    (.createTopics client topics)))

(defn delete-topics
  [{:keys [props
           names] :as opts}]
  (let [client  (KafkaAdminClient/create props)]
    (.deleteTopics client (java.util.ArrayList. names))))

(defn list-topics
  [{:keys [props] :as opts}]
  (let [client (KafkaAdminClient/create props)
        kfu (.listTopics client)]
    (.. kfu (names) (get))))

(defn add-shutdown-hook
  [props streams latch]
  (-> (Runtime/getRuntime)
      (.addShutdownHook (proxy
                         [Thread]
                         ["streams-shutdown-hook"]
                          (run []
                            (when (.isRunning (.state streams))
                              (.println (System/out))
                              (println "; closing" (.get props "application.id"))
                              (.close streams))
                            (.countDown latch))))))

(defn produce-event
  [producer topic k event]
  (.send producer (ProducerRecord.
                   topic
                   k
                   event)))
(s/fdef produce-event
  :args (s/cat :producer :instance/kproducer
               :topic string?
               :k (s/alt :uuid uuid? :string string?)
               :event :ev/event))

(defn delete-record
  [producer topic k]
  (produce-event
   producer
   topic
   k
   {:ev/type :ev.c/delete-record}))

(defn future-call-consumer
  [{:keys [topic
           key-des
           value-des
           recordf]
    :or {key-des "starnet.app.aux.serdesTransitJsonDeserializer"
         value-des "starnet.app.aux.serdesTransitJsonDeserializer"}
    :as opts}]
  (future-call
   (fn []
     (let [consumer (KafkaConsumer.
                     {"bootstrap.servers" "broker1:9092"
                      "auto.offset.reset" "earliest"
                      "auto.commit.enable" "false"
                      "group.id" (.toString (java.util.UUID/randomUUID))
                      "consumer.timeout.ms" "5000"
                      "key.deserializer" key-des
                      "value.deserializer" value-des})]
       (.subscribe consumer (Arrays/asList (object-array [topic])))
       (while true
         (let [records (.poll consumer 1000)]
           (.println System/out (str "; polling " topic (java.time.LocalTime/now)))
           (doseq [rec records]
             (recordf rec)
            )))))))

(defn create-user
  [producer event]
  (produce-event producer
                 "alpha.user.data"
                 (:u/uuid event)
                 event))

(s/fdef create-user
  :args (s/cat :producer some? :event :ev.u/create))


(def topic-evtype-map
  {"alpha.user" #{:ev.u/create :ev.u/update :ev.u/delete}
   "alpha.game" #{:ev.g.u/create :ev.g.u/delete
                  :ev.g.u/join :ev.g.u/leave
                  :ev.g.u/configure :ev.g.u/start
                  :ev.g.p/move-cape :ev.g.p/collect-tile-value
                  :ev.g.a/finish-game}})

(def evtype-topic-map
  (->> topic-evtype-map
       (map (fn [[topic kset]]
              (map #(vector % topic) kset)))
       (mapcat identity)
       (into {})))

(def evtype-recordkey-map
  {:ev.u/create :u/uuid
   :ev.u/update :u/uuid
   :ev.u/delete :u/uuid
   :ev.g.u/configure :g/uuid
   })

(defn event-to-recordkey
  [ev]
  (or
   (-> ev :ev/type evtype-recordkey-map ev)
   (java.util.UUID/randomUUID)))

(defn event-to-topic
  [ev]
  (-> ev :ev/type evtype-topic-map))

(defmulti send-event
  "Send kafka event. Topic is mapped by ev/type."
  {:arglists '([ev kproducer]
               [ev topic kproducer]
               [ev uuidkey kproducer]
               [ev topic uuidkey kproducer])}
  (fn [ev & args]
    (mapv type (into [ev] args))))

(defmethod send-event [Object :isa/kproducer]
  [ev kproducer]
  (.send kproducer
         (ProducerRecord.
          (event-to-topic ev)
          (event-to-recordkey ev)
          ev)))
(defmethod send-event [Object String :isa/kproducer]
  [ev topic kproducer]
  (.send kproducer
         (ProducerRecord.
          topic-evtype-map
          (event-to-recordkey ev)
          ev)))
(defmethod send-event [Object :isa/uuid :isa/kproducer]
  [ev uuidkey kproducer]
  (.send kproducer
         (ProducerRecord.
          (event-to-topic ev)
          uuidkey
          ev)))
(defmethod send-event [Object String :isa/uuid :isa/kproducer]
  [ev topic uuidkey kproducer]
  (.send kproducer
         (ProducerRecord.
          topic
          uuidkey
          ev)))





