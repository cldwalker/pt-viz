(ns pt-viz.core
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [cemerick.url :refer [url url-encode]]
            [clojure.pprint :refer [pprint]]))

(def api-token
  "My pivotal tracker api token. TODO: Move to env var"
  "X")

(def project-id
  "X")

(def epic-id
  "The epic I'm testing with"
  "3460799")

(def pt-url
  "Base url for pivotal tracker"
  "https://www.pivotaltracker.com/services/v5")

(defn api-get
  [url]
  (-> (client/get url
                  {:headers {"X-TrackerToken" api-token}})
      :body
      (parse-string true)))

(def preceded-by-syntax-regex
  "The regex used to convey which stories precede a given story"
  #"##[ ]*Preceded[ ]*By[\s]*\[[#0-9 ,]+\]")

(def preceded-by-regex
  #"\[[#0-9 ,]+\]")

(defn get-epic-stories
  "Get the necessary information for each story in an epic needed to construct the story graph"
  [epic-id]
  (let [epic-url (str pt-url "/projects/" project-id "/epics/" epic-id)
        epic-label-name (-> (api-get epic-url)
                            :label
                            :name)
        stories-url (str pt-url "/projects/" project-id "/stories?with_label=" (url-encode epic-label-name))
        stories (api-get stories-url)
        parse-preceded-by (fn [description]
                            (if-let [statement (re-find preceded-by-syntax-regex description)]
                              (if-let [raw (re-find preceded-by-regex statement)]
                                (-> (clojure.string/replace raw #"#" "")
                                    read-string)
                                [])
                              []))
        process-story (fn [{:keys [description] :as story}]
                        (let [preceded-by (parse-preceded-by description)]
                          (-> story
                              (select-keys [:id :url :name])
                              (assoc :preceded-by preceded-by))))]
    (mapv #(process-story %) stories)))

(defn stories->graphviz-data
  "Create a data representation of the node needed to make a graphviz dot"
  [stories]
  (reduce (fn [acc
               {:keys [id url name preceded-by] :as story}]
            (let [create-edge (fn [from-id]
                                {:from from-id
                                 :to id})
                  new-edges (mapv create-edge preceded-by)]
              (-> acc
                  (update :nodes conj story)
                  (update :edges concat new-edges)
                  )))
          {:nodes []
           :edges []}
          stories))

(defn graphviz-data->graphviz
  "Turn the data representation of the graph into the text format that the `dot` command line tool understands"
  [{:keys [nodes edges]}]
  (let [create-node (fn [{:keys [id url name]}]
                           (format "\t%d [label=\"%s\",shape=box,fontcolor=blue,URL=\"%s\"];" id name url))
        create-edge (fn [{:keys [from to]}]
                      (format "\t%d -> %d;" from to))
        node-lines (mapv create-node nodes)
        edge-lines (mapv create-edge edges)
        lines (concat node-lines edge-lines)
        body (clojure.string/join "\n" lines)]
    (format "digraph G {\n%s\n}" body)))

(pprint (-> (get-epic-stories epic-id)
            stories->graphviz-data))

(spit "graph2.gv"
      (-> (get-epic-stories epic-id)
          stories->graphviz-data
          graphviz-data->graphviz))
