(ns pt-viz.core
  (:require [clj-http.client :as client]
            [cheshire.core :as cheshire]
            [cemerick.url :refer [url url-encode]]
            [clojure.string :as string]
            [clojure.java.shell :as sh]
            [clojure.pprint :refer [pprint]]))

(def api-token
  "My pivotal tracker api token. TODO: Move to env var"
  "X")

(def project-id
  "X")

(def pt-url
  "Base url for pivotal tracker"
  "https://www.pivotaltracker.com/services/v5")

(defn api-get
  [url]
  (-> (client/get url
                  {:headers {"X-TrackerToken" api-token}})
      :body
      (cheshire/parse-string true)))

(defn fetch-blockers [story-id]
  (->> (str pt-url "/projects/" project-id "/stories/" story-id "/blockers")
      api-get
      (keep #(if-let [story-id (->> % :description (re-matches #"(?:#|https://www.pivotaltracker.com/story/show/)(\d+)") second)]
               (Integer/parseInt story-id)
               ;; Support displaying raw-text blockers
               (:description %)))
       vec))

(defn get-epic-stories
  "Get the necessary information for each story in an epic needed to construct the story graph"
  [epic-id]
  (let [epic-url (str pt-url "/projects/" project-id "/epics/" epic-id)
        epic-label-name (-> (api-get epic-url)
                            :label
                            :name)
        stories-url (str pt-url "/projects/" project-id "/stories?with_label=" (url-encode epic-label-name))
        stories (api-get stories-url)
        _ (println (format "Processing %s stories for epic '%s'..." (count stories) epic-label-name))
        stories_ (mapv (fn [{:keys [id] :as story}]
                                      (let [blockers (fetch-blockers id)]
                                        (-> story
                                            (select-keys [:id :url :name])
                                            (assoc :blockers blockers))))
                                    stories)]
    (println "Found"
             (count (mapcat :blockers stories_))
             "dependencies among epic stories")
    stories_))

(defn stories->graphviz-data
  "Create a data representation of the node needed to make a graphviz dot"
  [stories]
  (reduce (fn [acc
               {:keys [id url name blockers] :as story}]
            (let [create-edge (fn [from-id]
                                {:from from-id
                                 :to id})
                  new-edges (mapv create-edge blockers)]
              (-> acc
                  (update :nodes conj story)
                  (update :edges concat new-edges))))
          {:nodes []
           :edges []}
          stories))

(let [counter (atom 0)]
  (defn unique-id "Unique id for anonymous text nodes. Ok to start with 1 as PT stories are never this low" []
    (swap! counter inc)))

(defn graphviz-data->graphviz
  "Turn the data representation of the graph into the text format that the `dot` command line tool understands"
  [{:keys [nodes edges]}]
  (let [create-node (fn [{:keys [id url name]}]
                           (if url
                             (format "\t%d [label=\"%s\",shape=box,fontcolor=blue,URL=\"%s\"];" id name url)
                             (format "\t%d [label=\"%s\",shape=box];" id name)))
        create-edge (fn [{:keys [from to]}]
                      (format "\t%d -> %d;" from to))
        anon-nodes (atom [])
        edges_ (mapv (fn [{:keys [from] :as edge}]
                       (if (string? from)
                         ;; Support text edges
                         (let [id (unique-id)]
                           (swap! anon-nodes conj {:id id :name from})
                           (assoc edge :from id))
                         edge))
                     edges)
        node-lines (mapv create-node (into nodes @anon-nodes))
        edge-lines (mapv create-edge edges_)
        lines (concat node-lines edge-lines)
        body (string/join "\n" lines)]
    (format "digraph G {\n%s\n}" body)))

(defn -main
  "Given a PT epic id, generate a story dependency graph to a file. Default file is graph.svg."
  ([epic-id] (-main epic-id "graph.svg"))
  ([epic-id file]
   (let [epic-id_ (string/replace epic-id  #"^#*" "") ;; Allow click id and paste into terminal
         graphviz-text (-> (get-epic-stories epic-id_)
                           stories->graphviz-data
                           graphviz-data->graphviz)]
     (sh/sh "dot" "-Tsvg" "-o" file :in graphviz-text)
     ;; Needed to exit due to sh/sh
     (shutdown-agents))))
